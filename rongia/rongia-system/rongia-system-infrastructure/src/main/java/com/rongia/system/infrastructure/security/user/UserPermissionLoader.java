package com.rongia.system.infrastructure.security.user;

import com.rongia.system.domain.menu.Menu;
import com.rongia.system.domain.menu.MenuTypeEnum;
import com.rongia.system.domain.department.Department;
import com.rongia.system.domain.employee.Employee;
import com.rongia.system.domain.position.Position;
import com.rongia.system.domain.privilege.Privilege;
import com.rongia.system.domain.user.User;
import com.rongia.system.domain.user.UserRole;
import com.rongia.system.infrastructure.menu.MenuJpa;
import com.rongia.system.infrastructure.department.DepartmentJpa;
import com.rongia.system.infrastructure.employee.EmployeeJpa;
import com.rongia.system.infrastructure.position.PositionJpa;
import com.rongia.system.infrastructure.privilege.PrivilegeJpa;
import com.rongia.system.infrastructure.user.UserJpa;
import com.rongia.shared.infrastructure.security.user.LoginUser;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户权限加载器：登录时一次性算全「数据范围 + 应用许可」，避免每个请求都递归查组织树
 *
 * <p>数据范围口径：用户 → 员工 → 岗位 → 部门，再沿部门树**向下展开到子孙**，
 * 所以上级能看到自己及所有下级部门的岗位。用户本身不挂岗位（岗位是员工上的一列，单值）。
 */
@Service
public class UserPermissionLoader {

  /** 超级管理员角色Id */
  private static final Long ADMIN_ROLE_ID = 1L;

  /** 权限主体：用户 */
  private static final String MASTER_USER = "User";

  /** 权限主体：角色 */
  private static final String MASTER_ROLE = "Role";

  /** 权限主体：岗位 */
  private static final String MASTER_POSITION = "Position";

  /** 岗位 JPA */
  private final PositionJpa positionJpa;

  /** 部门 JPA */
  private final DepartmentJpa departmentJpa;

  /** 员工 JPA */
  private final EmployeeJpa employeeJpa;

  /** 权限 JPA */
  private final PrivilegeJpa privilegeJpa;

  /** 应用 JPA */
  private final MenuJpa menuJpa;

  /**
   * 构造器注入
   *
   * @param positionJpa 岗位 JPA
   * @param departmentJpa 部门 JPA
   * @param employeeJpa 员工 JPA
   * @param privilegeJpa 权限 JPA
   * @param menuJpa 应用 JPA
   */
  public UserPermissionLoader(
      PositionJpa positionJpa,
      DepartmentJpa departmentJpa,
      EmployeeJpa employeeJpa,
      PrivilegeJpa privilegeJpa,
      MenuJpa menuJpa) {
    this.positionJpa = positionJpa;
    this.departmentJpa = departmentJpa;
    this.employeeJpa = employeeJpa;
    this.privilegeJpa = privilegeJpa;
    this.menuJpa = menuJpa;
  }

  /**
   * 加载用户权限数据范围
   *
   * @param model 用户聚合
   * @return 权限数据
   */
  @Transactional(readOnly = true)
  public UserPermissionData load(User model) {
    UserPermissionData data = new UserPermissionData();

    // 1. 用户直接角色（聚合内，无需查库）
    Set<Long> userRoleIds =
        model.getUserRoles().stream().map(UserRole::getRoleId).collect(Collectors.toSet());

    // 2. 用户 → 员工 → 岗位（岗位是员工上的一列，可为空）
    List<Employee> employees = employeeJpa.findByUserId(model.getId());
    Set<Long> userPositionIds =
        employees.stream()
            .map(Employee::getPositionId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    List<Position> userPositions = userPositionIds.isEmpty() ? List.of() : positionJpa.findAllById(userPositionIds);

    // 3. 岗位 → 部门（聚合内是 Id，需查实体）
    Set<Long> userDepartmentIds = collectIds(userPositions, Position::getDepartmentId);

    // 4. 部门 → 公司（组织的顶层链路终点）
    List<Department> userDepartments =
        userDepartmentIds.isEmpty() ? List.of() : departmentJpa.findAllById(userDepartmentIds);
    data.companyIds = collectIds(userDepartments, Department::getCompanyId);

    // 5. 沿部门树向下展开到子孙，这是「数据范围」的真正口径
    Set<Long> allDepartmentIds =
        userDepartmentIds.isEmpty()
            ? Set.of()
            : collectIds(
                departmentJpa.findByIdsOrDescendants(userDepartmentIds), Department::getId);

    // 6. 部门子树内的岗位，才是用户实际可见的岗位
    Set<Long> allPositionIds =
        allDepartmentIds.isEmpty()
            ? Set.of()
            : collectIds(positionJpa.findByDepartmentIdIn(allDepartmentIds), Position::getId);
    data.departmentIds = allDepartmentIds;
    data.positionIds = allPositionIds;

    // 7. 超级管理员走独立分支：直接放开全部应用，不再查权限表
    if (userRoleIds.contains(ADMIN_ROLE_ID)) {
      data.superAdmin = true;
      Set<Menu> menus = new HashSet<>(menuJpa.findAll());
      fillMenus(data, menus);
      return data;
    }

    data.superAdmin = false;

    // 8. 权限来源三条：用户直接、用户角色、本人岗位（岗位授权走 Privilege 的 Position 主体）
    Set<Long> menuIds = new HashSet<>();
    addAccessValues(
        menuIds, privilegeJpa.findByMasterAndMasterValueIn(MASTER_USER, Set.of(model.getId())));
    if (!userRoleIds.isEmpty()) {
      addAccessValues(
          menuIds, privilegeJpa.findByMasterAndMasterValueIn(MASTER_ROLE, userRoleIds));
    }
    if (!userPositionIds.isEmpty()) {
      addAccessValues(
          menuIds, privilegeJpa.findByMasterAndMasterValueIn(MASTER_POSITION, userPositionIds));
    }

    if (menuIds.isEmpty()) {
      return data;
    }

    // 9. 按权限指向的应用反查许可
    fillMenus(data, new HashSet<>(menuJpa.findByIdIn(menuIds)));
    return data;
  }

  /**
   * 提取实体集合上的某个 Id 字段
   *
   * @param list 实体列表
   * @param mapper 取值函数
   * @param <T> 实体类型
   * @return Id 集合
   */
  private <T> Set<Long> collectIds(Collection<T> list, Function<T, Long> mapper) {
    if (list.isEmpty()) {
      return Set.of();
    }
    return list.stream().map(mapper).collect(Collectors.toSet());
  }

  /**
   * 累计权限记录指向的应用Id
   *
   * @param menuIds 累计目标集合
   * @param privileges 权限记录集合
   */
  private void addAccessValues(Set<Long> menuIds, Set<Privilege> privileges) {
    for (Privilege item : privileges) {
      menuIds.add(item.getAccessValue());
    }
  }

  /**
   * 填充应用Id与许可证（过滤掉 RESOURCE 类型的资源节点，它不参与许可）
   *
   * @param data 权限数据
   * @param menus 应用集合
   */
  private void fillMenus(UserPermissionData data, Set<Menu> menus) {
    data.menuIds =
        menus.stream()
            .filter(x -> !MenuTypeEnum.RESOURCE.getCode().equals(x.getType()))
            .map(Menu::getId)
            .collect(Collectors.toSet());
    data.licenses =
        menus.stream()
            .map(Menu::getLicense)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());
  }

  /** 用户权限数据 */
  public static class UserPermissionData {
    /** 是否超级管理员 */
    public boolean superAdmin;

    /** 公司Id集合 */
    public Set<Long> companyIds = Set.of();

    /** 部门Id集合（含子孙） */
    public Set<Long> departmentIds = Set.of();

    /** 岗位Id集合 */
    public Set<Long> positionIds = Set.of();

    /** 应用Id集合 */
    public Set<Long> menuIds = Set.of();

    /** 许可证集合 */
    public Set<String> licenses = Set.of();
  }
}

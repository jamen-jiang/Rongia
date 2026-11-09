package com.rongia.production.domain.jobcard;

import com.rongia.kernel.domain.entity.BaseDeleteEntity;
import com.rongia.kernel.domain.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 工卡工时日志
 *
 * <p>一笔一行：这一枪报了良品多少、报废多少、花了多少分钟。掐表时记起止，一枪报完时只有时长。
 * 照 ERPNext 的 Job Card Time Log——数量也挂在工时行上，所以它才是「一次报工」的凭证。
 *
 * <p>它没有作废状态：卡还在草稿/加工中时可以直接删行，提交之后整张卡锁死，要撤只能取消整张卡。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "JobCardTimeLog",
    indexes = {@Index(name = "IX_JobCardTimeLog_JobCardId", columnList = "JobCardId")})
public class JobCardTimeLog extends BaseDeleteEntity {

  /** 工卡Id */
  @Column(name = "JobCardId")
  private Long jobCardId;

  /** 开始时间（可空；与结束时间要么都填、要么都不填） */
  @Column(name = "FromTime")
  @Nullable
  private LocalDateTime fromTime;

  /** 结束时间（可空；有起止时由起止算工时） */
  @Column(name = "ToTime")
  @Nullable
  private LocalDateTime toTime;

  /** 本次工时（分钟） */
  @Column(name = "TimeInMinutes", nullable = false, precision = 18, scale = 2)
  private BigDecimal timeInMinutes;

  /** 本次良品数量 */
  @Column(name = "CompletedQty", nullable = false, precision = 18, scale = 2)
  private BigDecimal completedQty;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /**
   * 构造器
   *
   * @param fromTime 开始时间（可空）
   * @param toTime 结束时间（可空）
   * @param timeInMinutes 本次工时（分钟）
   * @param completedQty 本次良品数量
   * @param remark 备注（可空）
   * @throws DomainException 工时或数量为负、数量与工时同时为 0、或起止只填了一个时抛出
   */
  JobCardTimeLog(
      @Nullable LocalDateTime fromTime,
      @Nullable LocalDateTime toTime,
      BigDecimal timeInMinutes,
      BigDecimal completedQty,
      @Nullable String remark) {
    if (timeInMinutes.signum() < 0) {
      throw new DomainException("工时不能为负数");
    }
    if (completedQty.signum() < 0) {
      throw new DomainException("报工数量不能为负数");
    }
    if (completedQty.signum() == 0 && timeInMinutes.signum() == 0) {
      throw new DomainException("数量与工时不能同时为0");
    }
    if ((fromTime == null) != (toTime == null)) {
      throw new DomainException("开始时间与结束时间要么都填、要么都不填");
    }
    if (fromTime != null) {
      assert toTime != null;
      if (toTime.isBefore(fromTime)) {
        throw new DomainException("结束时间早于开始时间");
      }
      // 有起止就以起止为准，传入的分钟数只在没有起止（一枪报完）时才有意义
      this.timeInMinutes =
          BigDecimal.valueOf(Duration.between(fromTime, toTime).toSeconds())
              .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    } else {
      this.timeInMinutes = timeInMinutes;
    }
    this.fromTime = fromTime;
    this.toTime = toTime;
    this.completedQty = completedQty;
    this.remark = remark;
  }
}

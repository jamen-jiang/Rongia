package com.rongia.kernel.application.port.ftp;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** FTP 文件（只表示真文件：目录、软链接、解析失败的条目在取列表时已被剔除） */
@NullMarked
@Getter
@Setter
public class FtpFile {

  /** 文件名 */
  private String name = "";

  /** 完整路径（父目录 + 文件名，可直接喂给 get / remove） */
  private String path = "";

  /** 文件大小（字节，可空：服务器未返回大小时为 null） */
  @Nullable
  private Long size;

  /** 修改时间（可空：服务器未返回时间时为 null；部分服务器精度只到分钟） */
  @Nullable
  private LocalDateTime modifiedTime;
}

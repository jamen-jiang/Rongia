package com.rongia.shared.application.port.ftp;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;

/** FTP 文件 */
@NullMarked
@Getter
@Setter
public class FtpFile {

  /** 文件名 */
  private String name = "";

  /** 文件大小 */
  private Long size;
}

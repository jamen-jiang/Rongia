package com.rongia.shared.infrastructure.core;

import com.rongia.shared.application.exception.ApiException;
import com.rongia.shared.application.port.ftp.FtpClient;
import com.rongia.shared.application.port.ftp.FtpFile;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** 基于 Apache Commons Net 的 FTP 客户端实现 */
@NullMarked
@Service
public class ApacheFtpClient implements FtpClient {

  /** FTP主机 */
  private final String host;

  /** FTP端口 */
  private final int port;

  /** FTP用户名 */
  private final String username;

  /** FTP密码 */
  private final String password;

  /**
   * 构造器注入
   *
   * @param host FTP主机
   * @param port FTP端口
   * @param username FTP用户名
   * @param password FTP密码
   */
  public ApacheFtpClient(
      @Value("${ftp.host}") String host,
      @Value("${ftp.port}") int port,
      @Value("${ftp.username}") String username,
      @Value("${ftp.password}") String password) {
    this.host = host;
    this.port = port;
    this.username = username;
    this.password = password;
  }

  /**
   * 获取 FTP 文件内容
   *
   * @param path FTP 文件路径
   * @return 文件内容（可空），文件不存在返回 null
   */
  @Override
  public byte @Nullable [] get(String path) {
    FTPClient client = connect();
    try (InputStream stream = client.retrieveFileStream(path)) {
      if (stream == null) {
        return null;
      }
      byte[] bytes = stream.readAllBytes();
      client.completePendingCommand();
      return bytes;
    } catch (IOException e) {
      throw new ApiException("FTP 文件下载异常");
    } finally {
      disconnect(client);
    }
  }

  /**
   * 将流上传到 FTP 指定路径
   *
   * @param stream 要上传的流
   * @param remotePath FTP 目标路径
   * @return 上传是否成功
   */
  @Override
  public boolean upload(InputStream stream, String remotePath) {
    FTPClient client = connect();
    try {
      ensureDirectory(client, remotePath);
      return client.storeFile(remotePath, stream);
    } catch (IOException e) {
      throw new ApiException("FTP 文件上传异常");
    } finally {
      disconnect(client);
    }
  }

  /**
   * 将字节数组上传到 FTP 指定路径
   *
   * @param buffer 要上传的字节数组
   * @param remotePath FTP 目标路径
   * @return 上传是否成功
   */
  @Override
  public boolean upload(byte[] buffer, String remotePath) {
    try (InputStream stream = new ByteArrayInputStream(buffer)) {
      return upload(stream, remotePath);
    } catch (IOException e) {
      throw new ApiException("FTP 文件上传异常");
    }
  }

  /**
   * 将本地文件上传到 FTP 指定路径
   *
   * @param localPath 本地文件路径
   * @param remotePath FTP 目标路径
   * @return 上传是否成功
   */
  @Override
  public boolean upload(String localPath, String remotePath) {
    try (InputStream stream = new FileInputStream(localPath)) {
      return upload(stream, remotePath);
    } catch (IOException e) {
      throw new ApiException("FTP 文件上传异常");
    }
  }

  /**
   * 删除 FTP 文件
   *
   * @param path FTP 文件路径
   */
  @Override
  public void remove(String path) {
    FTPClient client = connect();
    try {
      client.deleteFile(path);
    } catch (IOException e) {
      throw new ApiException("FTP 文件删除异常");
    } finally {
      disconnect(client);
    }
  }

  /**
   * 创建 FTP 目录
   *
   * @param path FTP 目录路径
   */
  @Override
  public void createDirectory(String path) {
    FTPClient client = connect();
    try {
      client.makeDirectory(path);
    } catch (IOException e) {
      throw new ApiException("FTP 目录创建异常");
    } finally {
      disconnect(client);
    }
  }

  /**
   * 判断 FTP 文件是否存在
   *
   * @param path FTP 文件路径
   * @return 文件是否存在
   */
  @Override
  public boolean fileExists(String path) {
    FTPClient client = connect();
    try {
      FTPFile[] files = client.listFiles(path);
      return files.length > 0;
    } catch (IOException e) {
      throw new ApiException("FTP 文件查询异常");
    } finally {
      disconnect(client);
    }
  }

  /**
   * 判断 FTP 目录是否存在
   *
   * @param path FTP 目录路径
   * @return 目录是否存在
   */
  @Override
  public boolean directoryExists(String path) {
    FTPClient client = connect();
    try {
      return client.changeWorkingDirectory(path);
    } catch (IOException e) {
      throw new ApiException("FTP 目录查询异常");
    } finally {
      disconnect(client);
    }
  }

  /**
   * 获取 FTP 目录下的所有文件列表
   *
   * @param path FTP 目录路径
   * @return 文件列表
   */
  @Override
  public List<FtpFile> getFtpFiles(String path) {
    FTPClient client = connect();
    try {
      FTPFile[] files = client.listFiles(path);
      List<FtpFile> result = new ArrayList<>();
      for (FTPFile file : files) {
        FtpFile item = new FtpFile();
        item.setName(file.getName());
        item.setSize(file.getSize());
        result.add(item);
      }
      return result;
    } catch (IOException e) {
      throw new ApiException("FTP 目录查询异常");
    } finally {
      disconnect(client);
    }
  }

  /**
   * 建立连接并登录
   *
   * @return FTP客户端
   */
  private FTPClient connect() {
    FTPClient client = new FTPClient();
    try {
      client.setControlEncoding("UTF-8");
      client.connect(host, port);
      client.login(username, password);
      client.setFileType(FTP.BINARY_FILE_TYPE);
      client.enterLocalPassiveMode();
      if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
        client.disconnect();
        throw new ApiException("FTP 连接失败");
      }
      return client;
    } catch (IOException e) {
      throw new ApiException("FTP 连接异常");
    }
  }

  /**
   * 断开连接
   *
   * @param client FTP客户端
   */
  private void disconnect(@Nullable FTPClient client) {
    if (client == null || !client.isConnected()) {
      return;
    }
    try {
      client.logout();
      client.disconnect();
    } catch (IOException ignored) {
      // 忽略登出异常
    }
  }

  /**
   * 确保目标路径的目录存在
   *
   * @param client FTP客户端
   * @param remotePath FTP 目标路径
   * @throws IOException IO异常
   */
  private void ensureDirectory(FTPClient client, String remotePath) throws IOException {
    int lastSlash = remotePath.lastIndexOf('/');
    if (lastSlash <= 0) {
      return;
    }
    String dir = remotePath.substring(0, lastSlash);
    String[] parts = dir.split("/");
    String current = "";
    for (String part : parts) {
      if (part.isEmpty()) {
        continue;
      }
      current += "/" + part;
      client.makeDirectory(current);
    }
  }
}

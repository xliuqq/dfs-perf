package pasalab.dfs.perf.fs;

import alluxio.AlluxioURI;
import alluxio.client.file.FileSystem;
import alluxio.client.file.URIStatus;
import alluxio.conf.AlluxioConfiguration;
import alluxio.exception.AlluxioException;
import alluxio.exception.FileAlreadyExistsException;
import alluxio.exception.FileDoesNotExistException;
import alluxio.exception.InvalidPathException;
import alluxio.grpc.*;
import pasalab.dfs.perf.basic.TaskConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

;

public class PerfFileSystemAlluxioFS extends PerfFileSystem {

  public static PerfFileSystem getClient(String path, TaskConfiguration taskConf) {
    return new PerfFileSystemAlluxioFS(path, taskConf);
  }

  private final OpenFilePOptions mReadOptions;
  private final CreateFilePOptions mWriteOptions;
  private FileSystem mAlluxioFs;

  private PerfFileSystemAlluxioFS(String path, TaskConfiguration taskConf) {
    int blockSizeByte;
    ReadPType readType;
    WritePType writeType;

    if (taskConf == null) {
      blockSizeByte = 1024 * 1024 * 1024;
      readType = ReadPType.CACHE_PROMOTE;
      writeType = WritePType.ASYNC_THROUGH;
    } else {
      blockSizeByte =
          taskConf.hasProperty("block.size.bytes") ? taskConf.getIntProperty("block.size.bytes")
              : 1024 * 1024 * 1024;
      readType =
          taskConf.hasProperty("read.type") ? ReadPType.valueOf(taskConf.getProperty("read.type"))
              : ReadPType.CACHE_PROMOTE;
      writeType =
          taskConf.hasProperty("write.type") ? WritePType.valueOf(taskConf.getProperty("write.type"))
              : WritePType.ASYNC_THROUGH;
    }

    mReadOptions = OpenFilePOptions.newBuilder().setReadType(readType).build();
    mWriteOptions = CreateFilePOptions.newBuilder().setWriteType(writeType).setBlockSizeBytes(blockSizeByte).build();
  }

  @Override
  public void close() throws IOException {
  }

  @Override
  public void connect() throws IOException {
    mAlluxioFs = FileSystem.Factory.get();
  }

  @Override
  public boolean create(String path) throws IOException {
    AlluxioURI uri = new AlluxioURI(path);
    try {
      return (mAlluxioFs.createFile(uri, mWriteOptions) != null);
    } catch (AlluxioException e) {
      return false;
    }
  }

  @Override
  public boolean delete(String path, boolean recursive) throws IOException {
    try {
      mAlluxioFs.delete(new AlluxioURI(path), DeletePOptions.newBuilder().setRecursive(true).build());
      return true;
    } catch (AlluxioException e) {
      return false;
    }
  }

  @Override
  public boolean exists(String path) throws IOException {
    try {
      return mAlluxioFs.exists(new AlluxioURI(path));
    } catch (AlluxioException e) {
      return false;
    }
  }

  @Override
  public InputStream getInputStream(String path) throws IOException {
    AlluxioURI uri = new AlluxioURI(path);
    try {
      return mAlluxioFs.openFile(uri, mReadOptions);
    } catch (AlluxioException e) {
      throw new IOException();
    }
  }

  @Override
  public long getLength(String path) throws IOException {
    AlluxioURI uri = new AlluxioURI(path);
    try {
      return mAlluxioFs.getStatus(uri).getLength();
    } catch (AlluxioException e) {
      throw new IOException();
    }
  }

  @Override
  public long getModificationTime(String path) throws IOException {
    AlluxioURI uri = new AlluxioURI(path);
    try {
      return mAlluxioFs.getStatus(uri).getLastModificationTimeMs();
    } catch (AlluxioException e) {
      throw new IOException();
    }
  }

  @Override
  public OutputStream getOutputStream(String path) throws IOException {
    AlluxioURI uri = new AlluxioURI(path);
    try {
      return mAlluxioFs.createFile(uri, mWriteOptions);
    } catch (AlluxioException e) {
      throw new IOException();
    }
  }

  @Override
  public String getParent(String path) throws IOException {
    AlluxioURI uri = new AlluxioURI(path).getParent();
    if (uri == null) {
      return null;
    } else {
      return uri.getPath();
    }
  }

  @Override
  public boolean isDirectory(String path) throws IOException {
    AlluxioURI uri = new AlluxioURI(path);
    try {
      return mAlluxioFs.getStatus(uri).isFolder();
    } catch (AlluxioException e) {
      return false;
    }
  }

  @Override
  public boolean isFile(String path) throws IOException {
    AlluxioURI uri = new AlluxioURI(path);
    try {
      return !mAlluxioFs.getStatus(uri).isFolder();
    } catch (AlluxioException e) {
      return false;
    }
  }

  @Override
  public List<String> list(String path) throws IOException {
    List<URIStatus> files;
    try {
      files = mAlluxioFs.listStatus(new AlluxioURI(path));
    } catch (AlluxioException e) {
      throw new IOException();
    }
    if (files == null) {
      return null;
    }
    ArrayList<String> ret = new ArrayList<String>(files.size());
    for (URIStatus fileInfo : files) {
      ret.add(fileInfo.getPath());
    }
    return ret;
  }

  @Override
  public boolean mkdir(String path, boolean createParent) throws IOException {
    AlluxioURI uri = new AlluxioURI(path);
    try {
      mAlluxioFs.createDirectory(uri, CreateDirectoryPOptions.newBuilder().setRecursive(createParent).build());
      return true;
    } catch (InvalidPathException e) {
      return false;
    } catch (FileAlreadyExistsException e) {
      return false;
    } catch (AlluxioException e) {
      return false;
    }
  }

  @Override
  public boolean rename(String src, String dst) throws IOException {
    AlluxioURI srcURI = new AlluxioURI(src);
    AlluxioURI dstURI = new AlluxioURI(dst);
    try {
      mAlluxioFs.rename(srcURI, dstURI);
      return true;
    } catch (FileDoesNotExistException e) {
      return false;
    } catch (AlluxioException e) {
      return false;
    }
  }
}

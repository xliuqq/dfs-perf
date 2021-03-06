package pasalab.dfs.perf.benchmark.metadata;

import java.io.IOException;

import pasalab.dfs.perf.basic.PerfThread;
import pasalab.dfs.perf.basic.TaskConfiguration;
import pasalab.dfs.perf.benchmark.Operators;
import pasalab.dfs.perf.conf.PerfConf;
import pasalab.dfs.perf.fs.PerfFileSystem;

public class MetadataThread extends PerfThread {
  private PerfFileSystem[] mClients;
  private int mClientsNum;
  private int mOpTimeMs;
  private String mWorkDir;

  // in ops/sec
  private double mRate;
  private boolean mSuccess;

  public double getRate() {
    return mRate;
  }

  public boolean getSuccess() {
    return mSuccess;
  }

  public void run() {
    int currentOps = 0;
    int nextClient = 0;
    mSuccess = true;
    long timeMs = System.currentTimeMillis();
    while ((System.currentTimeMillis() - timeMs) < mOpTimeMs) {
      try {
        currentOps += Operators.metadataSample(mClients[nextClient], mWorkDir + "/" + mId);
      } catch (IOException e) {
        LOG.error("Failed to do metadata operation", e);
        mSuccess = false;
      }
      nextClient = (nextClient + 1) % mClientsNum;
    }
    timeMs = System.currentTimeMillis() - timeMs;
    mRate = (currentOps / 1.0) / (timeMs / 1000.0);
  }

  @Override
  public boolean setupThread(TaskConfiguration taskConf) {
    mClientsNum = taskConf.getIntProperty("clients.per.thread");
    mOpTimeMs = taskConf.getIntProperty("op.second.per.thread") * 1000;
    mWorkDir = taskConf.getProperty("work.dir");
    try {
      mClients = new PerfFileSystem[mClientsNum];
      String dfsAddress = PerfConf.get().DFS_ADDRESS;
      for (int i = 0; i < mClientsNum; i++) {
        mClients[i] = Operators.connect(dfsAddress, taskConf);
      }
    } catch (IOException e) {
      LOG.error("Failed to setup thread, task " + mTaskId + " - thread " + mId, e);
      return false;
    }
    mRate = 0;
    mSuccess = false;
    return true;
  }

  @Override
  public boolean cleanupThread(TaskConfiguration taskConf) {
    for (int i = 0; i < mClientsNum; i++) {
      try {
        Operators.close(mClients[i]);
      } catch (IOException e) {
        LOG.warn("Error when close file system, task " + mTaskId + " - thread " + mId, e);
      }
    }
    return true;
  }

}

import com.lind.algorithm.wheel.*;
import java.util.concurrent.*;
public class WheelDebug {
  public static void main(String[] args) throws Exception {
    ExecutorService worker = Executors.newSingleThreadExecutor();
    HashedWheelTimer timer = new HashedWheelTimer(10, 4, worker, true, 30, (t,e)->e.printStackTrace());
    CountDownLatch latch = new CountDownLatch(1);
    long start = System.currentTimeMillis();
    Timeout to = timer.newTimeout(t -> { System.out.println("FIRED after " + (System.currentTimeMillis()-start)); latch.countDown(); }, 120, TimeUnit.MILLISECONDS);
    System.out.println("scheduled pending="+timer.pendingTasks()+" cancelled="+to.isCancelled()+" expired="+to.isExpired());
    boolean ok = latch.await(3, TimeUnit.SECONDS);
    System.out.println("ok="+ok+" pending="+timer.pendingTasks()+" completed="+timer.completedTasks()+" expired="+to.isExpired());
    timer.stop();
  }
}

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

class Worker extends Thread {
    private final CustomThreadPool pool;
    private final long keepAliveTime;
    private final TimeUnit unit;
    private final BlockingQueue<Runnable> queue;
    private volatile boolean running = true;

    public Worker(CustomThreadPool pool, long keepAliveTime, TimeUnit unit,
                  BlockingQueue<Runnable> queue, String name) {
        super(name);
        this.pool = pool;
        this.keepAliveTime = keepAliveTime;
        this.unit = unit;
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            while (running && (!pool.isShutdown() || !queue.isEmpty())) {
                Runnable task = queue.poll(keepAliveTime, unit);
                if (task != null) {
                    try {
                        System.out.println("[Worker] " + getName() + " executes " + task);
                        task.run();
                    } finally {
                        pool.taskCompleted();
                    }
                } else if (pool.shouldTerminate()) {
                    System.out.println("[Worker] " + getName() + " idle timeout, stopping.");
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            running = false;
            pool.workerTerminated(this);
            System.out.println("[Worker] " + getName() + " terminated.");
        }
    }
}
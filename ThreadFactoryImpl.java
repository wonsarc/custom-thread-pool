import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

class ThreadFactoryImpl implements ThreadFactory {
    private final String poolName;
    private final AtomicInteger counter = new AtomicInteger(0);

    public ThreadFactoryImpl(String poolName) {
        this.poolName = poolName;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, poolName + "-worker-" + counter.incrementAndGet());
        System.out.println("[ThreadFactory] Creating new thread: " + thread.getName());
        return thread;
    }
}

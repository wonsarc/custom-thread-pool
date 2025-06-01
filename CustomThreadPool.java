import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

class CustomThreadPool implements CustomExecutor {
    private final int corePoolSize;
    private final int maxPoolSize;
    private final long keepAliveTime;
    private final TimeUnit timeUnit;
    private final int queueSize;
    private final int minSpareThreads;
    private final RejectionPolicy rejectionPolicy;
    private final ThreadFactory threadFactory;
    private final List<BlockingQueue<Runnable>> queues;
    private final List<Worker> workers = new ArrayList<>();
    private final AtomicInteger activeWorkers = new AtomicInteger();
    private final AtomicInteger idleWorkers = new AtomicInteger();
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);
    private final ReentrantLock poolLock = new ReentrantLock();
    private volatile boolean shutdown = false;
    private volatile boolean terminated = false;
    private final AtomicInteger nextWorkerIndex = new AtomicInteger(0);

    public CustomThreadPool(String name, int corePoolSize, int maxPoolSize, long keepAliveTime,
                            TimeUnit timeUnit, int queueSize, int minSpareThreads,
                            RejectionPolicy rejectionPolicy) {
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.queueSize = queueSize;
        this.minSpareThreads = minSpareThreads;
        this.rejectionPolicy = rejectionPolicy;
        this.threadFactory = new ThreadFactoryImpl(name);

        queues = new ArrayList<>(maxPoolSize);
        for (int i = 0; i < maxPoolSize; i++) {
            queues.add(new ArrayBlockingQueue<>(queueSize));
        }

        for (int i = 0; i < corePoolSize; i++) {
            createWorker();
        }
    }

    @Override
    public void execute(Runnable command) {
        if (shutdown) {
            throw new RejectedExecutionException("Executor is shutdown");
        }

        NamedTask task = new NamedTask(command, "Task-" + UUID.randomUUID().toString());

        if (!addToAnyQueue(task)) {
            handleOverload(task);
        } else {
            ensureMinSpareThreads();
        }
    }

    private boolean addToAnyQueue(Runnable task) {
        int start = roundRobinIndex.getAndIncrement();
        for (int i = 0; i < queues.size(); i++) {
            int idx = (start + i) % queues.size();
            BlockingQueue<Runnable> q = queues.get(idx);
            if (q.offer(task)) {
                System.out.println("[Pool] Task accepted into queue #" + idx + ": " + task);
                return true;
            }
        }
        return false;
    }

    private void handleOverload(Runnable task) {
        poolLock.lock();
        try {
            if (activeWorkers.get() < maxPoolSize) {
                if (createWorker()) {
                    if (addToAnyQueue(task)) {
                        return;
                    }
                }
            }
            rejectionPolicy.reject(task, this);
        } finally {
            poolLock.unlock();
        }
    }

    private boolean createWorker() {
        if (activeWorkers.get() >= maxPoolSize) {
            return false;
        }

        int index = nextWorkerIndex.getAndIncrement();
        if (index >= maxPoolSize) {
            nextWorkerIndex.decrementAndGet();
            return false;
        }

        BlockingQueue<Runnable> queue = queues.get(index);
        Worker worker = new Worker(this, keepAliveTime, timeUnit, queue,
                threadFactory.newThread(null).getName());
        worker.start();

        workers.add(worker);
        activeWorkers.incrementAndGet();
        idleWorkers.incrementAndGet();
        return true;
    }

    private void ensureMinSpareThreads() {
        if (idleWorkers.get() < minSpareThreads && activeWorkers.get() < maxPoolSize) {
            createWorker();
        }
    }

    public void taskCompleted() {
        idleWorkers.incrementAndGet();
    }

    public boolean shouldTerminate() {
        return activeWorkers.get() > corePoolSize;
    }

    public void workerTerminated(Worker worker) {
        poolLock.lock();
        try {
            if (workers.remove(worker)) {
                activeWorkers.decrementAndGet();
                idleWorkers.decrementAndGet();
                if (activeWorkers.get() == 0 && shutdown) {
                    terminated = true;
                }
            }
        } finally {
            poolLock.unlock();
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        FutureTask<T> task = new FutureTask<>(callable);
        execute(task);
        return task;
    }

    @Override
    public void shutdown() {
        poolLock.lock();
        try {
            shutdown = true;
            for (Worker worker : workers) {
                worker.interrupt();
            }
        } finally {
            poolLock.unlock();
        }
    }

    @Override
    public void shutdownNow() {
        shutdown();
        poolLock.lock();
        try {
            for (BlockingQueue<Runnable> queue : queues) {
                queue.clear();
            }
        } finally {
            poolLock.unlock();
        }
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public boolean isTerminated() {
        return terminated;
    }
}
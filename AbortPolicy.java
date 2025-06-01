import java.util.concurrent.RejectedExecutionException;

class AbortPolicy implements RejectionPolicy {
    @Override
    public void reject(Runnable task, CustomThreadPool pool) {
        System.out.println("[Rejected] Task " + task + " was rejected due to overload!");
        throw new RejectedExecutionException("Task rejected: " + task);
    }
}
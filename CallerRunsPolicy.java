class CallerRunsPolicy implements RejectionPolicy {
    @Override
    public void reject(Runnable task, CustomThreadPool pool) {
        System.out.println("[Rejected] Executing task in caller thread: " + task);
        task.run();
    }
}
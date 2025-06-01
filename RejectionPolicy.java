interface RejectionPolicy {
    void reject(Runnable task, CustomThreadPool pool);
}
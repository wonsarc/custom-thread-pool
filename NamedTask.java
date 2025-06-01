class NamedTask implements Runnable {
    private final Runnable task;
    private final String name;

    public NamedTask(Runnable task, String name) {
        this.task = task;
        this.name = name;
    }

    @Override
    public void run() {
        task.run();
    }

    @Override
    public String toString() {
        return name;
    }
}
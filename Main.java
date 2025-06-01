import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

class Main {
    public static void main(String[] args) throws InterruptedException {
        CustomExecutor executor = new CustomThreadPool(
                "MyPool",
                2,
                4,
                5,
                TimeUnit.SECONDS,
                3,
                1,
                new CallerRunsPolicy()
        );

        for (int i = 0; i < 20; i++) {
            final int id = i;
            try {
                executor.execute(() -> {
                    System.out.println("[Task] Task " + id + " started in " + Thread.currentThread().getName());
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        System.out.println("[Task] Task " + id + " interrupted");
                    }
                    System.out.println("[Task] Task " + id + " finished");
                });
            } catch (RejectedExecutionException e) {
                System.out.println("[Main] Task " + id + " rejected");
            }
            Thread.sleep(200);
        }

        Thread.sleep(3000);
        executor.shutdown();

        try {
            executor.execute(() -> System.out.println("Should be rejected"));
        } catch (RejectedExecutionException e) {
            System.out.println("[Main] Task correctly rejected after shutdown");
        }
    }
}
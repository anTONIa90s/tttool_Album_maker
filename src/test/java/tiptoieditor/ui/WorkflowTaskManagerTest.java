package tiptoieditor.ui;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowTaskManagerTest {

    @Test
    void cancelAllInterruptsTrackedBackgroundTasks() throws InterruptedException {
        WorkflowTaskManager taskManager = new WorkflowTaskManager();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);

        taskManager.start("test-task", () -> {
            started.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException e) {
                interrupted.countDown();
                Thread.currentThread().interrupt();
            }
        });

        assertTrue(started.await(1, TimeUnit.SECONDS));
        assertTrue(taskManager.isRunning());

        taskManager.cancelAll();

        assertTrue(interrupted.await(1, TimeUnit.SECONDS));
        assertTrue(waitUntilStopped(taskManager));
        assertFalse(taskManager.isRunning());
    }

    private boolean waitUntilStopped(WorkflowTaskManager taskManager) throws InterruptedException {
        for (int attempt = 0; attempt < 20; attempt++) {
            if (!taskManager.isRunning()) {
                return true;
            }
            Thread.sleep(10);
        }
        return false;
    }
}

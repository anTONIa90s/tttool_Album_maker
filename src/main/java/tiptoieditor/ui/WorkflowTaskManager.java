package tiptoieditor.ui;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Tracks background tasks and child processes so the UI can cancel them together. */
public class WorkflowTaskManager {

    private final Set<Thread> runningThreads = ConcurrentHashMap.newKeySet();
    private final Set<Process> runningProcesses = ConcurrentHashMap.newKeySet();
    private Consumer<Boolean> runningChangedListener = running -> {
    };

    public void start(String name, Runnable task) {
        Thread thread = new Thread(() -> {
            try {
                task.run();
            } finally {
                runningThreads.remove(Thread.currentThread());
                notifyRunningChanged();
            }
        }, name);
        runningThreads.add(thread);
        notifyRunningChanged();
        thread.start();
    }

    /** Registers a process started by a tracked task. */
    public void register(Process process) {
        runningProcesses.add(process);
    }

    /** Removes a process after it exits. */
    public void unregister(Process process) {
        runningProcesses.remove(process);
    }

    /** Interrupts all background tasks and forcibly ends their process trees. */
    public void cancelAll() {
        for (Process process : runningProcesses) {
            process.toHandle().descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
        }
        for (Thread thread : runningThreads) {
            thread.interrupt();
        }
    }

    public boolean isRunning() {
        return !runningThreads.isEmpty() || !runningProcesses.isEmpty();
    }

    public void setOnRunningChanged(Consumer<Boolean> runningChangedListener) {
        this.runningChangedListener = runningChangedListener;
        notifyRunningChanged();
    }

    private void notifyRunningChanged() {
        runningChangedListener.accept(isRunning());
    }
}

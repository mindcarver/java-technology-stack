package codecarver.chapter12;

import java.util.LinkedList;

public class Demo1 {
    private final static LinkedList<Runnable> TASK_QUEUE = new LinkedList<>();
    private enum TaskState {
        FREE, RUNNING, BLOCKED, DEAD
    }
    private static class ServiceTask extends Thread {

        private volatile TaskState taskState = TaskState.FREE;

        public ServiceTask(ThreadGroup group, String name) {
            super(group, name);
        }

        public TaskState getTaskState() {
            return this.taskState;
        }

        public void run() {

        }

        public void close() {
            this.taskState = TaskState.DEAD;
        }
    }
}

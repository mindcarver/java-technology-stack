package codecarver.chapter12;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/***************************************
 * @author:Alex Wang
 * @Date:2017/2/25 QQ:532500648
 * QQ交流群:286081824
 ***************************************/
public class SimpleThreadPool extends Thread {

    private int size;

    private final int queueSize;

    private final static int DEFAULT_TASK_QUEUE_SIZE = 2000;

    private static volatile int seq = 0;

    private final static String THREAD_PREFIX = "SIMPLE_THREAD_POOL-";

    private final static ThreadGroup GROUP = new ThreadGroup("Pool_Group");

    // 任务队列
    private final static LinkedList<Runnable> TASK_QUEUE = new LinkedList<>();

    private final static List<ServiceTask> THREAD_QUEUE = new ArrayList<>();

    private final DiscardPolicy discardPolicy;

    public final static DiscardPolicy DEFAULT_DISCARD_POLICY = () -> {
        throw new DiscardException("Discard This Task.");
    };

    private volatile boolean destroy = false;

    private int min;

    private int max;

    private int active;

    public SimpleThreadPool() {
        this(4, 8, 12, DEFAULT_TASK_QUEUE_SIZE, DEFAULT_DISCARD_POLICY);
    }

    public SimpleThreadPool(int min, int active, int max, int queueSize, DiscardPolicy discardPolicy) {
        this.min = min;
        this.active = active;
        this.max = max;
        this.queueSize = queueSize;
        this.discardPolicy = discardPolicy;
        init();
    }

    private void init() {
        for (int i = 0; i < this.min; i++) {
            createWorkTask();
        }
        this.size = min;
        this.start();
    }

    public void submit(Runnable runnable) {
        if (destroy)
            throw new IllegalStateException("The thread pool already destroy and not allow submit task.");

        synchronized (TASK_QUEUE) {
            if (TASK_QUEUE.size() > queueSize)
                discardPolicy.discard();
            TASK_QUEUE.addLast(runnable);
            TASK_QUEUE.notifyAll();
        }
    }

    @Override
    public void run() {
        while (!destroy) {
            System.out.printf("Pool#Min:%d,Active:%d,Max:%d,Current:%d,QueueSize:%d\n",
                    this.min, this.active, this.max, this.size, TASK_QUEUE.size());
            try {
                Thread.sleep(5_000L);
                if (TASK_QUEUE.size() > active && size < active) {
                    for (int i = size; i < active; i++) {
                        createWorkTask();
                    }
                    System.out.println("The pool incremented to active.");
                    size = active;
                } else if (TASK_QUEUE.size() > max && size < max) {
                    for (int i = size; i < max; i++) {
                        createWorkTask();
                    }
                    System.out.println("The pool incremented to max.");
                    size = max;
                }

                synchronized (THREAD_QUEUE) {
                    if (TASK_QUEUE.isEmpty() && size > active) {
                        System.out.println("=========Reduce========");
                        int releaseSize = size - active;
                        for (Iterator<ServiceTask> it = THREAD_QUEUE.iterator(); it.hasNext(); ) {
                            if (releaseSize <= 0)
                                break;

                            ServiceTask task = it.next();
                            task.close();
                            task.interrupt();
                            it.remove();
                            releaseSize--;
                        }
                        size = active;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void createWorkTask() {
        ServiceTask task = new ServiceTask(GROUP, THREAD_PREFIX + (seq++));
        task.start();
        THREAD_QUEUE.add(task);
    }

    public void shutdown() throws InterruptedException {
        while (!TASK_QUEUE.isEmpty()) {
            Thread.sleep(50);
        }

        synchronized (THREAD_QUEUE) {
            int initVal = THREAD_QUEUE.size();
            while (initVal > 0) {
                for (ServiceTask task : THREAD_QUEUE) {
                    if (task.getTaskState() == TaskState.BLOCKED) {
                        task.interrupt();
                        task.close();
                        initVal--;
                    } else {
                        Thread.sleep(10);
                    }
                }
            }
        }

        System.out.println(GROUP.activeCount());

        this.destroy = true;
        System.out.println("The thread pool disposed.");
    }

    public int getQueueSize() {
        return queueSize;
    }

    public int getSize() {
        return size;
    }

    public boolean isDestroy() {
        return this.destroy;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public int getActive() {
        return active;
    }

    private enum TaskState {
        FREE, RUNNING, BLOCKED, DEAD
    }

    public static class DiscardException extends RuntimeException {

        public DiscardException(String message) {
            super(message);
        }
    }

    public interface DiscardPolicy {

        void discard() throws DiscardException;
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
            OUTER:
            while (this.taskState != TaskState.DEAD) {
                Runnable runnable;
                synchronized (TASK_QUEUE) {
                    while (TASK_QUEUE.isEmpty()) {
                        try {
                            taskState = TaskState.BLOCKED;
                            TASK_QUEUE.wait();
                        } catch (InterruptedException e) {
                            System.out.println("Closed.");
                            break OUTER;
                        }
                    }
                    runnable = TASK_QUEUE.removeFirst();
                }

                if (runnable != null) {
                    taskState = TaskState.RUNNING;
                    runnable.run();
                    taskState = TaskState.FREE;
                }
            }
        }

        public void close() {
            this.taskState = TaskState.DEAD;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        SimpleThreadPool threadPool = new SimpleThreadPool();
        for (int i = 0; i < 40; i++) {
            threadPool.submit(() -> {
                System.out.println("The runnable  be serviced by " + Thread.currentThread() + " start.");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("The runnable be serviced by " + Thread.currentThread() + " finished.");
            });
        }

        Thread.sleep(10000);
        threadPool.shutdown();

       /* Thread.sleep(10000);
        threadPool.shutdown();
        threadPool.submit(() -> System.out.println("======="));*/
    }
}

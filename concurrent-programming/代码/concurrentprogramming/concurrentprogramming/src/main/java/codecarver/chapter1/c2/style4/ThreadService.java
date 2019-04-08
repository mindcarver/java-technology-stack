package codecarver.chapter1.c2.style4;

public class ThreadService {

    private Thread executeThread;

    private boolean finished = false;

    public void execute(Runnable task) {
        executeThread = new Thread() {
            @Override
            public void run() {
                // 子线程runner是一个守护线程
                // 将其设置为守护线程，如果不join的话，那么excuteThread一旦run
                // 那么守护线程的生命周期立马会结束掉，所以在守护线程上进行join
                // 直到守护线程执行完毕再去执行excuteThread的业务逻辑
                Thread runner = new Thread(task);
                runner.setDaemon(true);

                runner.start();
                try {
                    runner.join();
                    // 守护线程执行完毕之后，设置flag为true
                    // 所以如果守护线程的生命周期很长，那么可以手动shutdown执行线程
                    finished = true;
                } catch (InterruptedException e) {
                }
            }
        };

        executeThread.start();
    }

    // 只给执行线程执行mills时间。
    public void shutdown(long mills) {
        long currentTime = System.currentTimeMillis();
        while (!finished) {
            if ((System.currentTimeMillis() - currentTime) >= mills) {
                System.out.println("任务超时，需要结束他!");
                // 通过打断来中断线程 executeThread结束，那么子线程runner也必须结束
                executeThread.interrupt();
                break;
            }


            // 既没有超时，也没有结束
            try {
                executeThread.sleep(1);
            } catch (InterruptedException e) {
                // 执行线程被打断
                System.out.println("执行线程被打断!");
                break;
            }
        }

        finished = false;
    }
}
package codecarver.chapter6.case1;

public class SynchronizedRunableDemo implements Runnable {

    private int index = 1;

    private final static int MAX = 200;

    private final Object MONITOR = new Object();

    @Override
    public void run() {

        while (true) {
            synchronized (MONITOR) {
                // 如果超过500 直接跳出
                if (index > MAX)
                    break;
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread() + " 的号码是:" + (index++));
            }
        }
    }
}



package codecarver.chapter8.classp;


public class Version1 {

    private int i = 1;

    final private Object LOCK = new Object();

    // 生产者
    private void produce() {
        synchronized (LOCK) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("P->" + (i++));
        }
    }

    // 消费者
    private void consume() {
        synchronized (LOCK) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("C->" + i);
        }
    }

    public static void main(String[] args) {

        Version1 v1 = new Version1();

        new Thread("P") {
            @Override
            public void run() {
                while (true)
                    v1.produce();
            }
        }.start();

        new Thread("C") {
            @Override
            public void run() {
                while (true)
                    v1.consume();
            }
        }.start();
    }
}

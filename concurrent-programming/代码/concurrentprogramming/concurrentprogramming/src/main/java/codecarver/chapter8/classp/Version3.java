package codecarver.chapter8.classp;

// 程序会假死
public class Version3 {

    private int i = 0;

    final private Object LOCK = new Object();

    private volatile boolean isProduced = false;

    public void produce() {
        synchronized (LOCK) {
            if (isProduced) {
                // 如果已经生产了，那么就去等待被消费把
                try {
                    LOCK.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                // 如果没有生产，那么生产一个，并且通知那谁来消费
                i++;
                System.out.println("P->" + i);
                // 我已经生产了，所以通知你来消费。
                LOCK.notify();
                isProduced = true;
            }
        }
    }

    public void consume() {
        synchronized (LOCK) {
            // 如果数据已经生产了，那么就去消费他
            if (isProduced) {
                System.out.println("C->" + i);
                // 消费之后将isproduced置为false，和上面的else里面对应起来
                // 通过锁的notIfy来告知他已经消费了。
                LOCK.notify();
                // 告诉你还得继续生产
                isProduced = false;
            } else {
                try {
                    // 如果没有生产，我就等
                    LOCK.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        Version3 v2 = new Version3();
        for (int i = 0; i < 10; i++) {
            new Thread("P" + i) {
                @Override
                public void run() {
                    while (true)
                        v2.produce();
                }
            }.start();
        }

        for (int i = 0; i < 10; i++) {
            new Thread("C" + i) {
                @Override
                public void run() {
                    while (true)
                        v2.consume();
                }
            }.start();
        }

    }
    
}
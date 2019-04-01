package codecarver.chapter8.classp;

// 解决程序假死以及为什么不能用if,要用while
public class Version4 {

    private int i = 0;

    final private Object LOCK = new Object();

    private volatile boolean isProduced = false;

    // 生产者
    public void produce() {
        synchronized (LOCK) {
            // 如果你已经生产了数据
            while(isProduced){
                // 如果已经生产了，那么就去等待被消费把
                try {
                    LOCK.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // 如果没有生产，被唤醒了
            // 那么生产一个，并且通知那谁来消费
            i++;
            System.out.println("P->" + i);
            // 我已经生产了，所以通知你来消费。
            LOCK.notifyAll();
            isProduced = true;
        }
    }

    // 消费者
    public void consume() {
        synchronized (LOCK) {
            while (!isProduced){
                // 如果生产者没有生产数据
                try {
                    // 如果没有生产，我就等
                    LOCK.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // 如果数据已经生产了，那么就去消费他
            System.out.println("C->" + i);
            // 消费之后将isproduced置为false，和上面的else里面对应起来
            // 通过锁的notIfy来告知他已经消费了。
            LOCK.notifyAll();
            // 告诉你还得继续生产
            isProduced = false;
        }
    }

    public static void main(String[] args) {
        Version4 v2 = new Version4();
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
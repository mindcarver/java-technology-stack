package codecarver.chapter8.classp;

import java.util.stream.Stream;

/***************************************
 * @author:Alex Wang
 * @Date:2017/2/19 QQ:532500648
 * QQ交流群:286081824
 ***************************************/
public class ProduceConsumerVersion3 {

    private int i = 0;

    final private Object LOCK = new Object();

    private volatile boolean isProduced = false;

    public void produce() {
        synchronized (LOCK) {
            while (isProduced) {
                try {
                    LOCK.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            i++;
            System.out.println("P->" + i);
            LOCK.notifyAll();
            isProduced = true;

        }
    }

    public void consume() {
        synchronized (LOCK) {
            while (!isProduced) {
                try {
                    LOCK.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("C->" + i);
            LOCK.notifyAll();
            isProduced = false;
        }
    }

    public static void main(String[] args) {
        ProduceConsumerVersion3 pc = new ProduceConsumerVersion3();
        Stream.of("P1", "P2", "P3").forEach(n ->
                new Thread(n) {
                    @Override
                    public void run() {
                        while (true) {
                            pc.produce();
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }.start()
        );
        Stream.of("C1", "C2", "C3", "C4").forEach(n ->
                new Thread(n) {
                    @Override
                    public void run() {
                        while (true) {
                            pc.consume();
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }.start()
        );
    }
}
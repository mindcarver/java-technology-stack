package codecarver.chapter17;

import java.util.concurrent.CyclicBarrier;

/**
 * @ClassName demo3
 * @Description TODO
 * @Author lenovo
 * @Date 2019/1/30 16:20
 **/
public class CyclicBarrierDemo {
    // 自定义工作线程
    private static class Worker extends Thread {
        private CyclicBarrier cyclicBarrier;

        public Worker(CyclicBarrier cyclicBarrier) {
            this.cyclicBarrier = cyclicBarrier;
        }

        @Override
        public void run() {
            super.run();

            try {
                System.out.println(Thread.currentThread().getName() + "开始等待其他线程");
                cyclicBarrier.await();
                System.out.println(Thread.currentThread().getName() + "开始执行");
                // 工作线程开始处理，这里用Thread.sleep()来模拟业务处理
                Thread.sleep(5_000);
                System.out.println(Thread.currentThread().getName() + "执行完毕");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        CyclicBarrier cyclicBarrier = new CyclicBarrier(3);

        for (int i = 0; i < 3; i++) {
            Worker worker = new Worker(cyclicBarrier);
            worker.start();
        }
    }
}

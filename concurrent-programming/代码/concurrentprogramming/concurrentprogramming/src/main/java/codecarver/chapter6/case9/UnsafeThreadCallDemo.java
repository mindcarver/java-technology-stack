package codecarver.chapter6.case9;

import codecarver.chapter9.Lock;

/**
 * @ClassName UnsafeThreadCallDemo
 * @Description 不安全的线程调用演示
 * @Author lenovo
 * @Date 2019/1/24 19:56
 **/
public class UnsafeThreadCallDemo {
    private static int i = 0;
    public static class Mythread extends Thread{
        @Override
        public void run() {
            for (int j = 0; j < 1000; j++) {
                i++;
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Mythread t1 = new Mythread();
        Mythread t2 = new Mythread();
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println("i最后的值是：" + i);
    }
}

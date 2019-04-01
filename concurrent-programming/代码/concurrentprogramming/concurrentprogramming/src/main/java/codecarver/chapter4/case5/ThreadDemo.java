package codecarver.chapter4.case5;

import com.sun.xml.internal.stream.util.ThreadLocalBufferAllocator;

/**
 * @ClassName ThreadDemo
 * @Description TODO
 * @Author lenovo
 * @Date 2019/1/24 18:48
 **/
public class ThreadDemo {
    private static int i = 0;
    public static class Mythread extends Thread{
        @Override
        public void run() {
            try {
                Thread.sleep(3_000);
            } catch (InterruptedException e) {
                System.out.println("我在睡眠的时候被中断了");
            }
            for (; i < 10; i++);
            System.out.println("i的值是：" + i);
        }
    }

    public static void main(String[] args) {
        Mythread t1 = new Mythread();
        t1.start();
    }
}

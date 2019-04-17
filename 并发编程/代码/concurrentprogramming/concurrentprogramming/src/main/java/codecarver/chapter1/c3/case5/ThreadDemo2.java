package codecarver.chapter1.c3.case5;

/**
 * @ClassName ThreadDemo2
 * @Description 测试sleep方法由于中断而抛出异常的话是会清除中断标志位的
 * @Author lenovo
 * @Date 2019/1/24 19:07
 **/
public class ThreadDemo2 {
    public static class Mythread extends Thread{
        @Override
        public void run() {
            try {
                Thread.sleep(2_000);
            } catch (InterruptedException e) {
                // 注释这段来验证
                Thread.currentThread().interrupt();
            }
            System.out.println(Thread.currentThread().isInterrupted());
        }
    }

    public static void main(String[] args) {
        Mythread t1 = new Mythread();
        t1.start();
        t1.interrupt();
    }
}

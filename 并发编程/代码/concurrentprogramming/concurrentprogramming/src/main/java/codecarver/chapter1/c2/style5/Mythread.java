package codecarver.chapter1.c2.style5;

// 通过sleep和interrupt来控制线程停止
// 如果不通过interrupt ，那么线程不会再中途停止，会正常结束
public class Mythread extends Thread {
    @Override
    public void run() {
        System.out.println("开始");
        try {
            Thread.sleep(10_000);
            System.out.println("正常结束");
        } catch (InterruptedException e) {
            System.out.println("异常结束");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Thread myThread  = new Mythread();
        myThread.start();

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            System.out.println("main方法捕获到异常");
            e.printStackTrace();
        }
        /*myThread.interrupt();*/
        System.out.println("主线程main结束");
    }
}

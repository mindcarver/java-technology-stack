package codecarver.chapter1.c1;

// 实现runable接口
public class MyRunable implements Runnable {
    public void run() {
        try {
            Thread.sleep(5000);
            System.out.println("我的线程");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
    static class   MyThread2Demo{
        public static void main(String[] args) {
            MyRunable myRunable = new MyRunable();
            Thread t = new Thread(myRunable);
            t.start();
            System.out.println("main线程");
        }
    }
}

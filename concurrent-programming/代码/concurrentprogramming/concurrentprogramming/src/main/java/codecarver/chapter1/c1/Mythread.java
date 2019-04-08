package codecarver.chapter1.c1;

// 继承实现
public class Mythread extends Thread {
    @Override
    public void run() {
        try {
            Thread.sleep(5000);
            System.out.println("我的线程是：" + Thread.currentThread().getName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    static class MyTheadDemo{
        public static void main(String[] args) {
            Mythread mt = new Mythread();
            mt.run();
            //mt.start();
            System.out.println("main线程的线程："  + Thread.currentThread().getName());
        }
    }
}

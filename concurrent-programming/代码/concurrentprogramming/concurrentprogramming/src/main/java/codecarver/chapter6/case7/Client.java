package codecarver.chapter6.case7;

public class Client {
    public static void main(String[] args) {
        Thread t1 = new Thread(new Runnable(){
            @Override
            public void run() {
                SynchronizedStaticInstanceDemo.addM1();
            }
        },"Thread-1");

        Thread t2 = new Thread(new Runnable(){
            @Override
            public void run() {
                SynchronizedStaticInstanceDemo.addM2();
            }
        },"Thread-2");


        Thread t3 = new Thread(new Runnable(){
            @Override
            public void run() {
                SynchronizedStaticInstanceDemo.addM1();
            }
        },"Thread-3");

        Thread t4 = new Thread(new Runnable(){
            @Override
            public void run() {
                SynchronizedStaticInstanceDemo.addM2();
            }
        },"Thread-4");

        t1.start();
        t2.start();
        t3.start();
        t4.start();
    }
}

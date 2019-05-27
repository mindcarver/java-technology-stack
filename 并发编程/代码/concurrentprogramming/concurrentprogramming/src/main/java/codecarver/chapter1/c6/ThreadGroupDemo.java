package codecarver.chapter1.c6;

public class ThreadGroupDemo implements Runnable{
    public static void main(String[] args) {
        ThreadGroup tg = new ThreadGroup("TestGroup");
        Thread t1 = new Thread(tg, new ThreadGroupDemo(), "T1");
        Thread t2 = new Thread(tg, new ThreadGroupDemo(), "T2");

        t1.start();
        t2.start();
        System.out.println("线程组中有：" + tg.activeCount()+"个活动线程");
        tg.list();
    }

    @Override
    public void run() {
        String groupAndNAme = Thread.currentThread().getThreadGroup().getName()
                + "-" + Thread.currentThread().getName();
        while(true){
            System.out.println("I am " + groupAndNAme);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

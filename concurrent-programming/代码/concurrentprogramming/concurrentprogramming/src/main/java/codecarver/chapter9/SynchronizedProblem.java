package codecarver.chapter9;

/***************************************
 * @author:Alex Wang
 * @Date:2017/2/22 QQ:532500648
 * QQ交流群:286081824
 ***************************************/
public class SynchronizedProblem {

    public static void main(String[] args) throws InterruptedException {

        new Thread() {
            @Override
            public void run() {
                SynchronizedProblem.run();
            }
        }.start();

        Thread.sleep(1000);

        Thread t2 = new Thread() {
            @Override
            public void run() {
//                /sdfsdfsd
                SynchronizedProblem.run();
                //sdfsdfsd
            }
        };
        t2.start();
        Thread.sleep(2000);
        t2.interrupt();
        System.out.println(t2.isInterrupted());
    }

    private synchronized static void run() {
        System.out.println(Thread.currentThread());
        while (true) {

        }
    }
}

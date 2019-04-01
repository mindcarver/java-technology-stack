/**
 * @ClassName testSynchronized
 * @Description TODO
 * @Author lenovo
 * @Date 2019/2/12 12:53
 **/
public class testSynchronized {
    static class MyThread extends Thread{
        @Override
        public void run() {
            synchronized (this){
                printIn();
            }
        }

        public synchronized void printIn(){
            System.out.println(1);
        }
    }
    public static void main(String[] args) {
        Thread t1 = new MyThread();
        t1.start();
    }
}

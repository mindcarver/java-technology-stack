package codecarver.charpter3.style3;

public class Client {
    public static void main(String[] args) throws InterruptedException {
        StopThread t1 = new StopThread();
        t1.start();
        // main线程睡2秒，子线程执行了一部分
        Thread.sleep(2000);
        t1.interrupt();
        //确保线程已经终止
        while(t1.isAlive()){
        }
        //输出结果
        t1.print();
    }
}

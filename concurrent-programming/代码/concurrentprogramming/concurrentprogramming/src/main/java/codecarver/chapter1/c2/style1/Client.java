package codecarver.chapter1.c2.style1;

public class Client {
    public static void main(String[] args) throws InterruptedException {
        StopThread t1 = new StopThread();
        t1.start();
        // 睡6秒，表示执行了一部分
        Thread.sleep(2000);
        t1.stop();
        //输出结果
        t1.print();
    }
}

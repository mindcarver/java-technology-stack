package codecarver.charpter3.style6;

// 这部分先打断，再让线程睡眠
// 对比style5里面的先睡眠再打断方式
// 主线程标记你为中断线程，你自己再睡眠，那么直接把你扔进异常中
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
        myThread.interrupt();
        System.out.println("主线程main结束");
    }
}

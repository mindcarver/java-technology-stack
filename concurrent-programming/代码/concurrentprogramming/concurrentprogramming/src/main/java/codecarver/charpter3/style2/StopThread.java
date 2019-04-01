package codecarver.charpter3.style2;

public class StopThread extends Thread{
    private int i = 0;
    private int j = 0;
    volatile boolean stopme = false;
    public void stopMe(){
        stopme = true;
    }

    @Override
    public void run() {
        while (true){
            if(stopme){
                System.out.println("通过标记变量退出");
                break;
            }
            synchronized (StopThread.class){
                ++i;
                try {
                    // 模拟做加法要耗时5秒
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                ++j;
            }
        }
    }
    public void print(){
        // 打印出i和j
        System.out.println("线程：" + Thread.currentThread().getName() + ":i=" + i + "j=" + j);
    }
}

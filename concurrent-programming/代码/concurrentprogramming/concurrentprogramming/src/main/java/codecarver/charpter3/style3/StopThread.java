package codecarver.charpter3.style3;

public class StopThread extends Thread{
    private int i = 0;
    private int j = 0;

    @Override
    public void run() {
        while (true){
            if(Thread.currentThread().isInterrupted()){
                System.out.println("通过中断退出");
                break;
            }
            synchronized (StopThread.class){
                ++i;
                try {
                    // 模拟做加法要耗时50秒
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    // 设置中断状态
                    // 这里我们没有break，而是进行了再次设置中断标志位来进行后续的++j操作
                    /*Thread.currentThread().interrupt();*/
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

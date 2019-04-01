package codecarver.base.interrupt;

/**
 * @ClassName InterruptDemo
 * @Description 中断线程退出
 * @Author lenovo
 * @Date 2019/1/23 21:04
 **/
public class InterruptDemo2 {
    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(){
            @Override
            public void run() {
                while (true){
                    System.out.println("我还没被中断呢");
                   if(Thread.currentThread().isInterrupted()){
                       System.out.println("我被中断了");
                       break;
                   }
                }
            }
        };
        t1.start();
        Thread.sleep(2_000);
        //中断t1
        t1.interrupt();
    }
}

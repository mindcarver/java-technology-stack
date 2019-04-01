package codecarver.base.interrupt;

/**
 * @ClassName InterruptDemo
 * @Description TODO
 * @Author lenovo
 * @Date 2019/1/23 21:04
 **/
public class InterruptDemo {
    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(){
            @Override
            public void run() {
                while (true){
                    System.out.println("中断。。。");
                }
            }
        };
        t1.start();
        Thread.sleep(2_000);
        //中断t1
        t1.interrupt();
    }
}

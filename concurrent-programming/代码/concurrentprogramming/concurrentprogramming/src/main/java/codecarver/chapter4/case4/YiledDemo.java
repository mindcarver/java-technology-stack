package codecarver.chapter4.case4;

/**
 * @ClassName YiledDemo
 * @Description TODO
 * @Author lenovo
 * @Date 2019/1/24 9:10
 **/
public class YiledDemo {

    public static void main(String[]args)
    {
        MyThread t = new MyThread();
        t.start();

        for (int i=0; i<5; i++)
        {
            // 将控制权交给了子线程（Mythread）
            Thread.yield();
            // 当执行完子线程之后，main线程继续接手
            System.out.println(Thread.currentThread().getName()
                    + "正在控制");
        }
    }

    static class MyThread extends Thread
    {
        public void run()
        {
            for (int i=0; i<5 ; i++)
                System.out.println(Thread.currentThread().getName()
                        + "正在控制");
        }
    }
}

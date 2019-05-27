package codecarver.chapter1.c3.case1;

public class Client {
    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(){
            @Override
            public void run() {
                System.out.println("测试 sleep方法");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        t1.start();
        /*
        * 关键就在这一步，本来如果线程在睡眠的时候遇到中断，那么会触发中断异常
        * 下面的这一步睡眠4秒，我不加的话，t1线程执行就会睡2秒，然后main线程
        * 执行t1.interrupt，会触发异常，如果我让main线程睡4秒，那么t1线程
        * 执行完都不会被中断，所以也不会抛出异常。
        * */
        Thread.sleep(4000);
        t1.interrupt();
    }
}

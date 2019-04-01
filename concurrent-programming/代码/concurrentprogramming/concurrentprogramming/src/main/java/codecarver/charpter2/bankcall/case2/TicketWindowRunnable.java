package codecarver.charpter2.bankcall.case2;


public class TicketWindowRunnable implements Runnable {

    private final int NUMBER_MAX = 50;

    private static   int index = 1;

    public TicketWindowRunnable(){
    }

    public void run() {
        while(index <= NUMBER_MAX){
            System.out.println(Thread.currentThread() + "的号码是：" + index++);
        }
    }
}

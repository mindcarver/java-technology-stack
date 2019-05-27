package codecarver.chapter1.c4.case1;

public class Client {
    public static void main(String[] args) {
        final SynchronizedRunableDemo ticketWindow = new SynchronizedRunableDemo();

        Thread windowThread1 = new Thread(ticketWindow, "一号窗口");
        Thread windowThread2 = new Thread(ticketWindow, "二号窗口");
        Thread windowThread3 = new Thread(ticketWindow, "三号窗口");
        windowThread1.start();
        windowThread2.start();
        windowThread3.start();
    }
}

package codecarver.chapter6.case8;

public class Client {
    public static void main(String[] args) {
        AddTest addTest = new AddTest();
        Thread a = new MyThreadA(addTest);
        Thread b = new MyThreadB(addTest);
        a.start();
        b.start();
    }
}

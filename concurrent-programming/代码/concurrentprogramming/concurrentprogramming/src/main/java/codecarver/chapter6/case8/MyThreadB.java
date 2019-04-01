package codecarver.chapter6.case8;

public class MyThreadB extends Thread {
    private AddTest addTest;
    public MyThreadB(AddTest addTest){
        this.addTest = addTest;
    }
    @Override
    public void run() {
        System.out.println("B线程开始调用");
        addTest.add();
        System.out.println("B结束调用");
    }
}

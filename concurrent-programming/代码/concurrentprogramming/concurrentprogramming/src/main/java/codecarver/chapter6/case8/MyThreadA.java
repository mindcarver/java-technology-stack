package codecarver.chapter6.case8;

public class MyThreadA extends Thread {
    private AddTest addTest ;
    public MyThreadA(AddTest addTest){
        this.addTest = addTest;
    }
    @Override
    public void run() {
        System.out.println("A线程开始调用");
        addTest.add();
        System.out.println("A结束调用");
    }
}

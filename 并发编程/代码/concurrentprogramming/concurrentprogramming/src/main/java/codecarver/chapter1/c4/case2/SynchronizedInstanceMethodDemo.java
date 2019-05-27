package codecarver.chapter1.c4.case2;

public class SynchronizedInstanceMethodDemo implements Runnable {

    private int index = 1;

    private final static int MAX = 200;

    @Override
    public void run() {
       while (true){
            if(!callNumber()){
                break;
            }
       }

    }

    private synchronized boolean callNumber(){
        if(index > MAX){
            return false;
        }
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(Thread.currentThread() + " 的号码是:" + (index++));
        return  true;
    }
}



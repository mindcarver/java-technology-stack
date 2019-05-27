package codecarver.chapter1.c5;

/**
 * 即使你设置了优先级，线程调度也并不能保证高优先级的总是在低优先级前面完成
 */
public class Priority {
    public static class HighPrioprity extends Thread{
        static  int count = 0;
        public void run(){
            while(true){
                synchronized (Priority.class){
                    count++;
                    if(count>100000){
                        System.out.println("High Prioprity is complete");
                        break;
                    }
                }
            }
        }
    }

    public static class LowPrioprity extends Thread{
        static  int count = 0;
        public void run(){
            while(true){
                synchronized (Priority.class){
                    count++;
                    if(count>100000){
                        System.out.println("low Prioprity is complete");
                        break;
                    }
                }
            }
        }
    }

    public static  void  main(String[] args){
        Thread high = new HighPrioprity();
        Thread low = new LowPrioprity();
        high.setPriority(Thread.MAX_PRIORITY);
        low.setPriority(Thread.MIN_PRIORITY);
        low.start();
        high.start();
    }


}

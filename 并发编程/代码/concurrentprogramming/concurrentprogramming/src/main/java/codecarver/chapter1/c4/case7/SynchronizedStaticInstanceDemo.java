package codecarver.chapter1.c4.case7;

public class SynchronizedStaticInstanceDemo  {

    public static synchronized void addM1(){
        for(int i=1 ;i<5;i++){
            System.out.println(Thread.currentThread().getName()+":"+i);
        }
        try {
            // 假设要执行5秒
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
   /* public static synchronized void addM2(){
        for(int i=6 ;i<10;i++){
            System.out.println(Thread.currentThread().getName()+":"+i);
        }
        try {
            // 假设要执行5秒
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }*/

    public static  void addM2(){
        synchronized (SynchronizedStaticInstanceDemo.class){
            for(int i=6 ;i<10;i++){
                System.out.println(Thread.currentThread().getName()+":"+i);
            }
            try {
                // 假设要执行5秒
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}



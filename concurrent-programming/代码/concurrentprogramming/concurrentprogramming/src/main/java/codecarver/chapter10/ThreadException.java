package codecarver.chapter10;

import java.util.Arrays;
import java.util.List;

public class ThreadException {
    public static void main(String[] args) {
        Thread t = new Thread(){
            @Override
            public void run() {
                int i = 1 / 0;
                List list =    Arrays.asList(Thread.getAllStackTraces());
                // 这边打印的时候去搜一下THread。getAll...方法
                System.out.println(i);
            }
        };
        t.start();
        // 这一步用捕获线程内运行时异常
        t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                System.out.println(e);
                System.out.println(t);
            }
        });
    }
}

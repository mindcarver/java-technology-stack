package codecarver.chapter19.concurrentHashMap;

import java.util.HashMap;
import java.util.UUID;

/**
 * @ClassName HashMapExcuteInConccrrencyEnvironment
 * @Description TODO
 * @Author lenovo
 * @Date 2019/1/30 21:38
 **/
public class HashMapExcuteInConccrrencyEnvironment {
    final static HashMap<String, String> map = new HashMap<String, String>(2);

    public static void main(String[] args) throws InterruptedException {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100000; i++) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            map.put(UUID.randomUUID().toString(), "");
                        }
                    }, "moon" + i).start();
                }
            }
        }, "gxx");
        t.start();
        t.join();
        System.out.println("子线程执行完成");
    }
}

package codecarver.chapter4.blockingQueue.arrayBlockingQueue;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * @ClassName ArrayBlockingQueue
 * @Description TODO
 * @Author lenovo
 * @Date 2019/2/27 19:24
 **/
public class ArrayBlockingQueueDemo2 {
    public static void main(String[] args)  {
        ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<>(3);

        try {
            queue.put(1);
            queue.put(2);
            queue.put(3);
           // queue.put(4);  // 如果不注释，将会一直阻塞当前线程（main），除非有容量空出。
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("queue contains " + queue);
    }
}

package codecarver.chapter4.blockingQueue.arrayBlockingQueue;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * @ClassName ArrayBlockingQueue
 * @Description TODO
 * @Author lenovo
 * @Date 2019/2/27 19:24
 **/
public class ArrayBlockingQueueDemo4 {
    public static void main(String[] args) throws InterruptedException {
        ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue(3);
        queue.add(1);
        queue.add(2);
        queue.add(3);
        System.out.println("Before drainTo Operation");
        System.out.println("queue = " + queue);

        ArrayList<Integer> list = new ArrayList();

        queue.drainTo(list);
        System.out.println("After drainTo Operation");
        System.out.println("queue = " + queue);
        System.out.println("collection = " + list);
    }
}

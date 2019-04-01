package codecarver.chapter20.blockingQueue.arrayBlockingQueue;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * @ClassName ArrayBlockingQueue
 * @Description ADD method
 * @Author lenovo
 * @Date 2019/2/27 19:24
 **/
public class ArrayBlockingQueueDemo1 {
    public static void main(String[] args) {
        // define capacity of ArrayBlockingQueue
        int capacity = 3;

        // create object of ArrayBlockingQueue
        ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<Integer>(capacity);

        // Add element to ArrayBlockingQueue
        queue.add(1);

        System.out.println("After adding 1");
        System.out.println(queue);

        queue.add(2);
        queue.add(3);
        queue.add(4);

        System.out.println("After adding four element");
        System.out.println(queue);
    }
}

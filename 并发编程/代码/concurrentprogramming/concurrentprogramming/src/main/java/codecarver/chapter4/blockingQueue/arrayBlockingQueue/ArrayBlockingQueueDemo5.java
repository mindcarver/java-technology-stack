package codecarver.chapter4.blockingQueue.arrayBlockingQueue;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * @ClassName ArrayBlockingQueue
 * @Description TODO
 * @Author lenovo
 * @Date 2019/2/27 19:24
 **/
public class ArrayBlockingQueueDemo5 {
    public static void main(String[] args) throws InterruptedException {
        ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<Integer>(5);
        queue.offer(1);
        queue.offer(2);
        queue.offer(3);
        System.out.println("Queue Contains" + queue);
        System.out.println("Removing From head: " + queue.poll());
        System.out.println("Queue Contains" + queue);
        System.out.println("Removing From head: " + queue.poll());
        System.out.println("Queue Contains" + queue);
        System.out.println("Removing From head: " + queue.poll());
        System.out.println("Queue Contains" + queue);
        System.out.println("Removing From head: " + queue.poll());
        System.out.println("Queue Contains" + queue);
    }
}

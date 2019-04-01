package codecarver.chapter20.blockingQueue.arrayBlockingQueue;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * @ClassName ArrayBlockingQueue
 * @Description TODO
 * @Author lenovo
 * @Date 2019/2/27 19:24
 **/
public class ArrayBlockingQueueDemo3 {
    public static void main(String[] args) throws InterruptedException {
        ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<>(3);
        queue.add(1);
        queue.add(2);
        queue.add(3);
        System.out.println("After addding numbers Queue: " +queue);

        int head=queue.take();
        System.out.println("Head of queue removed is " +head);
        System.out.print("After removing head,Queue: ");
        System.out.println(queue);

        head = queue.take();

        System.out.println("Head of queue removed is " + head);
        System.out.print("After removing head Queue: ");
        System.out.println(queue);
    }
}

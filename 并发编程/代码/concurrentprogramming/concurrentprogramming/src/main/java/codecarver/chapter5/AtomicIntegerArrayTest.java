package codecarver.chapter5;

import java.util.concurrent.atomic.AtomicIntegerArray;

public class AtomicIntegerArrayTest {
    private static int[] value = new int[]{1,2,3};
    private static AtomicIntegerArray atomicInteger = new AtomicIntegerArray(value);

    public static void main(String[] args){
        atomicInteger.getAndSet(0,12);
        System.out.println(atomicInteger.get(0));
        System.out.println(value[0]);
    }

}

package codecarver.chapter5;

import java.util.concurrent.atomic.AtomicInteger;

public class AtomicIntegerTest {
    private static AtomicInteger atomicInteger = new AtomicInteger(1);

    public static void main(String[] args){
        System.out.println(atomicInteger.getAndIncrement());
        System.out.println(atomicInteger.get());
        System.out.println(atomicInteger.getAndSet(5));
        System.out.println(atomicInteger.get());
    }
}

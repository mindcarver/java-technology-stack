package codecarver.chapter14;

// 这边到时候做笔记就是要结合volatile添加或者不添加做演示

public class VolatileDemo {
    private static volatile int INIT_VALUE = 0;

    private final static int MAX_LIMIT = 500;

    // 如果你不加volatile ，会导致一直更新，不会提示下面这句话
    // 因为java做了优化，发现这边都是读，所以就好心的一直从缓存中读取。
    // 但如果你的程序中有写的操作，那么就会刷新回主内存
    public static void main(String[] args) {
        new Thread(() -> {
            int localValue = INIT_VALUE;
            while (localValue < MAX_LIMIT) {
                if (localValue != INIT_VALUE) {
                    System.out.printf("The value updated to [%d]\n", INIT_VALUE);
                    localValue = INIT_VALUE;
                }
            }
        }, "READER").start();

        new Thread(() -> {
            int localValue = INIT_VALUE;
            while (INIT_VALUE < MAX_LIMIT) {
                System.out.printf("Update the value to [%d]\n", ++localValue);
                INIT_VALUE = localValue;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "UPDATER").start();
    }
}

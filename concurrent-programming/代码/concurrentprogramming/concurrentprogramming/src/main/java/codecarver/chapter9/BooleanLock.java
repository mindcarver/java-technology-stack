package codecarver.chapter9;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class BooleanLock implements Lock {

    //The initValue is true indicated the lock have be get.
    //The initValue is false indicated the lock is free (other thread can get this.)
    private boolean initValue;

    private Collection<Thread> blockedThreadCollection = new ArrayList<>();

    private Thread currentThread;

    public BooleanLock() {
        this.initValue = false;
    }

    @Override
    public synchronized void lock() throws InterruptedException {
        while (initValue) {
            blockedThreadCollection.add(Thread.currentThread());
            this.wait();
        }

        blockedThreadCollection.remove(Thread.currentThread());
        this.initValue = true;
        this.currentThread = Thread.currentThread();
    }

    @Override
    public synchronized void lock(long mills) throws InterruptedException, TimeOutException {
        // 你玩赖我也玩赖，你传入时间为0 ，我就让你进入lock方法
        if (mills <= 0)
            lock();

        // 传入之间
        long hasRemaining = mills;
        // 结束时间
        long endTime = System.currentTimeMillis() + mills;
        while (initValue) {

            if (hasRemaining <= 0)
                throw new TimeOutException("Time out");
            // 表示锁被占用，那么把我扔到等待线程中
            blockedThreadCollection.add(Thread.currentThread());
            this.wait(mills);
            hasRemaining = endTime - System.currentTimeMillis();
        }

        this.initValue = true;
        this.currentThread = Thread.currentThread();

    }

    @Override
    public synchronized void unlock() {
        if (Thread.currentThread() == currentThread) {
            this.initValue = false;
            Optional.of(Thread.currentThread().getName() + " release the lock monitor.")
                    .ifPresent(System.out::println);
            this.notifyAll();
        }
    }

    @Override
    public Collection<Thread> getBlockedThread() {
        return Collections.unmodifiableCollection(blockedThreadCollection);
    }

    @Override
    public int getBlockedSize() {
        return blockedThreadCollection.size();
    }
}

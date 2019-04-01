package codecarver.chapter9;

import java.util.Collection;

public interface Lock {

    class TimeOutException extends Exception {

        public TimeOutException(String message) {
            super(message);
        }
    }

    // 让锁可以打断，synchronized是不可以打断的
    void lock() throws InterruptedException;

    void lock(long mills) throws InterruptedException, TimeOutException;

    void unlock();

    Collection<Thread> getBlockedThread();

    int getBlockedSize();

}
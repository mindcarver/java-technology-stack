package codecarver.chapter5;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class AtomicIntegerFieldUpdaterTest {
    //创建原子更新器
    private static AtomicIntegerFieldUpdater<User> updater = AtomicIntegerFieldUpdater.newUpdater(User.class,"age");

    public static void main(String[] args){
        User user = new User("codecarver",21);
        //codecarver长了一岁
        updater.getAndIncrement(user);
        System.out.println(updater.get(user));
    }

    static class User{

        private String name;
        public volatile int age;

        public User(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }
    }
}

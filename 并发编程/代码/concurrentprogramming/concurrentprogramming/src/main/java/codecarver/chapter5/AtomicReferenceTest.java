package codecarver.chapter5;

import java.util.concurrent.atomic.AtomicReference;

public class AtomicReferenceTest {
    private static AtomicReference<User> reference = new AtomicReference<User>();

    public static void main(String[] args){
        User user = new User("Jony",23);
        reference.set(user);
        User updateUser = new User("codecarver",34);
        reference.compareAndSet(user,updateUser);
        System.out.println(reference.get().getName());
        System.out.println(reference.get().getAge());
    }


    static class User{

        private String name;
        private int age;

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

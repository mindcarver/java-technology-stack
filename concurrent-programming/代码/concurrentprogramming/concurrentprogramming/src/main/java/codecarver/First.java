package codecarver;

public class First{
    public static void main(String[] args) {
        /*Thread t = new Thread("foreach-print-i"){
            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    System.out.println("2->" + i);
                }
            }
        };
        t.start();
        for (int i = 0; i < 10; i++) {
            System.out.println("1->" + i);
        }*/
        new Thread("foreach-print-i"){
            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    System.out.println(i);
                }
            }
        }.start();
    }
}

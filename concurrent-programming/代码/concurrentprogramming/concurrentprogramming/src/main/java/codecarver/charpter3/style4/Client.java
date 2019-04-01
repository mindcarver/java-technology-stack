package codecarver.charpter3.style4;

public class Client {

    public static void main(String[] args) {
        ThreadService service = new ThreadService();
        long start = System.currentTimeMillis();
        service.execute(() -> {
            //
            while (true) {
                // 模拟一个很耗时的资源，理论只给他10秒，就会shutdown
            }
           /* try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
        });
        service.shutdown(10000);
        long end = System.currentTimeMillis();
        System.out.println(end - start);
    }
}

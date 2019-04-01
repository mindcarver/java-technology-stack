package codecarver.chapter6.case8;

public class AddTest {

    public void add(){
        /*此变量如果放到方法外面
        * 那么就会出现线程不安全问题
        * */
        int num = 0;
        for (int i = 0; i < 5; i++) {
            ++num;
        }
        System.out.println("num:" + num);
    }
}

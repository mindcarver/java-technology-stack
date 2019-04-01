package codecarver.charpter2.bankcall.case1;

public class TicketWindow extends Thread {
    // 窗体名称
    private String name;

    // 最大取号数
    private final int NUMBER_MAX = 50;

    // 计数索引
    private static   int index = 1;

    public TicketWindow(String name){
        this.name = name;
    }

    @Override
    public void run() {
        while(index <= NUMBER_MAX){
            System.out.println("柜台：" + name + "，当前的号码是：" + index++);
        }
    }
}

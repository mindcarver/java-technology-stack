package codecarver.charpter2.bankcall.case1;


public class Client {
    public static void main(String[] args) {
        // 很明显我们会打印出各种号码，各玩各的，不能控制index
        TicketWindow ticketWindow1 = new TicketWindow("一号柜台");
        ticketWindow1.start();
        TicketWindow ticketWindow2 = new TicketWindow("二号柜台");
        ticketWindow2.start();
        TicketWindow ticketWindow3 = new TicketWindow("三号柜台");
        ticketWindow3.start();
    }
}

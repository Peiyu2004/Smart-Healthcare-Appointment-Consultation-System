package appointment.notification;

public class ConsoleNotifier implements Notifiable {
    @Override
    public void send(Notification notification) {
        System.out.println(notification.toString());
    }
}
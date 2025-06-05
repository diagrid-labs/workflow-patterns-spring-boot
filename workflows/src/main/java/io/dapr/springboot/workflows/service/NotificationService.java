package io.dapr.springboot.workflows.service;

import io.dapr.springboot.workflows.model.Notification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class NotificationService {
  private List<Notification> notifications = new ArrayList<>();

  public void sendNotification(Notification notification){
    notifications.add(notification);
    System.out.println(">> Notification Sent: " + notification);
  }

  public List<Notification> getNotifications(){
    return notifications;
  }

  public void resetNotifications(){
    notifications.clear();
  }

}

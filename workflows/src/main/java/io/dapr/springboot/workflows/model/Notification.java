package io.dapr.springboot.workflows.model;

import java.util.UUID;

public class Notification {
  private String id;
  private PaymentRequest  paymentRequest;
  private String message;

  public Notification() {
    this.id = UUID.randomUUID().toString();
  }

  public Notification(PaymentRequest paymentRequest, String message) {
    this();
    this.paymentRequest = paymentRequest;
    this.message = message;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public PaymentRequest getPaymentRequest() {
    return paymentRequest;
  }

  public void setPaymentRequest(PaymentRequest paymentRequest) {
    this.paymentRequest = paymentRequest;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @Override
  public String toString() {
    return "Notification{" +
            "id='" + id + '\'' +
            ", paymentRequest=" + paymentRequest +
            ", message='" + message + '\'' +
            '}';
  }
}

package io.dapr.springboot.payments.model;

public class AuditPaymentPayload {
  private String paymentRequestId;
  private String customer;
  private Integer amount;
  private String message;

  public AuditPaymentPayload(String paymentRequestId, String customer, Integer amount, String message) {
    this.paymentRequestId = paymentRequestId;
    this.customer = customer;
    this.amount = amount;
    this.message = message;
  }

  public AuditPaymentPayload() {
  }

  public String getPaymentRequestId() {
    return paymentRequestId;
  }

  public void setPaymentRequestId(String paymentRequestId) {
    this.paymentRequestId = paymentRequestId;
  }

  public String getCustomer() {
    return customer;
  }

  public void setCustomer(String customer) {
    this.customer = customer;
  }

  public Integer getAmount() {
    return amount;
  }

  public void setAmount(Integer amount) {
    this.amount = amount;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @Override
  public String toString() {
    return "AuditPaymentPayload{" +
            "paymentRequestId='" + paymentRequestId + '\'' +
            ", customer='" + customer + '\'' +
            ", amount=" + amount +
            ", message='" + message + '\'' +
            '}';
  }
}

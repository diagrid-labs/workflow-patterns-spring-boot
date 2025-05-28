package io.dapr.springboot.workflows.model;

/*
 * PaymentPayload represent the data structure expected from an external system
 */
public class PaymentPayload {
  private String paymentRequestId;
  private String customer;
  private Integer amount;
  private String message;

  public PaymentPayload(String paymentRequestId, String customer, Integer amount, String message) {
    this.paymentRequestId = paymentRequestId;
    this.customer = customer;
    this.amount = amount;
    this.message = message;
  }

  public PaymentPayload() {
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

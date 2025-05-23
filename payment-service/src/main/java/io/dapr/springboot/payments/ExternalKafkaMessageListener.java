package io.dapr.springboot.payments;

import io.dapr.springboot.payments.model.PaymentRequest;
import io.dapr.workflows.client.DaprWorkflowClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ExternalKafkaMessageListener {

  private final Logger logger = LoggerFactory.getLogger(PaymentsServiceRestController.class);

  @Autowired
  private DaprWorkflowClient daprWorkflowClient;

  @Autowired
  private PaymentsServiceRestController paymentsServiceRestController;

  /**
   *  Wait for async kafka message
   *  @param paymentRequest associated with a workflow instance
   *  @return confirmation that the follow-up was requested
   */
  @KafkaListener(topics = "${REMOTE_KAFKA_TOPIC}", groupId = "payments-service")
  public String orderApproval(PaymentRequest paymentRequest) {
    logger.info("Payment request approval requested: " + paymentRequest.getId());
    String workflowIdForCustomer = paymentsServiceRestController.getPaymentsWorkflows().get(paymentRequest.getId());
    if (workflowIdForCustomer == null || workflowIdForCustomer.isEmpty()) {
      return "There is no workflow associated with customer: " + paymentRequest.getId();
    } else {
      daprWorkflowClient.raiseEvent(workflowIdForCustomer, "ExternalProcessingDone", paymentRequest);
      return "Payment Approval requested";
    }
  }
}

package io.dapr.springboot.workflows.asynckafka;

import io.dapr.springboot.workflows.model.PaymentRequest;
import io.dapr.springboot.workflows.service.PaymentRequestsStore;
import io.dapr.springboot.workflows.service.PaymentWorkflowsStore;
import io.dapr.workflows.client.DaprWorkflowClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ExternalKafkaMessageListener {

  private final Logger logger = LoggerFactory.getLogger(ExternalKafkaMessageListener.class);

  @Autowired
  private DaprWorkflowClient daprWorkflowClient;

  @Autowired
  private PaymentWorkflowsStore paymentWorkflowsStore;

  @Autowired
  private PaymentRequestsStore ordersStore;

  /**
   *  Wait for async kafka message
   *  @param paymentRequest associated with a workflow instance
   *  @return confirmation that the follow-up was requested
   */
  @KafkaListener(topics = "${REMOTE_KAFKA_TOPIC:topic}", groupId = "workflows")
  public String paymentRequestApproval(PaymentRequest paymentRequest) {
    logger.info("Payment request approval requested: " + paymentRequest.getId());
    String workflowIdForPayment = paymentWorkflowsStore.getPaymentWorkflowInstanceId(paymentRequest.getId());
    if (workflowIdForPayment == null || workflowIdForPayment.isEmpty()) {
      return "There is no workflow associated with the paymentId: " + paymentRequest.getId();
    } else {
      paymentRequest.setProcessedByExternalAsyncSystem(true);
      ordersStore.savePaymentRequest(paymentRequest);
      daprWorkflowClient.raiseEvent(workflowIdForPayment, "ExternalProcessingDone", paymentRequest);
      return "Payment processed by async service";
    }
  }
}

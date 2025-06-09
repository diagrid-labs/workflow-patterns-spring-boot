package io.dapr.springboot.workflows.suspendresume;

import io.dapr.springboot.workflows.model.PaymentRequest;
import io.dapr.springboot.workflows.service.PaymentRequestsStore;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LogPaymentActivity implements WorkflowActivity {

  private final Logger logger = LoggerFactory.getLogger(LogPaymentActivity.class);

  @Autowired
  private PaymentRequestsStore paymentRequestsStore;

  @Override
  public Object run(WorkflowActivityContext ctx) {
    PaymentRequest paymentRequest = ctx.getInput(PaymentRequest.class);
    logger.info("Log payment: {}", paymentRequest);

    paymentRequestsStore.savePaymentRequest(paymentRequest);

    return paymentRequest;
  }
} 
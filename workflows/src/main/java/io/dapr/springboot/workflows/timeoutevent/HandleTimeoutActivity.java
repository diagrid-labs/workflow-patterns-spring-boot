package io.dapr.springboot.workflows.timeoutevent;

import io.dapr.springboot.workflows.model.PaymentRequest;
import io.dapr.springboot.workflows.service.PaymentRequestsStore;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class HandleTimeoutActivity implements WorkflowActivity {
    
    private final Logger logger = LoggerFactory.getLogger(HandleTimeoutActivity.class);
    
    @Autowired
    private PaymentRequestsStore paymentRequestsStore;

    @Override
    public Object run(WorkflowActivityContext ctx) {
        PaymentRequest paymentRequest = ctx.getInput(PaymentRequest.class);
        logger.info("Handling timeout for payment: " + paymentRequest.getId());
        
        paymentRequest.setRecoveredFromTimeout(true);
        paymentRequestsStore.savePaymentRequest(paymentRequest);
        
        return paymentRequest;
    }
} 
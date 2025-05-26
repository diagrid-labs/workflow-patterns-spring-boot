/*
 * Copyright 2025 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.springboot.payments.workflow;


import io.dapr.springboot.payments.model.AuditPaymentPayload;
import io.dapr.springboot.payments.model.PaymentRequest;
import io.dapr.springboot.payments.service.PaymentRequestsStore;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/*
 This activity shows how to interact with an external system that will store the Payment request,
 for example for auditing purposes.
 */
@Component
public class StorePaymentRequestActivity implements WorkflowActivity {

  private final Logger logger = LoggerFactory.getLogger(StorePaymentRequestActivity.class);
  private final PaymentRequestsStore paymentRequestsStore;

  public StorePaymentRequestActivity(PaymentRequestsStore ordersStore) {
    this.paymentRequestsStore = ordersStore;
  }


  @Autowired
  private RestTemplate restTemplate;

  @Value("${application.validation-base-url:}")
  private String validationBaseURL;

  @Override
  public Object run(WorkflowActivityContext ctx) {
    PaymentRequest paymentRequest = ctx.getInput(PaymentRequest.class);

    // Local registry of payment requests
    paymentRequestsStore.savePaymentRequest(paymentRequest);


    if(validationBaseURL != null && !validationBaseURL.isEmpty()) {
      // Define the shape of the request for the remote service
      HttpEntity<AuditPaymentPayload> request =
              new HttpEntity<>(new AuditPaymentPayload(paymentRequest.getId(),
                      paymentRequest.getCustomer(), paymentRequest.getAmount(), "Payment accepted for processing"));
      AuditPaymentPayload orderUpdateString =
              restTemplate.postForObject(validationBaseURL + "/validate", request, AuditPaymentPayload.class);
      logger.info("Update Order result: " + orderUpdateString);
    }
    logger.info("PaymentRequest: " + paymentRequest.getId() + " stored for auditing.");
    return paymentRequest;
  }


}

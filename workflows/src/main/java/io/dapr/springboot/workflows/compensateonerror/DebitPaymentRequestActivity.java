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

package io.dapr.springboot.workflows.compensateonerror;


import io.dapr.springboot.workflows.model.PaymentPayload;
import io.dapr.springboot.workflows.model.PaymentRequest;
import io.dapr.springboot.workflows.service.PaymentRequestsStore;
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
 This activity shows how to interact with an external HTTP endpoint from an activity.
 */
@Component
public class DebitPaymentRequestActivity implements WorkflowActivity {

  private final Logger logger = LoggerFactory.getLogger(DebitPaymentRequestActivity.class);
  private final PaymentRequestsStore paymentRequestsStore;

  @Autowired
  private RestTemplate restTemplate;

  @Autowired
  private CompensationHelper workflowAuditBean;

  @Value("${application.validation-base-url:}")
  private String validationBaseURL;

  @Autowired
  private AccountService accountService;

  public DebitPaymentRequestActivity(PaymentRequestsStore ordersStore) {
    this.paymentRequestsStore = ordersStore;
  }

  @Override
  public Object run(WorkflowActivityContext ctx) {

    PaymentRequest paymentRequest = ctx.getInput(PaymentRequest.class);

    if(paymentRequest.getCustomer().equals("salaboy")){
      throw new IllegalStateException("Invalid Customer!");
    }

    if(validationBaseURL != null && !validationBaseURL.isEmpty()) {
      // Define the shape of the request for the remote service
      HttpEntity<PaymentPayload> request =
              new HttpEntity<>(new PaymentPayload(paymentRequest.getId(),
                      paymentRequest.getCustomer(), paymentRequest.getAmount(), "Payment accepted for processing"));
      PaymentPayload paymentResult =
              restTemplate.postForObject(validationBaseURL + "/debit", request, PaymentPayload.class);
      logger.info("Debit Payment Result: " + paymentResult);
      paymentRequest.setProcessedByRemoteHttpService(true);
    }
    logger.info("Debit PaymentRequest: " + paymentRequest.getId() + " sent.");
    paymentRequestsStore.savePaymentRequest(paymentRequest);
    accountService.debit(paymentRequest.getCustomer(), paymentRequest.getAmount());
    return paymentRequest;
  }


}

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

package io.dapr.springboot.workflows.savestate;


import io.dapr.client.DaprClient;
import io.dapr.client.domain.State;
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
public class GetStateActivity implements WorkflowActivity {

  private final Logger logger = LoggerFactory.getLogger(GetStateActivity.class);
  private final PaymentRequestsStore paymentRequestsStore;

  @Autowired
  private DaprClient daprClient;


  public GetStateActivity(PaymentRequestsStore ordersStore) {
    this.paymentRequestsStore = ordersStore;
  }

  @Override
  public Object run(WorkflowActivityContext ctx) {
    PaymentRequest paymentRequest = ctx.getInput(PaymentRequest.class);

    State<PaymentRequest> paymentRequestState = daprClient.getState("kvstore", paymentRequest.getId(), PaymentRequest.class).block();

    assert paymentRequestState != null;

    logger.info("PaymentRequest: {} retrieved from State Store {}.", paymentRequestState.getValue(), "kvstore");

    return paymentRequest;
  }


}

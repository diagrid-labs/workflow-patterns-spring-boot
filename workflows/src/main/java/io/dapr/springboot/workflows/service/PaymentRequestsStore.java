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

package io.dapr.springboot.workflows.service;

import io.dapr.springboot.workflows.model.PaymentRequest;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PaymentRequestsStore {
  private final Map<String, PaymentRequest> paymentRequests = new ConcurrentHashMap<>();

  private final Map<String, List<Date>> paymentTimes = new ConcurrentHashMap<>();

  private final List<PaymentRequest> log = new ArrayList<>();

  public void savePaymentRequest(PaymentRequest paymentRequest) {
    paymentRequests.put(paymentRequest.getId(), paymentRequest);
  }

  public void logPayment(PaymentRequest paymentRequest){
    log.add(paymentRequest);
  }

  public List<PaymentRequest> getLoggedPayments(){
    return log;
  }

  public void logPaymentTime(PaymentRequest paymentRequest){
    paymentTimes.computeIfAbsent(paymentRequest.getId(), k -> new ArrayList<>());
    paymentTimes.get(paymentRequest.getId()).add(new Date());
  }

  public List<Date> getPaymentTimes(String paymentId){
    return paymentTimes.get(paymentId);
  }

  public PaymentRequest getPaymentRequest(String requestId) {
    return paymentRequests.get(requestId);
  }

  public Collection<PaymentRequest> getPaymentRequests() {
    return paymentRequests.values();
  }

  public void clearPaymentTimes(){
    paymentTimes.clear();
  }

}

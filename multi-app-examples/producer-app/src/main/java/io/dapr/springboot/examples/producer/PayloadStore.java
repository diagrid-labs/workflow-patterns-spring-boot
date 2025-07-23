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

package io.dapr.springboot.examples.producer;

import io.dapr.springboot.examples.Payload;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component
public class PayloadStore {
  private Map<String, Payload> payloads = new HashMap<>();

  public void addPayload(Payload payload) {
    payloads.put(payload.getId(), payload);
  }

  public Payload getPayloadById(String payloadId) {
    return payloads.get(payloadId);
  }

  public Collection<Payload> getPayloads() {
    return payloads.values();
  }

}

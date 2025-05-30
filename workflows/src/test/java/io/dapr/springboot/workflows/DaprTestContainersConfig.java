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

package io.dapr.springboot.workflows;

import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import io.dapr.testcontainers.Subscription;
import io.github.microcks.testcontainers.MicrocksContainersEnsemble;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TestConfiguration(proxyBeanMethods = false)
public class DaprTestContainersConfig {

  @Bean
  public Network getDaprNetwork(Environment env) {
    boolean reuse = env.getProperty("reuse", Boolean.class, false);
    if (reuse) {
      Network defaultDaprNetwork = new Network() {
        @Override
        public String getId() {
          return "dapr-network";
        }

        @Override
        public void close() {

        }

        @Override
        public Statement apply(Statement base, Description description) {
          return null;
        }
      };

      List<com.github.dockerjava.api.model.Network> networks = DockerClientFactory.instance().client().listNetworksCmd()
          .withNameFilter("dapr-network").exec();
      if (networks.isEmpty()) {
        Network.builder().createNetworkCmdModifier(cmd -> cmd.withName("dapr-network")).build().getId();
        return defaultDaprNetwork;
      } else {
        return defaultDaprNetwork;
      }
    } else {
      return Network.newNetwork();
    }
  }

  @Bean
  @ServiceConnection
  KafkaContainer kafkaContainer(Network network) {
    KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))
            .withNetwork(network)
            .withNetworkAliases("kafka")
            .withListener(() -> "kafka:19092");
    return kafkaContainer;
  }


  @Bean
  MicrocksContainersEnsemble microcksEnsemble(Network network) {
    MicrocksContainersEnsemble ensemble = new MicrocksContainersEnsemble(network, "quay.io/microcks/microcks-uber:1.11.2")
            .withAccessToHost(true)   // We need this to access our webapp while it runs
            .withMainArtifacts("third-parties/remote-http-service.yaml");
    return ensemble;
  }

  @Bean
  public DynamicPropertyRegistrar endpointsProperties(MicrocksContainersEnsemble ensemble) {
    // We need to replace the default endpoints with those provided by Microcks.
    return (properties) -> {
      properties.add("application.validation-base-url", () -> ensemble.getMicrocksContainer()
              .getRestMockEndpoint("API Payment Validator", "1.0.0"));
    };
  }


  @Bean
  @ServiceConnection
  @ConditionalOnProperty(prefix = "tests", name = "dapr.local", havingValue = "true")
  public DaprContainer daprContainer(Network daprNetwork, KafkaContainer kafkaContainer) {

    Map<String, String> kafkaProperties = new HashMap<>();
    kafkaProperties.put("brokers", "kafka:19092");
    kafkaProperties.put("authType", "none");

    return new DaprContainer("daprio/daprd:1.15.4")
            .withAppName("workflows")
            .withNetwork(daprNetwork)
            .withComponent(new Component("kvstore", "state.in-memory", "v1",
                    Collections.singletonMap("actorStateStore", "true")))
            .withComponent(new Component("pubsub", "pubsub.kafka", "v1", kafkaProperties))
            .withSubscription(new Subscription("app", "pubsub", "pubsubTopic", "/asyncpubsub/continue"))
//  Uncomment if you want to troubleshoot Dapr related problems
//            .withDaprLogLevel(DaprLogLevel.DEBUG)
//            .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()))
            .withAppPort(8080)
            .withAppHealthCheckPath("/actuator/health")
            .withAppChannelAddress("host.testcontainers.internal")
            .dependsOn(kafkaContainer);
  }


}

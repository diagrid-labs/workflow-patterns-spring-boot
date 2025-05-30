package io.dapr.springboot.workflows.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import io.dapr.springboot.workflows.service.PaymentRequestsStore;
import io.dapr.springboot.workflows.simplehttp.SimpleHttpRestController;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import io.dapr.springboot.workflows.asynckafka.AsyncKafkaRestController;
import io.dapr.springboot.workflows.model.PaymentRequest;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Route("")
public class WorkflowsUI extends VerticalLayout {

    @Autowired
    private AsyncKafkaRestController asyncKafkaRestController;

    @Autowired
    private SimpleHttpRestController simpleHttpRestController;

    @Autowired
    private PaymentRequestsStore paymentRequestsStore;


    public WorkflowsUI() {
        var ui = UI.getCurrent();

        VerticalLayout verticalLayout = new VerticalLayout();

        Button simpleHttp = new Button("Start Simple HTTP Workflow");

        Button asyncKafka = new Button("Start Async Kafka Workflow");

        verticalLayout.add(simpleHttp, asyncKafka);

        simpleHttp.addClickListener(e -> {
            var paymentRequest = simpleHttpRestController.placePaymentRequest(new PaymentRequest("123", "salaboy", 10));
            Notification.show("Payment request placed with id: " + paymentRequest.getWorkflowInstanceId());
            HorizontalLayout row = new HorizontalLayout();
            row.setPadding(true);
            verticalLayout.add(row);
            Card simpleHttpCard = new Card();
            row.add(simpleHttpCard);
            MessageList simpleHttpMessagesList = new MessageList();
            simpleHttpCard.add(simpleHttpMessagesList);
            MessageListItem workflowIdMessage = new MessageListItem();
            workflowIdMessage.setText("Workflow ID: " + paymentRequest.getWorkflowInstanceId());


            MessageListItem workflowStatusMessage = new MessageListItem();
            workflowStatusMessage.setText("Customer: " + paymentRequest.getCustomer());


            MessageListItem processedByRemoteHttpServiceMessage = new MessageListItem();
            processedByRemoteHttpServiceMessage.setText("HTTP Call done?: " + paymentRequest.getProcessedByRemoteHttpService());
            simpleHttpMessagesList.setItems(Arrays.asList(workflowIdMessage, workflowStatusMessage, processedByRemoteHttpServiceMessage));

            Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(()-> {
                ui.access(() -> {
                    PaymentRequest latestPaymentRequest = paymentRequestsStore.getPaymentRequest(paymentRequest.getId());
                    if(latestPaymentRequest != null && latestPaymentRequest.getProcessedByRemoteHttpService()){
                        processedByRemoteHttpServiceMessage.setText("HTTP Call done?: " + latestPaymentRequest.getProcessedByRemoteHttpService());
                    }
                });
            }, 1000, 2000, TimeUnit.MILLISECONDS);

        });
        add(verticalLayout);

    }
}

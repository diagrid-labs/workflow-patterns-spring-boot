package io.dapr.springboot.workflows.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import io.dapr.springboot.workflows.service.PaymentRequestsStore;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import io.dapr.springboot.workflows.asynckafka.AsyncKafkaRestController;
import io.dapr.springboot.workflows.model.PaymentRequest;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Route("")
public class WorkflowsUI extends VerticalLayout {

    @Autowired
    private AsyncKafkaRestController asyncKafkaRestController;

    @Autowired
    private PaymentRequestsStore paymentRequestsStore;


    public WorkflowsUI() {
        var ui = UI.getCurrent();

        VerticalLayout verticalLayout = new VerticalLayout();

        Button button = new Button("Click me");

        button.addClickListener(e -> {
            var paymentRequest = asyncKafkaRestController.placePaymentRequest(new PaymentRequest("123", "salaboy", 10));
            Notification.show("Payment request placed with id: " + paymentRequest.getWorkflowInstanceId());
            Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(()-> {
                ui.access(() -> {
                    HorizontalLayout row = new HorizontalLayout();
                    verticalLayout.add(row);
                    TextField textField = new TextField();
                    row.add(textField);
                    textField.setValue(paymentRequestsStore.getPaymentRequest(paymentRequest.getId()).getProcessedByExternalAsyncSystem().toString());
                });
            }, 1000, 2000, TimeUnit.MILLISECONDS);

        });
        add(button, verticalLayout);

    }
}

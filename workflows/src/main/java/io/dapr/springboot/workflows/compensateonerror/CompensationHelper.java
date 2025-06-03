package io.dapr.springboot.workflows.compensateonerror;

import io.dapr.durabletask.Task;
import org.springframework.stereotype.Component;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class CompensationHelper {

    private final Logger logger = LoggerFactory.getLogger(CompensationHelper.class);

    private final Map<String, Functions.Func<Task<?>>> compensationActivities = new LinkedHashMap<>();

    public void addCompensation(String activityName, Functions.Func<Task<?>> compensationFunction) {
      compensationActivities.put(activityName, compensationFunction);
    }

    public void compensate(){
        for (int i = compensationActivities.size() - 1; i >= 0; i--) {
            Functions.Func<Task<?>> f = (Functions.Func<Task<?>>)
                                compensationActivities.values().toArray()[i];
            Task<?> result = f.apply();
            result.await();
        }

    }
    
}

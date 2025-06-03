package io.dapr.springboot.workflows.compensateonerror;

import java.io.Serializable;

public final class Functions {
    private Functions(){

    }

    @FunctionalInterface
    public interface Func<R> extends Serializable {
      R apply();
    }

}

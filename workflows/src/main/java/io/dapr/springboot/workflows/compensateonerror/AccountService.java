package io.dapr.springboot.workflows.compensateonerror;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AccountService {
  private final Map<String, Integer> customersBalance = new HashMap<>();

  public void debit(String customer, Integer amount){
    Integer customerBalance = customersBalance.get(customer);
    if (customerBalance == null){
      customersBalance.put(customer, -amount);
    } else {
      customersBalance.put(customer, customersBalance.get(customer) - amount);
    }
  }

  public void credit(String customer, Integer amount){
    Integer customerBalance = customersBalance.get(customer);
    if (customerBalance == null){
      customersBalance.put(customer, amount);
    } else {
      customersBalance.put(customer, customersBalance.get(customer) + amount);
    }
  }

  public Integer getCustomerBalance(String customer){
    return customersBalance.get(customer);
  }

}

package com.oms.repository;

import com.oms.model.Customer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CustomerRepository {
    private final Map<String, Customer> customers = new ConcurrentHashMap<>();

    public Customer save(Customer customer) {
        customers.put(customer.getCustomerId(), customer);
        return customer;
    }

    public Optional<Customer> findById(String customerId) {
        return Optional.ofNullable(customers.get(customerId));
    }

    public List<Customer> findAll() {
        return new ArrayList<>(customers.values());
    }
}
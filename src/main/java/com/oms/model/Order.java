package com.oms.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Order {
    private String orderId;
    private String customerId;
    private List<Item> items;
    private OrderStatus status;
    private Instant createdOn;
    private Instant updatedOn;
    private BigDecimal totalAmount;
    private BigDecimal vatAmount;
    private String shippingAddress;

    public Order() {
        this.items = new ArrayList<>();
        this.createdOn = Instant.now();
        this.updatedOn = Instant.now();
        this.status = OrderStatus.CREATED;
    }

    public Order(String orderId, String customerId, List<Item> items, String shippingAddress) {
        this();
        this.orderId = orderId;
        this.customerId = customerId;
        this.items = new ArrayList<>(items);
        this.shippingAddress = shippingAddress;
    }

    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public List<Item> getItems() { return new ArrayList<>(items); }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedOn() { return createdOn; }
    public Instant getUpdatedOn() { return updatedOn; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getVatAmount() { return vatAmount; }
    public String getShippingAddress() { return shippingAddress; }

    public void setOrderId(String orderId) { this.orderId = orderId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public void setItems(List<Item> items) { this.items = new ArrayList<>(items); }
    public void setStatus(OrderStatus status) {
        this.status = status;
        this.updatedOn = Instant.now();
    }
    public void setCreatedOn(Instant createdOn) { this.createdOn = createdOn; }
    public void setUpdatedOn(Instant updatedOn) { this.updatedOn = updatedOn; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public void setVatAmount(BigDecimal vatAmount) { this.vatAmount = vatAmount; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
}
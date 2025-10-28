package com.oms.model;

import java.math.BigDecimal;

public class Item {
    private String productId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal linePrice;

    public Item() {}

    public Item(String productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public Item(String productId, Integer quantity, BigDecimal unitPrice) {
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.linePrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public String getProductId() { return productId; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getLinePrice() { return linePrice; }

    public void setProductId(String productId) { this.productId = productId; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public void setLinePrice(BigDecimal linePrice) { this.linePrice = linePrice; }
}
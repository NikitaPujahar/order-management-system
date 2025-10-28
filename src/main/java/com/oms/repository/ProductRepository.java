package com.oms.repository;

import com.oms.model.Product;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ProductRepository {
    private final Map<String, Product> products = new ConcurrentHashMap<>();

    public Product save(Product product) {
        products.put(product.getId(), product);
        return product;
    }

    public Optional<Product> findById(String productId) {
        return Optional.ofNullable(products.get(productId));
    }

    public List<Product> findAll() {
        return new ArrayList<>(products.values());
    }
}
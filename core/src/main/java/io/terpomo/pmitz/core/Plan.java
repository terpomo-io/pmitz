package io.terpomo.pmitz.core;

public class Plan {

    private Product product;

    private String planId;

    public Plan(Product product, String planId) {
        this.planId = planId;
        this.product = product;
    }
}

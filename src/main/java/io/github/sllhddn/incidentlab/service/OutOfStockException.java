package io.github.sllhddn.incidentlab.service;

public class OutOfStockException extends RuntimeException {

    public OutOfStockException(String sku, int requested) {
        super("Not enough stock for " + sku + " (requested " + requested + ")");
    }
}

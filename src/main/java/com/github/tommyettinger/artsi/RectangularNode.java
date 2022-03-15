package com.github.tommyettinger.artsi;

/**
 * A rectangular node
 * @param <T> the tyoe of the data in the node
 */
public class RectangularNode<T> extends Node2DImpl {
    /**
     * The data in the node
     */
    public T data;

    public RectangularNode(double minX, double minY, double maxX, double maxY, T data) {
        super(minX, minY, maxX, maxY);
        this.data = data;
    }

    /**
     *
     * @return the data in the node
     */
    public T get() {
        return data;
    }
}

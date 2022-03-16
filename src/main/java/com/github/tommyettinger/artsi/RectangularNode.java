package com.github.tommyettinger.artsi;

/**
 * A rectangular node.
 * @param <T> the type of the data in the node
 */
public class RectangularNode<T> extends Node2DImpl {
    /**
     * The data in the node
     */
    public T data;

    public RectangularNode(float minX, float minY, float maxX, float maxY, T data) {
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

package com.github.tommyettinger.artsi;

/**
 * A Rectangular node
 *
 * @param <T> the type of data in the leaf node
 */
public class PointNode<T> extends Node2D {

    /**
     * The data in the node
     */
    public T data;
    final double x, y;

    /**
     * Create a point data node
     *
     * @param x    the x component of the data
     * @param y    the y component of the data
     * @param data the data
     */
    public PointNode(double x, double y, T data) {
        super();
        this.x = x;
        this.y = y;
        this.data = data;
    }

    @Override
    public double getMinX() {
        return x;
    }

    @Override
    public double getMinY() {
        return y;
    }

    @Override
    public final double getMaxX() {
        return getMinX();
    }

    @Override
    public final double getMaxY() {
        return getMinY();
    }

    @Override
    public final double getMidX() {
        return getMinX();
    }

    @Override
    public final double getMidY() {
        return getMinY();
    }

    /**
     * @return the data in this point node
     */
    public T get() {
        return data;
    }
}

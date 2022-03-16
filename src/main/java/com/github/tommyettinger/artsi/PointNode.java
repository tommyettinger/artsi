package com.github.tommyettinger.artsi;

/**
 * A node that has data associated with one point.
 *
 * @param <T> the type of data in the leaf node
 */
public class PointNode<T> extends Node2D {

    /**
     * The data in the node
     */
    public T data;
    final float x, y;

    /**
     * Create a point data node
     *
     * @param x    the x component of the data
     * @param y    the y component of the data
     * @param data the data
     */
    public PointNode(float x, float y, T data) {
        super();
        this.x = x;
        this.y = y;
        this.data = data;
    }

    @Override
    public float getMinX() {
        return x;
    }

    @Override
    public float getMinY() {
        return y;
    }

    @Override
    public final float getMaxX() {
        return getMinX();
    }

    @Override
    public final float getMaxY() {
        return getMinY();
    }

    @Override
    public final float getMidX() {
        return getMinX();
    }

    @Override
    public final float getMidY() {
        return getMinY();
    }

    /**
     * @return the data in this point node
     */
    public T get() {
        return data;
    }
}

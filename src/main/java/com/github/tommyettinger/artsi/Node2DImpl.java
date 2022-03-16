package com.github.tommyettinger.artsi;

import com.github.tommyettinger.ds.ObjectList;

import java.util.List;

/**
 * An internal rectangular node
 */
class Node2DImpl extends Node2D {

    float minX = Float.MAX_VALUE;
    float minY = Float.MAX_VALUE;
    float maxX = -Float.MAX_VALUE;
    float maxY = -Float.MAX_VALUE;

    /**
     * Create a rectangular data node
     *
     * @param minX the minimum x component of the data
     * @param minY the maximum x component of the data
     * @param maxX the minimum y component of the data
     * @param maxY the maximum y component of the data
     */
    protected Node2DImpl(float minX, float minY, float maxX, float maxY) {
        super(true);
        set(minX, minY, maxX, maxY);
    }

    Node2DImpl(ObjectList<Node2D> children) {
        super(children);
    }

    Node2DImpl(Node2D... nodes) {
        super(nodes);
    }

    @Override
    protected final void set(float minX, float minY, float maxX, float maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    @Override
    public float getMinX() {
        return minX;
    }

    @Override
    public float getMinY() {
        return minY;
    }

    @Override
    public float getMaxX() {
        return maxX;
    }

    @Override
    public float getMaxY() {
        return maxY;
    }

    /**
     * Comparator of the minimum x component
     *
     * @param a left hand operand
     * @param b right hand operand
     * @return an int representing whether a is greater/lesser/equal to the b
     */
    static int compareMinX(Node2D a, Node2D b) {
        return Float.compare(a.getMinX(), b.getMinX());
    }

    /**
     * Comparator of the minimum y component
     *
     * @param a left hand operand
     * @param b right hand operand
     * @return an int representing whether a is greater/lesser/equal to the b
     */
    static int compareMinY(Node2D a, Node2D b) {
        return Float.compare(a.getMinY(), b.getMinY());
    }

    /**
     * Calculate the area of performing a union between two nodes
     *
     * @param a node a
     * @param b node b
     * @return the area of performing a union with nodes a and b
     */
    static float enlargedArea(Node2D a, Node2D b) {
        return enlargedArea(a.getMinX(), a.getMinY(), a.getMaxX(), a.getMaxY(), b.getMinX(), b.getMinY(), b.getMaxX(), b.getMaxY());
    }

    /**
     * Calculate the area of intersection between two nodes
     *
     * @param a node a
     * @param b node b
     * @return the area of intersecting a with b
     */
    static float intersectionArea(Node2D a, Node2D b) {
        return intersectionArea(a.getMinX(), a.getMinY(), a.getMaxX(), a.getMaxY(), b.getMinX(), b.getMinY(), b.getMaxX(), b.getMaxY());
    }

    static boolean contains(Node2D a, Node2D b) {
        return contains(a.getMinX(), a.getMinY(), a.getMaxX(), a.getMaxY(), b.getMinX(), b.getMinY(), b.getMaxX(), b.getMaxY());
    }

    static boolean contains(Node2D a,
                            float bMinX,
                            float bMinY,
                            float bMaxX,
                            float bMaxY) {
        return contains(a.getMinX(), a.getMinY(), a.getMaxX(), a.getMaxY(), bMinX, bMinY, bMaxX, bMaxY);
    }

    static float enlargedArea(
            float aMinX,
            float aMinY,
            float aMaxX,
            float aMaxY,
            float bMinX,
            float bMinY,
            float bMaxX,
            float bMaxY
    ) {
        return (Math.max(bMaxX, aMaxX) - Math.min(bMinX, aMinX)) *
                (Math.max(bMaxY, aMaxY) - Math.min(bMinY, aMinY));
    }

    static float intersectionArea(
            float aMinX,
            float aMinY,
            float aMaxX,
            float aMaxY,
            float bMinX,
            float bMinY,
            float bMaxX,
            float bMaxY
    ) {
        float minX = Math.max(aMinX, bMinX);
        float minY = Math.max(aMinY, bMinY);
        float maxX = Math.min(aMaxX, bMaxX);
        float maxY = Math.min(aMaxY, bMaxY);

        return Math.max(0, maxX - minX) *
                Math.max(0, maxY - minY);
    }

    static boolean contains(
            float aMinX,
            float aMinY,
            float aMaxX,
            float aMaxY,
            Node2D b) {
        return contains(aMinX, aMinY, aMaxX, aMaxY, b.getMinX(), b.getMinY(), b.getMaxX(), b.getMaxY());
    }

    static boolean contains(
            float aMinX,
            float aMinY,
            float aMaxX,
            float aMaxY,
            float bMinX,
            float bMinY,
            float bMaxX,
            float bMaxY
    ) {
        return aMinX <= bMinX &&
                aMinY <= bMinY &&
                bMaxX <= aMaxX &&
                bMaxY <= aMaxY;
    }

    /**
     * Check if two nodes intersect. Order doesn't matter
     *
     * @param a node a
     * @param b node b
     * @return whether the two nodes intersect
     */
    static boolean intersects(Node2D a, Node2D b) {
        return intersects(a.getMinX(), a.getMinY(), a.getMaxX(), a.getMaxY(), b.getMinX(), b.getMinY(), b.getMaxX(), b.getMaxY());
    }

    /**
     * @param aMinX min x of a
     * @param aMinY min y of a
     * @param aMaxX max x of a
     * @param aMaxY max y of a
     * @param b     the other bound
     * @return whether the two bounds intersect
     */
    public static boolean intersects(float aMinX,
                                     float aMinY,
                                     float aMaxX,
                                     float aMaxY
            , Node2D b) {
        return intersects(aMinX, aMinY, aMaxX, aMaxY, b.getMinX(), b.getMinY(), b.getMaxX(), b.getMaxY());
    }

    /**
     * @param aMinX min x of a
     * @param aMinY min y of a
     * @param aMaxX max x of a
     * @param aMaxY max y of a
     * @param bMinX min x of b
     * @param bMinY min y of b
     * @param bMaxX max x of b
     * @param bMaxY max y of b
     * @return whether the two bounds intersect
     */
    public static boolean intersects(
            float aMinX,
            float aMinY,
            float aMaxX,
            float aMaxY,
            float bMinX,
            float bMinY,
            float bMaxX,
            float bMaxY

    ) {
        return bMinX <= aMaxX &&
                bMinY <= aMaxY &&
                bMaxX >= aMinX &&
                bMaxY >= aMinY;
    }
}

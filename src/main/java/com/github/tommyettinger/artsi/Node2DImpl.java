package com.github.tommyettinger.artsi;

import com.github.tommyettinger.ds.ObjectList;

import java.util.List;

/**
 * An internal rectangular node
 */
class Node2DImpl extends Node2D {

    double minX = Double.MAX_VALUE;
    double minY = Double.MAX_VALUE;
    double maxX = -Double.MAX_VALUE;
    double maxY = -Double.MAX_VALUE;

    /**
     * Create a rectangular data node
     *
     * @param minX the minimum x component of the data
     * @param minY the maximum x component of the data
     * @param maxX the minimum y component of the data
     * @param maxY the maximum y component of the data
     */
    protected Node2DImpl(double minX, double minY, double maxX, double maxY) {
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
    protected final void set(double minX, double minY, double maxX, double maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    @Override
    public double getMinX() {
        return minX;
    }

    @Override
    public double getMinY() {
        return minY;
    }

    @Override
    public double getMaxX() {
        return maxX;
    }

    @Override
    public double getMaxY() {
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
        return Double.compare(a.getMinX(), b.getMinX());
    }

    /**
     * Comparator of the minimum y component
     *
     * @param a left hand operand
     * @param b right hand operand
     * @return an int representing whether a is greater/lesser/equal to the b
     */
    static int compareMinY(Node2D a, Node2D b) {
        return Double.compare(a.getMinY(), b.getMinY());
    }

    /**
     * Calculate the area of performing a union between two nodes
     *
     * @param a node a
     * @param b node b
     * @return the area of performing a union with nodes a and b
     */
    static double enlargedArea(Node2D a, Node2D b) {
        return enlargedArea(a.getMinX(), a.getMinY(), a.getMaxX(), a.getMaxY(), b.getMinX(), b.getMinY(), b.getMaxX(), b.getMaxY());
    }

    /**
     * Calculate the area of intersection between two nodes
     *
     * @param a node a
     * @param b node b
     * @return the area of intersecting a with b
     */
    static double intersectionArea(Node2D a, Node2D b) {
        return intersectionArea(a.getMinX(), a.getMinY(), a.getMaxX(), a.getMaxY(), b.getMinX(), b.getMinY(), b.getMaxX(), b.getMaxY());
    }

    static boolean contains(Node2D a, Node2D b) {
        return contains(a.getMinX(), a.getMinY(), a.getMaxX(), a.getMaxY(), b.getMinX(), b.getMinY(), b.getMaxX(), b.getMaxY());
    }

    static boolean contains(Node2D a,
                            double bMinX,
                            double bMinY,
                            double bMaxX,
                            double bMaxY) {
        return contains(a.getMinX(), a.getMinY(), a.getMaxX(), a.getMaxY(), bMinX, bMinY, bMaxX, bMaxY);
    }

    static double enlargedArea(
            double aMinX,
            double aMinY,
            double aMaxX,
            double aMaxY,
            double bMinX,
            double bMinY,
            double bMaxX,
            double bMaxY
    ) {
        return (Math.max(bMaxX, aMaxX) - Math.min(bMinX, aMinX)) *
                (Math.max(bMaxY, aMaxY) - Math.min(bMinY, aMinY));
    }

    static double intersectionArea(
            double aMinX,
            double aMinY,
            double aMaxX,
            double aMaxY,
            double bMinX,
            double bMinY,
            double bMaxX,
            double bMaxY
    ) {
        double minX = Math.max(aMinX, bMinX);
        double minY = Math.max(aMinY, bMinY);
        double maxX = Math.min(aMaxX, bMaxX);
        double maxY = Math.min(aMaxY, bMaxY);

        return Math.max(0, maxX - minX) *
                Math.max(0, maxY - minY);
    }

    static boolean contains(
            double aMinX,
            double aMinY,
            double aMaxX,
            double aMaxY,
            Node2D b) {
        return contains(aMinX, aMinY, aMaxX, aMaxY, b.getMinX(), b.getMinY(), b.getMaxX(), b.getMaxY());
    }

    static boolean contains(
            double aMinX,
            double aMinY,
            double aMaxX,
            double aMaxY,
            double bMinX,
            double bMinY,
            double bMaxX,
            double bMaxY
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
    public static boolean intersects(double aMinX,
                                     double aMinY,
                                     double aMaxX,
                                     double aMaxY
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
            double aMinX,
            double aMinY,
            double aMaxX,
            double aMaxY,
            double bMinX,
            double bMinY,
            double bMaxX,
            double bMaxY

    ) {
        return bMinX <= aMaxX &&
                bMinY <= aMaxY &&
                bMaxX >= aMinX &&
                bMaxY >= aMinY;
    }
}

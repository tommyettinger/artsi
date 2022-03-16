package com.github.tommyettinger.artsi;

import com.github.tommyettinger.ds.ObjectList;
import com.github.tommyettinger.ds.support.BitConversion;

import java.util.List;

/**
 * A 2D node that can be inserted into a 2D spatial tree
 */
public abstract class Node2D {
    int height;
    final boolean leaf;

    final ObjectList<Node2D> children;

    /**
     * Create a leaf node
     */
    protected Node2D() {
        this(true);
    }

    protected Node2D(boolean isLeaf) {
        this.leaf = isLeaf;
        this.children = null;
    }

    protected Node2D(ObjectList<Node2D> children, boolean isLeaf) {
        this.children = children;
        this.leaf = isLeaf;
    }

    /**
     * Create a node either with children, or an empty leaf
     *
     * @param children the children to initialize with (if {@code null}, assumed to be an orphaned node)
     */
    protected Node2D(ObjectList<Node2D> children) {
        this(children, children == null);
    }

    protected Node2D(Node2D... nodes) {
        this.children = ObjectList.with(nodes);
        leaf = false;
    }


    /**
     * @return the minimum x component
     */
    public abstract double getMinX();

    /**
     * @return the minimum y component
     */
    public abstract double getMinY();

    /**
     * @return the center x component
     * @implNote note final so that point data can avoid having to do unnecessary calculations
     */
    public double getMidX() {
        return (getMaxX() + getMinX()) * .5;
    }

    /**
     * @return the center y component
     * @implNote note final so that point data can avoid having to do unnecessary calculations
     */
    public double getMidY() {
        return (getMaxY() + getMinY()) * .5;
    }

    /**
     * @return the maximum x component
     */
    public abstract double getMaxX();

    /**
     * @return the maximum y component
     */
    public abstract double getMaxY();

    /**
     * @param minX the minimum x component
     * @param minY the minimum y component
     * @param maxX the maximum x component
     * @param maxY the maximum y component
     * @return whether this node contains a rectangle defined by its extends
     */
    public boolean contains(double minX, double minY, double maxX, double maxY) {
        return Node2DImpl.contains(this, minX, minY, maxX, maxY);
    }

    /**
     * @param minX the minimum x component
     * @param minY the minimum y component
     * @param maxX the maximum x component
     * @param maxY the maximum y component
     * @return whether this node is contained within a rectangle defined by its extends
     */
    public boolean isContainedIn(double minX, double minY, double maxX, double maxY) {
        return Node2DImpl.contains(minX, minY, maxX, maxY, this);
    }

    /**
     * @param minX the minimum x component
     * @param minY the minimum y component
     * @param maxX the maximum x component
     * @param maxY the maximum y component
     * @return whether this node intersects a rectangle defined by its extends
     */
    public boolean intersects(double minX, double minY, double maxX, double maxY) {
        return Node2DImpl.intersects(minX, minY, maxX, maxY, this);
    }

    void recalculateBBox() {
        distBBox(this, 0, children.size(), this);
    }

    double calculateWidth() {
        return getMaxX() - getMinX();
    }

    double calculateHeight() {
        return getMaxY() - getMinY();
    }

    double calculateArea() {
        return calculateWidth() * calculateHeight();
    }

    double calculateHalfPerimeter() {
        return calculateWidth() + calculateHeight();
    }

    @Override
    public String toString() {
        return "Node {minX: " + getMinX() + ", minY: " + getMinY() + ", maxX: " + getMaxX() + ", maxY: " + getMaxY()
                + (leaf ? ", data}" : ", numChildren: " + children.size() + "}");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node2D)) return false;
        Node2D node = (Node2D) o;
        return Double.compare(node.getMinX(), getMinX()) == 0 && Double.compare(node.getMinY(), getMinY()) == 0 && Double.compare(node.getMaxX(), getMaxX()) == 0 && Double.compare(node.getMaxY(), getMaxY()) == 0;
    }

    @Override
    public int hashCode() {
        int h = BitConversion.doubleToMixedIntBits(getMinX()) * 31 * 31 * 31
                + BitConversion.doubleToMixedIntBits(getMinY()) * 31 * 31
                + BitConversion.doubleToMixedIntBits(getMaxX()) * 31
                + BitConversion.doubleToMixedIntBits(getMaxY());
        return h ^ h >>> 16;
    }

    // min bounding rectangle of node children from k to p-1
    static Node2D distBBox(Node2D node, int k, int p, Node2D destNode) {
        if (destNode == null) {
            destNode = new Node2DImpl((ObjectList<Node2D>) null);
        }
        destNode.set(
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                Double.NEGATIVE_INFINITY
        );

        for (int i = k; i < p; i++) {
            Node2D child = node.children.get(i);
            union(destNode, child);
        }

        return destNode;
    }

    /**
     * Set one node to the size of the union with another node
     *
     * @param a the node to set
     * @param b the node to union with
     */
    static void union(Node2D a, Node2D b) {
        a.set(
                Math.min(a.getMinX(), b.getMinX()),
                Math.min(a.getMinY(), b.getMinY()),
                Math.max(a.getMaxX(), b.getMaxX()),
                Math.max(a.getMaxY(), b.getMaxY())
        );
    }

    /**
     * Set the bounds of this
     *
     * @param minX the minimum x component of the data
     * @param minY the maximum x component of the data
     * @param maxX the minimum y component of the data
     * @param maxY the maximum y component of the data
     * @implNote this must be implemented by other internal nodes to allow for unions with other nodes
     */
    void set(double minX, double minY, double maxX, double maxY) {

    }
}

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
    public abstract float getMinX();

    /**
     * @return the minimum y component
     */
    public abstract float getMinY();

    /**
     * @return the center x component
     * @implNote note final so that point data can avoid having to do unnecessary calculations
     */
    public float getMidX() {
        return (getMaxX() + getMinX()) * .5f;
    }

    /**
     * @return the center y component
     * @implNote note final so that point data can avoid having to do unnecessary calculations
     */
    public float getMidY() {
        return (getMaxY() + getMinY()) * .5f;
    }

    /**
     * @return the maximum x component
     */
    public abstract float getMaxX();

    /**
     * @return the maximum y component
     */
    public abstract float getMaxY();

    /**
     * @param minX the minimum x component
     * @param minY the minimum y component
     * @param maxX the maximum x component
     * @param maxY the maximum y component
     * @return whether this node contains a rectangle defined by its extends
     */
    public boolean contains(float minX, float minY, float maxX, float maxY) {
        return Node2DImpl.contains(this, minX, minY, maxX, maxY);
    }

    /**
     * @param minX the minimum x component
     * @param minY the minimum y component
     * @param maxX the maximum x component
     * @param maxY the maximum y component
     * @return whether this node is contained within a rectangle defined by its extends
     */
    public boolean isContainedIn(float minX, float minY, float maxX, float maxY) {
        return Node2DImpl.contains(minX, minY, maxX, maxY, this);
    }

    /**
     * @param minX the minimum x component
     * @param minY the minimum y component
     * @param maxX the maximum x component
     * @param maxY the maximum y component
     * @return whether this node intersects a rectangle defined by its extends
     */
    public boolean intersects(float minX, float minY, float maxX, float maxY) {
        return Node2DImpl.intersects(minX, minY, maxX, maxY, this);
    }

    void recalculateBBox() {
        distBBox(this, 0, children.size(), this);
    }

    float calculateWidth() {
        return getMaxX() - getMinX();
    }

    float calculateHeight() {
        return getMaxY() - getMinY();
    }

    float calculateArea() {
        return calculateWidth() * calculateHeight();
    }

    float calculateHalfPerimeter() {
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
        return Float.compare(node.getMinX(), getMinX()) == 0 && Float.compare(node.getMinY(), getMinY()) == 0 && Float.compare(node.getMaxX(), getMaxX()) == 0 && Float.compare(node.getMaxY(), getMaxY()) == 0;
    }

    @Override
    public int hashCode() {
        int h = BitConversion.floatToRawIntBits(getMinX()) * 31 * 31 * 31
                + BitConversion.floatToRawIntBits(getMinY()) * 31 * 31
                + BitConversion.floatToRawIntBits(getMaxX()) * 31
                + BitConversion.floatToRawIntBits(getMaxY());
        return h ^ h >>> 16;
    }

    // min bounding rectangle of node children from k to p-1
    static Node2D distBBox(Node2D node, int k, int p, Node2D destNode) {
        if (destNode == null) {
            destNode = new Node2DImpl((ObjectList<Node2D>) null);
        }
        destNode.set(
                Float.POSITIVE_INFINITY,
                Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY,
                Float.NEGATIVE_INFINITY
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
    void set(float minX, float minY, float maxX, float maxY) {

    }
}

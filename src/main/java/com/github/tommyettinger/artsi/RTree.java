package com.github.tommyettinger.artsi;

import com.github.tommyettinger.ds.ObjectDeque;
import com.github.tommyettinger.ds.ObjectList;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import static com.github.tommyettinger.artsi.Node2D.distBBox;
import static com.github.tommyettinger.artsi.Node2D.union;
import static com.github.tommyettinger.artsi.Node2DImpl.enlargedArea;
import static com.github.tommyettinger.artsi.Node2DImpl.intersectionArea;


/**
 * Rtree implementation, heavily based on <a href="https://github.com/mourner/rbush">rbush</a>, with support for generics
 * and functional programming
 *
 * @param <T> the type of the leaf node
 */
public class RTree<T extends Node2D> {

    /**
     * @param <T> the "type" of the tree
     * @return an empty, unmodifiable tree
     */
    @SuppressWarnings("unchecked")
    public static <T extends Node2D> RTree<T> emptyTree() {
        return (RTree<T>) EMPTY_TREE;
    }

    private static final class UnmodifiableRTee<T extends Node2D> extends RTree<T> {
        @Override
        public void put(T item) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(float minX, float minY, float maxX, float maxY, Predicate<T> equalsFn) {
            return false;
        }

    }

    private static final RTree<?> EMPTY_TREE = new UnmodifiableRTee<>();

    private final TreeTraversal traversal;
    /**
     * The default number of maximum entries per node
     */
    static final int DEFAULT_MAX_ENTRIES = 9;
    /**
     * Default bulk loader
     */
    public static BulkLoader DEFAULT_BULK_LOADER = BulkLoader.OVERLAP_MINIMIZING_TOPDOWN;
    /**
     * The minimum entries per node
     */
    final int minEntries;
    /**
     * The maximum entries per node
     */
    final int maxEntries;
    /**
     * The root of the tree
     */
    Node2DImpl root;
    private int numData = 0;

    /**
     * Create an Rtree
     *
     * @param traversal         the mode of traversal
     * @param maxEntriesPerNode the maximum entries per node
     */
    public RTree(TreeTraversal traversal, int maxEntriesPerNode) {
        this.traversal = traversal;
        this.maxEntries = Math.max(4, maxEntriesPerNode);
        // min node fill is 40% for best performance
        this.minEntries = (int) Math.max(2, Math.ceil(this.maxEntries * 0.4));
        this.clear();
    }

    /**
     * Create an Rtree
     *
     * @param maxEntriesPerNode the maximum entries per node
     */
    public RTree(int maxEntriesPerNode) {
        this(TreeTraversal.RECURSIVE, maxEntriesPerNode);
    }

    /**
     * Create an Rtree with a maximum of 9 items per node
     */
    public RTree() {
        this(TreeTraversal.RECURSIVE, DEFAULT_MAX_ENTRIES);
    }

    /**
     * @param minX the minimum x component
     * @param minY the minimum y component
     * @param maxX the maximum x component
     * @param maxY the maximum y component
     * @return if any of the items in the Rtree intersect the bounds
     */
    public boolean collides(float minX, float minY, float maxX, float maxY) {
        return traversal.collides(root, minX, minY, maxX, maxY);
    }

    /**
     * @param minX the minimum x component
     * @param minY the minimum y component
     * @param maxX the maximum x component
     * @param maxY the maximum y component
     * @return a list of the items that intersect the bounds
     */

    public List<? extends T> search(float minX, float minY, float maxX, float maxY) {
        return traversal.search(root, new LinkedList<>(), minX, minY, maxX, maxY);
    }

    /**
     * Return a collection of matches in the given range. This can be useful if, for example, the matches need to be in a
     * specific order
     *
     * @param minX the minimum x component of the data
     * @param minY the maximum x component of the data
     * @param maxX the minimum y component of the data
     * @param maxY the maximum y component of the data
     * @return the leaf nodes in the given range
     */
    public Collection<? extends T> search(final Collection<T> out, float minX, float minY, float maxX, float maxY) {
        return traversal.search(root, out, minX, minY, maxX, maxY);
    }

    /**
     * Traverse the tree
     *
     * @param nodePredicate the test to see if traversal should continue down the tree
     * @param leafFunction  the test to see if the leaf is acceptable, if so, stop the traversal
     */
    public void traverse(Predicate<Node2D> nodePredicate, Predicate<T> leafFunction) {
        traversal.traverse(root, nodePredicate, leafFunction);
    }

    /**
     * Add a node into the tree
     *
     * @param item the item to add
     */
    public void put(T item) {
        if (item == null) {
            return;
        }
        put(item, this.root.height - 1);
    }

    /**
     * Add a number of leaf nodes using the default bulk loader
     *
     * @param data the leaves to add
     */
    @SafeVarargs
    public final void putAll(T... data) {
        putAll(DEFAULT_BULK_LOADER, data);
    }

    /**
     * Add a number of leaf nodes using the request bulk loader
     *
     * @param loader the bulk loader to use
     * @param data   the data to add
     */
    @SafeVarargs
    public final void putAll(BulkLoader loader, T... data) {
        if (data == null || data.length == 0) {
            return;
        }
        if (data.length < minEntries) {
            for (T datum : data) {
                this.put(datum);
            }
            return;
        }

        // recursively build the tree with the given data from scratch using the given bulk loader
        mergeSubtree(this, loader.load(this, data), data.length);
    }

    /**
     * Remove an element from the tree
     *
     * @param minX     the minimum x component of the data
     * @param minY     the maximum x component of the data
     * @param maxX     the minimum y component of the data
     * @param maxY     the maximum y component of the data
     * @param equalsFn the test on the node to check that it is the correct value
     * @return whether the node was correctly removed
     */
    @SuppressWarnings("unchecked")
    public boolean remove(float minX, float minY, float maxX, float maxY, Predicate<T> equalsFn) {
        final Node2D[] path = new Node2D[root.height];
        Node2D node = this.root, parent = null;
        final int[] indices = new int[node.height];
        int i = 0, idx = 0;
        int level = 0;
        boolean goingUp = false;
        while (node != null || level > 0) {
            if (node == null) { // go up
                node = path[--level];
                parent = path[level];
                i = indices[--idx];
                goingUp = true;
            }

            // check current node
            if (node.leaf && equalsFn.test((T) node)) {
                assert parent != null;
                if (parent.children.remove(node)) {
                    --numData; //decrement here as otherwise calling clear later on will result in size being -1
                    // item found, remove the item and condense tree upwards
                    while (level > 0) {
                        if (path[--level].children.size() == 0) {
                            if (level > 0) {
                                path[level - 1].children.remove(path[level]);
                            } else {
                                this.clear();
                            }
                        } else {
                            path[level].recalculateBBox();
                        }
                        path[level] = null;
                    }
                    return true;
                }
            }

            if (!goingUp && !node.leaf && node.contains(minX, minY, maxX, maxY)) { // go down
                path[level++] = node;
                indices[idx++] = i;
                i = 0;
                parent = node;
                node = node.children.get(0);

            } else if (parent != null) { // go right
                node = parent.children.get(++i);
                goingUp = false;

            } else {
                node = null; // nothing found
            }
        }
        return false;
    }

    /**
     * Traverse through the tree until a match is found
     *
     * @param nodePredicate the function to apply to internal nodes
     * @param leafFunction  the function to apply to leaf nodes
     * @return the matching leaf node
     */
    @SuppressWarnings("unchecked")
    public T findAny(Predicate<Node2D> nodePredicate, Predicate<T> leafFunction) {
        final Object[] out = new Object[1];
        traverse(nodePredicate, l -> {
            if (leafFunction.test(l)) {
                out[0] = l;
                //return true to terminate
                return true;
            }
            return false;
        });
        @SuppressWarnings("unchecked") final T cast = (T) out[0];
        return cast;
    }

    /**
     * Traverse through the tree until all matching leaves are found
     *
     * @param nodePredicate the function to apply to internal nodes
     * @param leafFunction  the function to apply to leaf nodes
     * @return an iterable of the matching data (these will most likely be {@link LinkedList})
     */
    public List<? extends T> findAll(Predicate<Node2D> nodePredicate, Predicate<T> leafFunction) {
        return findAll(new LinkedList<>(), nodePredicate, leafFunction);
    }

    /**
     * Traverse through the tree until all matching leaves are found
     *
     * @param out           the output collection
     * @param nodePredicate the function to apply to internal nodes
     * @param leafFunction  the function to apply to leaf nodes
     * @param <S>           the type of the collection
     * @return an iterable of the matching data (these will most likely be {@link LinkedList})
     */
    public <S extends Collection<T>> S findAll(S out, Predicate<Node2D> nodePredicate, Predicate<T> leafFunction) {
        traverse(nodePredicate, l -> {
            if (leafFunction.test(l)) {
                out.add(l);
            }
            //return false to continue traversal
            return false;
        });
        return out;
    }

    /**
     * Traverse through the tree until the "minimum" match is found (as defined by a comparator)
     *
     * @param nodePredicate the function to apply to internal nodes
     * @param leafFunction  the function to apply to leaf nodes
     * @param sort          the comparator used to sort the data
     * @return the T item that sorts as having the lowest value, or null if no leaf nodes were accepted
     */
    @SuppressWarnings("unchecked")
    public T findNearest(Predicate<Node2D> nodePredicate, Predicate<Node2D> leafFunction, Comparator<T> sort) {
        final Object[] out = new Object[1];
        traverse(nodePredicate, l -> {
            if (leafFunction.test(l)) {
                if (out[0] == null || sort.compare((T) out[0], l) > 0) {
                    out[0] = l;
                }
            }
            //return false to continue traversal
            return false;
        });
        return (T) out[0];
    }

    /**
     * Traverse through the tree until the "maximum" match is found (as defined be a comparator)
     *
     * @param nodePredicate the function to apply to internal nodes
     * @param leafFunction  the function to apply to leaf nodes
     * @param sort          the comparator used to sort the data
     * @return the T item that sorts as having the highest value, or null if no leaf nodes were accepted
     */
    @SuppressWarnings("unchecked")
    public T findFurthest(Predicate<Node2D> nodePredicate, Predicate<T> leafFunction, Comparator<T> sort) {
        final Object[] out = new Object[1];
        traverse(nodePredicate, l -> {
            if (leafFunction.test(l)) {
                if (out[0] == null || sort.compare((T) out[0], l) < 0) {
                    out[0] = l;
                }
            }
            //return false to continue traversal
            return false;
        });
        return (T) out[0];
    }

    /**
     * @return an iterable over the leaves in the tree
     */
    @SuppressWarnings("unchecked")
    public Iterable<T> leaves() {
        return () -> new Iterator<T>() {
            private final ObjectDeque<Node2D> stack = new ObjectDeque<>();

            {
                stack.add(root);
            }

            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < size();
            }

            @Override
            public T next() {
                while (!stack.isEmpty()) {
                    final Node2D node = stack.pop();
                    if (node.leaf) {
                        ++i;
                        return (T) node;
                    } else {
                        stack.addAll(node.children);
                    }
                }
                throw new IndexOutOfBoundsException();
            }
        };
    }

    /**
     * @param out any modifiable List of T or a subtype; will have the leaves appended
     * @return a list of the leaf nodes in the tree
     */
    public List<? extends T> getLeaves(List<? extends T> out) {
        return traversal.getLeaves(root, out);
    }

    /**
     * @return a list of the leaf nodes in the tree
     */
    public List<? extends T> getLeaves() {
        return traversal.getLeaves(root, new ObjectList<>(size()));
    }

    /**
     * Remove all the elements from the tree
     */
    public void clear() {
        this.root = new Node2DImpl(new ObjectList<>(minEntries));
        root.height = 0;
        numData = 0;
    }

    /**
     * @return the number of data points in the tree
     */
    public int size() {
        return numData;
    }

    /**
     * @return whether the tree is empty
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * @return the minimum x value in the tree
     */
    public float getMinX() {
        return root.minX;
    }

    /**
     * @return the minimum y value in the tree
     */
    public float getMinY() {
        return root.minY;
    }

    /**
     * @return the maximum x value in the tree
     */
    public float getMaxX() {
        return root.maxX;
    }

    /**
     * @return the maximum y value in the tree
     */
    public float getMaxY() {
        return root.maxY;
    }

    @Override
    public String toString() {
        return "RTree containing " + size() + (size() == 1 ? " node {min: {x: " :  " nodes {min: {x: ")
                + root.minX + ", y: " + root.minY + "}, max: {x: " + root.maxX + ", y: " + root.maxY + "}}";
    }

    /**
     * Merge a subtree into an Rtree
     *
     * @param tree        the tree to merge into
     * @param subtreeRoot the root of the subtree
     * @param numNewData  the number of data nodes
     * @param <T>         the type of the data
     */
    @SuppressWarnings("unchecked")
    private static <T extends Node2D> void mergeSubtree(RTree<T> tree, Node2DImpl subtreeRoot, int numNewData) {

        tree.numData += numNewData;

        if (tree.root.children.size() == 0) {
            // save as is if tree is empty
            tree.root = subtreeRoot;
        } else if (subtreeRoot.height == 1) {
            for (final Node2D datum : subtreeRoot.children) {
                tree.put((T) datum);
                --tree.numData;
            }
        } else if (tree.root.height == subtreeRoot.height) {
            // split root if trees have the same height
            tree.splitRoot(tree.root, subtreeRoot);

        } else {
            if (tree.root.height < subtreeRoot.height) {
                // swap trees if inserted one is bigger
                final Node2DImpl tmpNode = tree.root;
                tree.root = subtreeRoot;
                subtreeRoot = tmpNode;
            }
            // insert the small tree into the large tree at appropriate level
            tree.put(subtreeRoot, tree.root.height - subtreeRoot.height - 1);
            --tree.numData;//correct for put operation
        }
    }

    /**
     * Utility method to splice a list
     *
     * @param out         the output list
     * @param collection  the list to splice
     * @param from        the index to splice from
     * @param numElements the number of elements to splice
     * @param <E>         the type of the elements in the lists
     * @param <C>         the type of the collection
     * @return the output collection
     */
    static <E, C extends List<E>> C splice(C out, List<E> collection, int from, int numElements) {
        for (int i = 0; i < numElements; ++i) {
            out.add(collection.remove(from));
        }
        return out;
    }

    private Node2D chooseSubtree(Node2D bbox, Node2D node, int level, ObjectDeque<Node2D> path) {
        while (true) {
            path.add(node);
            if (node.leaf || path.size() - 1 == level) {
                break;
            }
            float minArea = Float.POSITIVE_INFINITY;
            float minEnlargement = Float.POSITIVE_INFINITY;
            Node2D targetNode = null;

            for (int i = 0; i < node.children.size(); i++) {
                Node2D child = node.children.get(i);
                float area = child.calculateArea();
                float enlargement = enlargedArea(bbox, child) - area;

                // choose entry with the least area enlargement
                if (enlargement < minEnlargement) {
                    minEnlargement = enlargement;
                    minArea = Math.min(area, minArea);
                    targetNode = child;
                    // otherwise choose one with the smallest area
                } else if (enlargement == minEnlargement) {
                    if (area < minArea) {
                        minArea = area;
                        targetNode = child;
                    }
                }
            }


            node = targetNode != null ? targetNode : node.children.get(0);

        }

        return node;
    }

    private void put(Node2D item, int level) {
        final ObjectDeque<Node2D> insertPath = new ObjectDeque<>();
        final Node2D node;
        // find the best node for accommodating the item, saving all nodes along the path too
        if (level == -1) {
            node = root;
            root.height = 1;
            level = 0;
            insertPath.add(root);
        } else {
            node = chooseSubtree(item, root, level, insertPath);
        }

        // put the item into the node
        node.children.add(item);
        union(node, item);

        // split on node overflow; propagate upwards if necessary
        while (level >= 0) {
            if (insertPath.get(level).children.size() > maxEntries) {
                split(insertPath, level--);
            } else {
                break;
            }
        }

        // adjust bboxes along the insertion path
        for (int i = level - 1; i >= 0; --i) {
            union(insertPath.get(i), item);
        }
        ++numData;
    }

    // split overflowed node into two
    private void split(ObjectDeque<Node2D> insertPath, int level) {
        final Node2D node = insertPath.get(level);
        int M = node.children.size();
        int m = this.minEntries;

        chooseSplitAxis(node, m, M);

        int splitIndex = chooseSplitIndex(node, m, M);

        final Node2D newNode = new Node2DImpl(new ObjectList<>(node.children, splitIndex, node.children.size() - splitIndex));
        node.children.removeRange(splitIndex, node.children.size());
        newNode.height = node.height;

        node.recalculateBBox();
        newNode.recalculateBBox();

        if (level != 0) {
            insertPath.get(level - 1).children.add(newNode);
        } else {
            splitRoot(node, newNode);
        }
    }

    /**
     * Split root node
     *
     * @param node    the new child node of the root
     * @param newNode the other new child of the root
     */
    private void splitRoot(Node2D node, Node2D newNode) {
        final ObjectList<Node2D> children = new ObjectList<>(minEntries);
        children.add(node);
        children.add(newNode);
        this.root = new Node2DImpl(children);
        this.root.height = node.height + 1;
        this.root.recalculateBBox();
    }

    private int chooseSplitIndex(Node2D node, int m, int M) {
        int index = -1;
        boolean foundIndex = false;
        float minOverlap = Float.POSITIVE_INFINITY;
        float minArea = Float.POSITIVE_INFINITY;

        for (int i = m; i <= M - m; ++i) {
            final Node2D bbox1 = distBBox(node, 0, i, null);
            final Node2D bbox2 = distBBox(node, i, M, null);

            float overlap = intersectionArea(bbox1, bbox2);
            float area = bbox1.calculateArea() + bbox2.calculateArea();

            // choose distribution with minimum overlap
            if (overlap < minOverlap) {
                minOverlap = overlap;
                index = i;
                foundIndex = true;

                minArea = Math.min(area, minArea);

            } else if (overlap == minOverlap) {
                // otherwise choose distribution with minimum area
                if (area < minArea) {
                    minArea = area;
                    index = i;
                    foundIndex = true;
                }
            }
        }

        return !foundIndex ? M - m : index;
    }

    // sorts node children by the best axis for split
    private void chooseSplitAxis(Node2D node, int m, int M) {
        float xMargin = this.allDistMargin(node, m, M, Node2DImpl::compareMinX);
        float yMargin = this.allDistMargin(node, m, M, Node2DImpl::compareMinY);

        // if total distributions margin value is minimal for x, sort by minX,
        // otherwise it's already sorted by minY
        if (xMargin < yMargin) {
            node.children.sort(Node2DImpl::compareMinX);
        }
    }

    // total margin of all possible split distributions where each node is at least m full
    private float allDistMargin(Node2D node, int m, int M, Comparator<Node2D> compare) {

        node.children.sort(compare);

        final Node2D leftBBox = distBBox(node, 0, m, null);
        final Node2D rightBBox = distBBox(node, M - m, M, null);
        float margin = leftBBox.calculateHalfPerimeter() + rightBBox.calculateHalfPerimeter();

        for (int i = m; i < M - m; i++) {
            Node2D child = node.children.get(i);
            union(leftBBox, child);
            margin += leftBBox.calculateHalfPerimeter();
        }

        for (int i = M - m - 1; i >= m; i--) {
            Node2D child = node.children.get(i);
            union(rightBBox, child);
            margin += rightBBox.calculateHalfPerimeter();
        }

        return margin;
    }

}

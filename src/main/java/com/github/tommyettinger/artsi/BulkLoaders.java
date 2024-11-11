package com.github.tommyettinger.artsi;

import com.github.tommyettinger.ds.ObjectList;
import com.github.tommyettinger.ds.QuickSelect;
import com.github.tommyettinger.ds.support.sort.ObjectComparators;
import com.github.tommyettinger.function.IntIntToIntBiFunction;

import java.util.Arrays;

/**
 * The actual implementation of bulk loaders
 */
final class BulkLoaders {
    private BulkLoaders() {

    }

    /**
     * Overlap minimizing top-down bulk loader. Adapted from <a href="https://github.com/mourner/rbush">rbush</a>.
     *
     * @param minEntries the min entries of the tree
     * @param maxEntries the max entries of the tree
     * @param items      the items to add
     * @return the root node of this subtree
     */
    static Node2DImpl OMT(int minEntries, int maxEntries, Node2D[] items) {
        return OMT(minEntries, maxEntries, items, 0, items.length - 1, -1);
    }

    private static Node2DImpl OMT(int minEntries, int maxEntries, Node2D[] items, int left, int right, int height) {

        int N = right - left + 1;
        int M = maxEntries;
        Node2DImpl node;

        if (N <= M) {
            // reached leaf level; return leaf
            node = new Node2DImpl(Arrays.copyOfRange(items, left, right + 1));
            node.height = 1;
            node.recalculateBBox();
            return node;
        }
        if (height == -1) {
            // target height of the bulk-loaded tree
            height = (int) Math.ceil(Math.log(N) / Math.log(M));

            // target number of root entries to maximize storage utilization
            M = (int) Math.ceil(N / Math.pow(M, height - 1));

        }

        node = new Node2DImpl(new ObjectList<>(minEntries));
        node.height = height;

        // split the items into M mostly square tiles

        int N2 = (int) Math.ceil((float) N / M);
        int N1 = (int) (N2 * Math.ceil(Math.sqrt(M)));

        QuickSelect.multiSelect(items, Node2DImpl::compareMinX, left, right, N1);

        for (int i = left; i <= right; i += N1) {

            int right2 = Math.min(i + N1 - 1, right);
            QuickSelect.multiSelect(items, Node2DImpl::compareMinY, i, right2, N2);
            for (int j = i; j <= right2; j += N2) {
                int right3 = Math.min(j + N2 - 1, right2);
                // pack each entry recursively
                node.children.add(OMT(minEntries, maxEntries, items, j, right3, height - 1));
            }
        }

        node.recalculateBBox();
        return node;

    }

    private static Node2DImpl SpaceFillingCurveSorted(int maxEntries, Node2D[] items, IntIntToIntBiFunction curveFunction) {

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        for (Node2D item : items) {
            minX = Math.min(minX, item.getMinX());
            minY = Math.min(minY, item.getMinY());
            maxX = Math.max(maxX, item.getMaxX());
            maxY = Math.max(maxY, item.getMaxY());
        }
        int[] indexValues = new int[items.length];
        int MAX_16_BIT = (1 << 16) - 1;

        float width = maxX - minX;
        float height = maxY - minY;
        for (int i = 0; i < items.length; ++i) {
            Node2D b = items[i];
            int x = (int) (MAX_16_BIT * ((b.getMinX() + b.getMaxX()) * .5 - minX) / width);
            int y = (int) (MAX_16_BIT * ((b.getMinY() + b.getMaxY()) * .5 - minY) / height);
            indexValues[i] = curveFunction.applyAsInt(x, y);
        }
        sort(indexValues, items, 0, items.length - 1, maxEntries);
        return mergeUpwards(items, maxEntries, 2);
    }

    /**
     * Hilbert sorted bulk loader. The nodes are sorted by their Hilbert value relative to the center of the
     * items.
     *
     * @param minEntries the min entries per node
     * @param maxEntries the max entries of the tree
     * @param items      the items to add
     * @return the root node of this subtree
     */
    static Node2DImpl HilbertSorted(int minEntries, int maxEntries, Node2D[] items) {
        return SpaceFillingCurveSorted(maxEntries, items, SpaceFillingCurves::encodeHilbert);
    }

    /**
     * Morton curve sorted bulk loaded.
     *
     * @param minEntries the min entries per node
     * @param maxEntries the max entries of the tree
     * @param items      the items to add
     * @return the root node of this subtree
     */
    static Node2DImpl ZOrderSorted(int minEntries, int maxEntries, Node2D[] items) {
        return SpaceFillingCurveSorted(maxEntries, items, SpaceFillingCurves::encodeMorton);

    }

    /**
     * Nearest X bulk loader. The nodes are sorted by the center of their bounds
     *
     * @param minEntries the min entries per node
     * @param maxEntries the max entries of the tree
     * @param items      the items to add
     * @return the root node of this subtree
     */
    static Node2DImpl NearestXSorted(int minEntries, int maxEntries, Node2D[] items) {
        QuickSelect.multiSelect(items, ObjectComparators.comparingFloat(Node2D::getMidX), 0, items.length - 1, maxEntries);
        return mergeUpwards(items, maxEntries, 2);
    }

    /**
     * Sort-tile recursive bulk loader. The nodes are first sorted by their center X value
     * and then sorted by their y axis
     *
     * @param minEntries the min entries per node
     * @param maxEntries the max entries of the tree
     * @param items      the items to add
     * @return the root node of this subtree
     */
    static Node2DImpl STR(int minEntries, int maxEntries, Node2D[] items) {

        final int N = items.length - 1;

        final int N2 = (int) Math.ceil((float) N / maxEntries);
        final int m = (int) Math.ceil(Math.sqrt(maxEntries));
        final int N1 = (N2 * m);

        QuickSelect.multiSelect(items, ObjectComparators.comparingFloat(Node2D::getMidX), 0, N, N1);

        for (int i = 0; i <= N; i += N1) {

            final int right2 = Math.min(i + N1 - 1, N);
            QuickSelect.multiSelect(items, ObjectComparators.comparingFloat(Node2D::getMidY), i, right2, m);
        }
        return mergeUpwards(items, maxEntries, 1);
    }

    /**
     * Utility method to merge nodes into a single root (bottom-up)
     *
     * @param items      the items to merge
     * @param maxEntries the maximum children in each leaf
     * @param height     the current height of the tree
     * @return a new root node for the generates subtree
     */
    static Node2DImpl mergeUpwards(Node2D[] items, int maxEntries, int height) {
        final int N = items.length;
        if (N <= maxEntries) {
            final Node2DImpl node = new Node2DImpl(items);
            node.height = height;
            node.recalculateBBox();
            return node;
        }

        final Node2D[] merged = new Node2D[(int) Math.ceil((float) items.length / maxEntries)];
        for (int i = 0, j = 0; i < items.length; i += maxEntries) {
            final ObjectList<Node2D> children = new ObjectList<>(maxEntries);
            for (int k = 0; k < maxEntries; ++k) {
                int l = i + k;
                if (l >= items.length) {
                    break;
                }
                children.add(items[l]);
            }

            final Node2D n = new Node2DImpl(children);
            n.height = height;
            n.recalculateBBox();
            merged[j++] = n;
        }
        return mergeUpwards(merged, maxEntries, height + 1);
    }


    /**
     * Swap two arrays
     *
     * @param indices the int array
     * @param values  the value arrays
     * @param i       the i index to swap
     * @param j       the j index to swap
     * @param <T>     the type of the values in the value array
     */
    private static <T> void swapWith(int[] indices, T[] values, int i, int j) {
        int tmp = indices[i];
        indices[i] = indices[j];
        indices[j] = tmp;
        T v = values[i];
        values[i] = values[j];
        values[j] = v;
    }

    /**
     * Modified quick sort
     *
     * @param sortIndices the indices to sort by
     * @param values      the values to sort
     * @param left        the left index to sort by
     * @param right       the right index to sort by
     * @param nodeSize    the size of the nodes
     * @param <T>         the type of the data in the values
     */
    private static <T> void sort(int[] sortIndices, T[] values, int left, int right, int nodeSize) {
        if (Math.floor((float) left / nodeSize) >= Math.floor((float) right / nodeSize)) {
            return;
        }
        final int pivot = sortIndices[(left + right) >> 1];
        int i = left - 1,
                j = right + 1;
        while (true) {
            do {
                ++i;
            } while (sortIndices[i] < pivot);
            do {
                --j;
            } while (sortIndices[j] > pivot);
            if (i >= j) {
                break;
            }
            swapWith(sortIndices, values, i, j);
        }
        sort(sortIndices, values, left, j, nodeSize);
        sort(sortIndices, values, j + 1, right, nodeSize);
    }

}

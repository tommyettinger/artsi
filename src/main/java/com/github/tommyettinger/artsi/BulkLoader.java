package com.github.tommyettinger.artsi;

/**
 * Enum for different Rtree bulk loaders
 */
public enum BulkLoader {

    /**
     * Sort the leaves by the mid-x component and merge nodes upwards (can be useful for point data, otherwise not
     * recommended)
     */
    NEAREST_X(BulkLoaders::NearestXSorted),
    /**
     * Sort the leaves by index of hHilbert curve and merge upwards
     */
    HILBERT_SORTED(BulkLoaders::HilbertSorted),
    /**
     * Sort the leaves by index of Morton (Z-order curve) and merge upwards
     */
    Z_ORDER_SORTED(BulkLoaders::ZOrderSorted),
    /**
     * Sort the leaves by x and y, and merge upwards
     */
    SORT_TILE_RECURSIVE(BulkLoaders::STR),
    /**
     * Sort the leaves by x and y, but merge downwards (recommended for most applications)
     */
    OVERLAP_MINIMIZING_TOPDOWN(BulkLoaders::OMT);

    private final BulkLoaderFunction<?> loader;

    @FunctionalInterface
    private interface BulkLoaderFunction<T extends Node2D> {
        /**
         * @param minEntries the minimum entries per node
         * @param maxEntries the maximum entries per node
         * @param items      the items to load
         * @return the root of the subtree
         */
        Node2DImpl accept(int minEntries, int maxEntries, T[] items);
    }

    <T extends Node2D> BulkLoader(BulkLoaderFunction<T> loader) {
        this.loader = loader;
    }

    /**
     * Load the items using the bulk loader
     *
     * @param tree  the tree to load into
     * @param items the items
     * @param <T>   the type of the leaf node
     * @return the root of the subtree
     */
    @SuppressWarnings("unchecked")
    <T extends Node2D> Node2DImpl load(RTree<T> tree, T[] items) {
        return ((BulkLoaderFunction<T>) loader).accept(tree.minEntries, tree.maxEntries, items);
    }

}

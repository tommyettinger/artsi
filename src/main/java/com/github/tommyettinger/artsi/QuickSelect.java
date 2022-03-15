package com.github.tommyettinger.artsi;

import java.util.Arrays;
import java.util.Comparator;

/**
 * QuickSelect (Floyd-Rivest selection), for partial sorting of an array
 */
public final class QuickSelect {
    private QuickSelect() {

    }

    /**
     * Select the kth element in an array using quick select
     *
     * @param arr     the array to sort
     * @param k       the desired sorted blocks
     * @param compare the comparator
     * @param <T>     the type of the data in the array
     */
    public static <T> void quickSelect(T[] arr, int k, Comparator<T> compare) {
        quickSelect(arr, k, 0, arr.length - 1, compare);
    }

    static <T> void quickSelect(T[] arr, int k, int left, int right, Comparator<T> compare) {
        while (right > left) {
            if (right - left > 600) {
                int n = right - left + 1;
                int m = k - left + 1;
                double z = Math.log(n);
                double s = 0.5 * Math.exp(2 * z / 3);
                double sd = 0.5 * Math.sqrt(z * s * (n - s) / n) * (m - n / 2 < 0 ? -1 : 1);
                double newLeft = Math.max(left, Math.floor(k - m * s / n + sd));
                double newRight = Math.min(right, Math.floor(k + (n - m) * s / n + sd));
                quickSelect(arr, k, (int) newLeft, (int) newRight, compare);
            }

            T t = arr[k];
            int i = left;
            int j = right;

            swap(arr, left, k);
            if (compare.compare(arr[right], t) > 0) swap(arr, left, right);

            while (i < j) {
                swap(arr, i, j);
                i++;
                j--;
                while (compare.compare(arr[i], t) < 0) i++;
                while (compare.compare(arr[j], t) > 0) j--;
            }

            if (compare.compare(arr[left], t) == 0) swap(arr, left, j);
            else {
                j++;
                swap(arr, j, right);
            }

            if (j <= k) left = j + 1;
            if (k <= j) right = j - 1;
        }
    }

    /**
     * Utility swap function
     *
     * @param arr the array in which to swap elements
     * @param i   index i
     * @param j   index j
     * @param <T> the type of the data in the array
     */
    private static <T> void swap(T[] arr, int i, int j) {
        T tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
    }

    /**
     * Push an element to an int stack, increasing the size of the stack, if necessary
     *
     * @param stack   the stack
     * @param N       the size of the stack (may not equal length)
     * @param element the element to add to the end (this is not updated)
     * @return the stack
     */
    private static int[] push(int[] stack, int N, int element) {
        if (N < stack.length) {
            stack[N] = element;
            return stack;
        }
        //increase size, in case the new element needs to be at the end
        final int[] newStack = Arrays.copyOf(stack, stack.length + 4);
        newStack[N] = element;
        return newStack;
    }

    // sort an array so that items come in groups of n unsorted items, with groups sorted between each other;
    // combines selection algorithm with binary divide & conquer approach
    static <T> void multiSelect(T[] data, int left, int right, int n, Comparator<T> compare) {
        //Set up a simple stack
        int[] stack = new int[data.length];
        stack[0] = left;
        stack[1] = right;
        int stackSize = 2;

        while (stackSize > 0) {
            right = stack[--stackSize];
            left = stack[--stackSize];

            if (right - left <= n) {
                continue;
            }

            int mid = (int) (left + Math.ceil(((double) right - left) / n / 2) * n);
            quickSelect(data, mid, left, right, compare);

            stack = push(stack, stackSize++, left);
            stack = push(stack, stackSize++, mid);
            stack = push(stack, stackSize++, mid);
            stack = push(stack, stackSize++, right);
        }
    }

}

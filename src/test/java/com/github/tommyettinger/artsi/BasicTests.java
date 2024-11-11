package com.github.tommyettinger.artsi;

import com.github.tommyettinger.ds.ObjectList;
import com.github.tommyettinger.random.AceRandom;

import java.util.Comparator;

public class BasicTests {
    public static void main(String[] args) {
        RTree<PointNode<String>> nameTree = new RTree<>(TreeTraversal.NON_RECURSIVE, 4);
        AceRandom random = new AceRandom(12345L);
        String[] words = {"anteater", "bee", "cat", "dog", "elephant", "frog", "gibbon", "horse", "ibex", "jaguar", "koala", "lemur", "mouse", "nuthatch", "owl", "penguin", "quahog", "ram", "squirrel", "thrush"};
        for(String w : words){
            nameTree.put(new PointNode<>(random.nextExclusiveFloat(), random.nextExclusiveFloat(), w));
        }
        final float targetX = 0.4f, targetY = 0.7f;
        Comparator<PointNode<String>> nearestToTarget = (a, b) -> {
            float diffAX = a.getMidX() - targetX, diffAY = a.getMidY() - targetY;
            float diffBX = b.getMidX() - targetX, diffBY = b.getMidY() - targetY;
            return Float.compare(
                    diffAX * diffAX + diffAY * diffAY,
                    diffBX * diffBX + diffBY * diffBY
                    );
        };
        PointNode<String> best = nameTree.findNearest((n) -> true, (n) -> true, nearestToTarget);
        System.out.println("Best is " + best.data + " at " + best.getMidX() + ", " + best.getMidY());
        ObjectList<PointNode<String>> listing = new ObjectList<>(words.length);
        nameTree.getLeaves(listing);
        listing.sort(nearestToTarget);
        for(PointNode<String> node : listing) {
            System.out.print(node.data + " at " + node.getMidX() + ", " + node.getMidY() + ": ");
            float diffX = node.getMidX() - targetX, diffY = node.getMidY() - targetY;
            System.out.println(diffX * diffX + diffY * diffY);
        }
    }
}

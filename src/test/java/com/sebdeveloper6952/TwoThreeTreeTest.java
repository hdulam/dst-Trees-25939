package com.sebdeveloper6952;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TwoThreeTreeTest {

    // -----------------------------------------------------------------------
    // get / put basics
    // -----------------------------------------------------------------------

    @Test
    void emptyTree() {
        Map<Integer, String> tree = new TwoThreeTree<>();
        assertTrue(tree.isEmpty());
        assertEquals(0, tree.size());
        assertNull(tree.get(42));
        assertFalse(tree.containsKey(42));
    }

    @Test
    void putThenGet() {
        Map<Integer, String> tree = new TwoThreeTree<>();
        assertNull(tree.put(1, "a"));
        assertEquals("a", tree.get(1));
        assertTrue(tree.containsKey(1));
        assertEquals(1, tree.size());
    }

    @Test
    void putReplacesExistingValue() {
        Map<Integer, String> tree = new TwoThreeTree<>();
        tree.put(1, "a");
        assertEquals("a", tree.put(1, "b"));
        assertEquals("b", tree.get(1));
        assertEquals(1, tree.size());
    }

    @Test
    void rejectsNullKeys() {
        Map<Integer, String> tree = new TwoThreeTree<>();
        assertThrows(NullPointerException.class, () -> tree.put(null, "x"));
        assertThrows(NullPointerException.class, () -> tree.get(null));
        assertThrows(NullPointerException.class, () -> tree.remove(null));
    }

    @Test
    void allowsNullValues() {
        Map<Integer, String> tree = new TwoThreeTree<>();
        tree.put(1, null);
        assertNull(tree.get(1));
        assertTrue(tree.containsKey(1));
    }

    // -----------------------------------------------------------------------
    // Insertions that grow the tree upward
    // -----------------------------------------------------------------------

    @Test
    void ascendingInsertionsStayBalanced() {
        // Matches the worked example in CONTENT.md §6.
        TwoThreeTree<Integer, Integer> tree = new TwoThreeTree<>();
        int[] keys = {10, 20, 30, 40, 50, 60, 70, 80};
        for (int k : keys) tree.put(k, k * 10);

        assertEquals(keys.length, tree.size());
        for (int k : keys) assertEquals(k * 10, tree.get(k));
        assertEquals(List.of(10, 20, 30, 40, 50, 60, 70, 80), tree.keysInOrder());
    }

    @Test
    void descendingInsertionsStayBalanced() {
        TwoThreeTree<Integer, Integer> tree = new TwoThreeTree<>();
        for (int i = 100; i >= 1; i--) tree.put(i, i);

        assertEquals(100, tree.size());
        List<Integer> expected = new ArrayList<>();
        for (int i = 1; i <= 100; i++) expected.add(i);
        assertEquals(expected, tree.keysInOrder());
    }

    // -----------------------------------------------------------------------
    // Deletion
    // -----------------------------------------------------------------------

    @Test
    void removeFromThreeNodeLeaf() {
        // Case A: deleting from a 3-node leaf — just drop the key.
        TwoThreeTree<Integer, Integer> tree = seed(10, 20, 30, 40, 50, 60, 70, 80);
        assertEquals(80, tree.remove(80));
        assertFalse(tree.containsKey(80));
        assertEquals(7, tree.size());
    }

    @Test
    void removeTriggersFusionUpToRoot() {
        // Reproduces Example 2 from CONTENT.md §8: removing 70 propagates fusion
        // all the way up and shrinks the tree's height by one.
        TwoThreeTree<Integer, Integer> tree = seed(10, 20, 30, 40, 50, 60, 70);
        assertEquals(70, tree.remove(70));
        assertFalse(tree.containsKey(70));
        assertEquals(List.of(10, 20, 30, 40, 50, 60), tree.keysInOrder());
    }

    @Test
    void removeInternalKeyUsesSuccessor() {
        // Example 3 from CONTENT.md §8: deleting the root key 40.
        TwoThreeTree<Integer, Integer> tree = seed(10, 20, 30, 40, 50, 60, 70, 80);
        assertEquals(40, tree.remove(40));
        assertFalse(tree.containsKey(40));
        assertEquals(List.of(10, 20, 30, 50, 60, 70, 80), tree.keysInOrder());
    }

    @Test
    void removeMissingKeyReturnsNull() {
        TwoThreeTree<Integer, Integer> tree = seed(1, 2, 3, 4, 5);
        assertNull(tree.remove(99));
        assertEquals(5, tree.size());
    }

    @Test
    void removeAllKeysLeavesEmptyTree() {
        TwoThreeTree<Integer, Integer> tree = seed(1, 2, 3, 4, 5, 6, 7);
        for (int k = 1; k <= 7; k++) tree.remove(k);
        assertTrue(tree.isEmpty());
        assertNull(tree.get(1));
    }

    // -----------------------------------------------------------------------
    // Randomized stress test — compares against java.util.TreeMap.
    // -----------------------------------------------------------------------

    @Test
    void randomizedStressMatchesReferenceMap() {
        java.util.TreeMap<Integer, Integer> reference = new java.util.TreeMap<>();
        TwoThreeTree<Integer, Integer> tree = new TwoThreeTree<>();
        Random rng = new Random(0xC0FFEE);

        for (int i = 0; i < 5_000; i++) {
            int key = rng.nextInt(500);
            int op = rng.nextInt(3);
            switch (op) {
                case 0 -> {
                    int v = rng.nextInt();
                    assertEquals(reference.put(key, v), tree.put(key, v));
                }
                case 1 -> assertEquals(reference.get(key), tree.get(key));
                case 2 -> assertEquals(reference.remove(key), tree.remove(key));
            }
            assertEquals(reference.size(), tree.size());
        }

        assertEquals(new ArrayList<>(reference.keySet()), tree.keysInOrder());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static TwoThreeTree<Integer, Integer> seed(int... keys) {
        TwoThreeTree<Integer, Integer> tree = new TwoThreeTree<>();
        List<Integer> ks = new ArrayList<>();
        for (int k : keys) ks.add(k);
        Collections.sort(ks);  // deterministic insertion order
        for (int k : ks) tree.put(k, k);
        return tree;
    }
}

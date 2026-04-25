package com.sebdeveloper6952;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * A 2-3 tree implementation of {@link Map}.
 * <p>
 * Every internal node holds either one key (2-node, two children) or two keys
 * (3-node, three children). All leaves are at the same depth, so search,
 * insert, and delete all run in {@code O(log n)} worst-case time.
 * <p>
 * The algorithms follow the classic formulation: insertion propagates overflow
 * upward by splitting transient 4-nodes; deletion propagates underflow upward
 * via redistribution or fusion with a sibling.
 */
public final class TwoThreeTree<K, V> implements Map<K, V> {

    private final Comparator<? super K> comparator;
    private Node<K, V> root;
    private int size;

    /** Uses the natural ordering of keys; keys must implement {@link Comparable}. */
    public TwoThreeTree() {
        this(null);
    }

    /** Uses the given comparator for key ordering. */
    public TwoThreeTree(Comparator<? super K> comparator) {
        this.comparator = comparator;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public V get(K key) {
        Entry<K, V> entry = findEntry(key);
        return entry == null ? null : entry.value;
    }

    @Override
    public boolean containsKey(K key) {
        return findEntry(key) != null;
    }

    @Override
    public V put(K key, V value) {
        Objects.requireNonNull(key, "key");
        if (root == null) {
            root = Node.leafOf(new Entry<>(key, value));
            size = 1;
            return null;
        }
        PutContext<V> ctx = new PutContext<>();
        SplitResult<K, V> split = insert(root, key, value, ctx);
        if (split != null) {
            Node<K, V> newRoot = Node.leafOf(split.promoted);
            newRoot.children[0] = root;
            newRoot.children[1] = split.newRight;
            root = newRoot;
        }
        if (!ctx.replaced) size++;
        return ctx.previousValue;
    }

    @Override
    public V remove(K key) {
        Objects.requireNonNull(key, "key");
        if (root == null) return null;
        RemoveResult<V> result = delete(root, key);
        if (root.numKeys == 0) {
            // Root absorbed its only key downward; promote its single child.
            root = root.children[0];
        }
        if (result.removed) size--;
        return result.value;
    }

    /** Returns all keys in ascending order. Intended for tests and debugging. */
    public List<K> keysInOrder() {
        List<K> out = new ArrayList<>(size);
        collect(root, out);
        return out;
    }

    // -----------------------------------------------------------------------
    // Lookup
    // -----------------------------------------------------------------------

    private Entry<K, V> findEntry(K key) {
        Objects.requireNonNull(key, "key");
        Node<K, V> node = root;
        while (node != null) {
            int cmp0 = compare(key, node.entries[0].key);
            if (cmp0 == 0) return node.entries[0];
            if (cmp0 < 0) {
                node = node.children[0];
                continue;
            }
            if (node.is2Node()) {
                node = node.children[1];
                continue;
            }
            int cmp1 = compare(key, node.entries[1].key);
            if (cmp1 == 0) return node.entries[1];
            node = (cmp1 < 0) ? node.children[1] : node.children[2];
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // Insertion
    // -----------------------------------------------------------------------

    private SplitResult<K, V> insert(Node<K, V> node, K key, V value, PutContext<V> ctx) {
        int cmp0 = compare(key, node.entries[0].key);
        if (cmp0 == 0) {
            ctx.previousValue = node.entries[0].value;
            ctx.replaced = true;
            node.entries[0].value = value;
            return null;
        }
        if (node.is3Node()) {
            int cmp1 = compare(key, node.entries[1].key);
            if (cmp1 == 0) {
                ctx.previousValue = node.entries[1].value;
                ctx.replaced = true;
                node.entries[1].value = value;
                return null;
            }
        }

        if (node.isLeaf()) {
            return insertIntoLeaf(node, new Entry<>(key, value));
        }

        int childIdx = childIndexFor(node, key);
        SplitResult<K, V> childSplit = insert(node.children[childIdx], key, value, ctx);
        if (childSplit == null) return null;
        return absorbOrSplit(node, childIdx, childSplit.promoted, childSplit.newRight);
    }

    private SplitResult<K, V> insertIntoLeaf(Node<K, V> leaf, Entry<K, V> fresh) {
        if (leaf.is2Node()) {
            if (compare(fresh.key, leaf.entries[0].key) < 0) {
                leaf.entries[1] = leaf.entries[0];
                leaf.entries[0] = fresh;
            } else {
                leaf.entries[1] = fresh;
            }
            leaf.numKeys = 2;
            return null;
        }
        // 3-node → temporary 4-node → split.
        Entry<K, V> a, b, c;
        if (compare(fresh.key, leaf.entries[0].key) < 0) {
            a = fresh;          b = leaf.entries[0]; c = leaf.entries[1];
        } else if (compare(fresh.key, leaf.entries[1].key) < 0) {
            a = leaf.entries[0]; b = fresh;          c = leaf.entries[1];
        } else {
            a = leaf.entries[0]; b = leaf.entries[1]; c = fresh;
        }
        leaf.entries[0] = a;
        leaf.entries[1] = null;
        leaf.numKeys = 1;
        return new SplitResult<>(b, Node.leafOf(c));
    }

    private SplitResult<K, V> absorbOrSplit(
            Node<K, V> node, int childIdx, Entry<K, V> promoted, Node<K, V> newRight) {

        if (node.is2Node()) {
            if (childIdx == 0) {
                node.entries[1] = node.entries[0];
                node.entries[0] = promoted;
                node.children[2] = node.children[1];
                node.children[1] = newRight;
            } else {
                node.entries[1] = promoted;
                node.children[2] = newRight;
            }
            node.numKeys = 2;
            return null;
        }

        // 3-node → split into two 2-nodes, promote middle.
        Entry<K, V> a, b, c;
        Node<K, V> c0, c1, c2, c3;
        switch (childIdx) {
            case 0:
                a = promoted;        b = node.entries[0]; c = node.entries[1];
                c0 = node.children[0]; c1 = newRight;
                c2 = node.children[1]; c3 = node.children[2];
                break;
            case 1:
                a = node.entries[0]; b = promoted;        c = node.entries[1];
                c0 = node.children[0]; c1 = node.children[1];
                c2 = newRight;         c3 = node.children[2];
                break;
            default: // 2
                a = node.entries[0]; b = node.entries[1]; c = promoted;
                c0 = node.children[0]; c1 = node.children[1];
                c2 = node.children[2]; c3 = newRight;
        }

        node.entries[0] = a;
        node.entries[1] = null;
        node.children[0] = c0;
        node.children[1] = c1;
        node.children[2] = null;
        node.numKeys = 1;

        Node<K, V> right = Node.leafOf(c);
        right.children[0] = c2;
        right.children[1] = c3;

        return new SplitResult<>(b, right);
    }

    // -----------------------------------------------------------------------
    // Deletion
    // -----------------------------------------------------------------------

    private RemoveResult<V> delete(Node<K, V> node, K key) {
        int idx = indexOfKey(node, key);
        if (idx >= 0) {
            RemoveResult<V> result = new RemoveResult<>();
            result.value = node.entries[idx].value;
            result.removed = true;

            if (node.isLeaf()) {
                removeEntryFromLeaf(node, idx);
                result.underflow = node.numKeys == 0;
                return result;
            }

            // Internal node: swap with inorder successor, then delete from leaf.
            Node<K, V> successorLeaf = node.children[idx + 1];
            while (!successorLeaf.isLeaf()) {
                successorLeaf = successorLeaf.children[0];
            }
            Entry<K, V> successor = successorLeaf.entries[0];
            node.entries[idx] = successor;

            RemoveResult<V> sub = delete(node.children[idx + 1], successor.key);
            if (sub.underflow) {
                fixUnderflow(node, idx + 1);
                result.underflow = node.numKeys == 0;
            }
            return result;
        }

        if (node.isLeaf()) {
            return RemoveResult.miss();
        }

        int childIdx = childIndexFor(node, key);
        RemoveResult<V> sub = delete(node.children[childIdx], key);
        if (sub.underflow) {
            fixUnderflow(node, childIdx);
            sub.underflow = node.numKeys == 0;
        }
        return sub;
    }

    private void removeEntryFromLeaf(Node<K, V> leaf, int idx) {
        if (idx == 0 && leaf.numKeys == 2) {
            leaf.entries[0] = leaf.entries[1];
        }
        leaf.entries[1] = null;
        leaf.numKeys--;
    }

    private void fixUnderflow(Node<K, V> parent, int emptyIdx) {
        // Prefer redistribution: borrow from a 3-node sibling.
        if (emptyIdx > 0 && parent.children[emptyIdx - 1].is3Node()) {
            redistributeFromLeft(parent, emptyIdx);
            return;
        }
        if (emptyIdx < parent.numKeys && parent.children[emptyIdx + 1].is3Node()) {
            redistributeFromRight(parent, emptyIdx);
            return;
        }
        // All siblings are 2-nodes → fuse with one.
        int leftIdx = (emptyIdx > 0) ? emptyIdx - 1 : emptyIdx;
        fuse(parent, leftIdx);
    }

    private void redistributeFromLeft(Node<K, V> parent, int emptyIdx) {
        Node<K, V> empty = parent.children[emptyIdx];
        Node<K, V> left  = parent.children[emptyIdx - 1];

        // Pull the separator down into the empty node; shift its children right.
        empty.children[1] = empty.children[0];
        empty.children[0] = left.children[2];
        empty.entries[0]  = parent.entries[emptyIdx - 1];
        empty.numKeys     = 1;

        // Rotate left's rightmost key up to replace the separator.
        parent.entries[emptyIdx - 1] = left.entries[1];
        left.entries[1]  = null;
        left.children[2] = null;
        left.numKeys     = 1;
    }

    private void redistributeFromRight(Node<K, V> parent, int emptyIdx) {
        Node<K, V> empty = parent.children[emptyIdx];
        Node<K, V> right = parent.children[emptyIdx + 1];

        empty.entries[0]  = parent.entries[emptyIdx];
        empty.children[1] = right.children[0];
        empty.numKeys     = 1;

        parent.entries[emptyIdx] = right.entries[0];
        right.entries[0]  = right.entries[1];
        right.entries[1]  = null;
        right.children[0] = right.children[1];
        right.children[1] = right.children[2];
        right.children[2] = null;
        right.numKeys     = 1;
    }

    /** Fuses {@code parent.children[leftIdx]} and {@code parent.children[leftIdx + 1]}. */
    private void fuse(Node<K, V> parent, int leftIdx) {
        Node<K, V> left  = parent.children[leftIdx];
        Node<K, V> right = parent.children[leftIdx + 1];
        Entry<K, V> separator = parent.entries[leftIdx];

        if (left.numKeys == 0) {
            // Left was the empty one. Right is a 2-node.
            left.entries[0]  = separator;
            left.entries[1]  = right.entries[0];
            left.children[1] = right.children[0];
            left.children[2] = right.children[1];
        } else {
            // Right was the empty one. Left is a 2-node.
            left.entries[1]  = separator;
            left.children[2] = right.children[0];
        }
        left.numKeys = 2;

        // Drop the separator and the pointer to `right` from the parent.
        if (leftIdx == 0) {
            parent.entries[0]  = parent.entries[1];
            parent.children[1] = parent.children[2];
        }
        parent.entries[1]  = null;
        parent.children[2] = null;
        parent.numKeys--;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private int compare(K a, K b) {
        if (comparator != null) return comparator.compare(a, b);
        return ((Comparable<? super K>) a).compareTo(b);
    }

    private int indexOfKey(Node<K, V> node, K key) {
        if (compare(key, node.entries[0].key) == 0) return 0;
        if (node.is3Node() && compare(key, node.entries[1].key) == 0) return 1;
        return -1;
    }

    private int childIndexFor(Node<K, V> node, K key) {
        int cmp0 = compare(key, node.entries[0].key);
        if (cmp0 < 0) return 0;
        if (node.is2Node()) return 1;
        int cmp1 = compare(key, node.entries[1].key);
        return (cmp1 < 0) ? 1 : 2;
    }

    private void collect(Node<K, V> node, List<K> out) {
        if (node == null) return;
        if (node.isLeaf()) {
            for (int i = 0; i < node.numKeys; i++) out.add(node.entries[i].key);
            return;
        }
        collect(node.children[0], out);
        out.add(node.entries[0].key);
        collect(node.children[1], out);
        if (node.is3Node()) {
            out.add(node.entries[1].key);
            collect(node.children[2], out);
        }
    }

    // -----------------------------------------------------------------------
    // Internal types
    // -----------------------------------------------------------------------

    private static final class Entry<K, V> {
        final K key;
        V value;

        Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private static final class Node<K, V> {
        final Entry<K, V>[] entries;
        final Node<K, V>[] children;
        int numKeys;

        @SuppressWarnings("unchecked")
        private Node() {
            this.entries = (Entry<K, V>[]) new Entry[2];
            this.children = (Node<K, V>[]) new Node[3];
        }

        static <K, V> Node<K, V> leafOf(Entry<K, V> entry) {
            Node<K, V> n = new Node<>();
            n.entries[0] = entry;
            n.numKeys = 1;
            return n;
        }

        boolean isLeaf()   { return children[0] == null; }
        boolean is2Node()  { return numKeys == 1; }
        boolean is3Node()  { return numKeys == 2; }
    }

    private static final class SplitResult<K, V> {
        final Entry<K, V> promoted;
        final Node<K, V> newRight;

        SplitResult(Entry<K, V> promoted, Node<K, V> newRight) {
            this.promoted = promoted;
            this.newRight = newRight;
        }
    }

    private static final class PutContext<V> {
        V previousValue;
        boolean replaced;
    }

    private static final class RemoveResult<V> {
        V value;
        boolean removed;
        boolean underflow;

        static <V> RemoveResult<V> miss() {
            return new RemoveResult<>();
        }
    }
}

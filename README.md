# Guided Exercise — Build a 2-3 Tree `Map<K,V>` in Java

In this exercise you will implement a balanced search tree — specifically a **2-3 tree** — and expose it through a minimal `Map<K,V>` interface. You will build it **incrementally**, one operation at a time: first lookup, then insertion, and finally deletion. By the end you will have a map whose `put`, `get`, and `remove` all run in `O(log n)` worst-case time, regardless of the order in which keys are inserted.

> **Reading before starting.** You will not get far without §1–§7 of `CONTENT.md`. Keep it open; the three operations in this guide map directly to the algorithms there.

---

## Table of Contents

1. [Learning Goals](#1-learning-goals)
2. [Project Setup](#2-project-setup)
3. [The Contract: `Map<K,V>`](#3-the-contract-mapkv)
4. [Phase 1 — Internal Representation](#4-phase-1--internal-representation)
5. [Phase 2 — `get`: Searching the Tree](#5-phase-2--get-searching-the-tree)
6. [Phase 3 — `put`: Insertion](#6-phase-3--put-insertion)
7. [Phase 4 — `remove`: Deletion](#7-phase-4--remove-deletion)
8. [Testing Your Implementation](#8-testing-your-implementation)
9. [Deliverables and Grading](#9-deliverables-and-grading)
10. [Stretch Goals](#10-stretch-goals)

---

## 1. Learning Goals

By completing this exercise you will:

- Implement a balanced search tree from scratch, not just describe one.
- Translate a non-trivial recursive algorithm (split propagation, underflow fixing) from pseudocode into clean Java.
- Reason about invariants — specifically, *why* every insertion and every deletion leaves the tree balanced.
- Understand experientially why B-trees and red-black trees exist: once you have written the 2-3 tree, the others become small generalizations.

---

## 2. Project Setup

The project uses **Gradle** with **Java 17+** and **JUnit 5**.

```bash
# from the repo root
./gradlew build      # compile + run tests
./gradlew test       # run tests only
```

Source layout (Maven/Gradle standard):

```
src/
├── main/java/com/sebdeveloper6952/
│   ├── Map.java             # the interface you must implement
│   └── TwoThreeTree.java    # your implementation goes here
└── test/java/com/sebdeveloper6952/
    └── TwoThreeTreeTest.java  # tests you should keep green
```

> **Tip.** Do not import `java.util.Map`. The `Map` in this exercise is our own minimal interface, defined in `com.sebdeveloper6952.Map`.

---

## 3. The Contract: `Map<K,V>`

Before you write any tree code, understand exactly what you are promising to deliver.

```java
public interface Map<K, V> {
    V put(K key, V value);        // returns previous value, or null
    V get(K key);                 // returns value, or null if absent
    V remove(K key);              // returns removed value, or null
    boolean containsKey(K key);
    int size();
    boolean isEmpty();
}
```

Key rules:

- **Keys may not be `null`** — throw `NullPointerException`.
- **Values may be `null`.** Therefore `containsKey` must distinguish "absent" from "present-but-null"; never implement it as `get(k) != null`.
- `size()` is `O(1)`: maintain a counter, do not traverse the tree.
- **Ordering.** Use `Comparable<K>` by default, but allow an optional `Comparator<? super K>` via a secondary constructor.

### Checkpoint 3.1

- [ ] You can explain, in one sentence, why `containsKey` cannot delegate to `get`.
- [ ] You know which method should return the *old* value on overwrite.

---

## 4. Phase 1 — Internal Representation

Before implementing any operation, design the data carrying the tree. Getting this right makes the next three phases dramatically easier; getting it wrong multiplies the effort.

### 4.1 Recommended approach: one unified `Node` class

A 2-3 tree has *two* kinds of internal node (2-nodes and 3-nodes). You could model them with two classes and polymorphism, but the code grows verbose. A cleaner approach is **one `Node` class** that can hold up to 2 keys and up to 3 children, with a size field telling you which shape it is:

```
class Node<K, V> {
    Entry<K,V>[] entries;   // length 2; only entries[0] used when it's a 2-node
    Node<K,V>[]  children;  // length 3; only children[0..1] used when it's a 2-node
    int numKeys;            // 1 = 2-node, 2 = 3-node
}
```

Add an `Entry<K,V>` record/class to carry the key and mutable value.

### 4.2 Transient 4-nodes

During insertion, a node may *temporarily* hold three keys and four children (§2.3 of `CONTENT.md`). You do **not** need a 4-node type: handle the overflow inside your insertion logic with local variables, split immediately, and return the promotion to the caller. A 4-node should never escape into the tree.

### 4.3 Helper methods on `Node`

Write these small helpers early — they will keep your logic readable:

- `boolean isLeaf()` — true when `children[0] == null`.
- `boolean is2Node()` / `boolean is3Node()` — based on `numKeys`.
- A static factory `Node.leafOf(entry)` that builds a fresh 2-node leaf.

### Checkpoint 4.1

- [ ] `Node` has fixed-size arrays (2 for entries, 3 for children). No `ArrayList`.
- [ ] You can draw, by hand, a 2-node and a 3-node and point to each array slot that is used.
- [ ] You understand that `children[0] == null` is the signal "this node is a leaf".

---

## 5. Phase 2 — `get`: Searching the Tree

Start here. Search is the simplest operation and it confirms that your node layout is sound.

### 5.1 Algorithm

Generalize BST search. At each node:

1. Compare the query key to the first key.
2. If equal — found. If less — descend into `children[0]`.
3. Otherwise, if the node is a 2-node, descend into `children[1]`.
4. If the node is a 3-node, compare against the second key and choose `children[1]` or `children[2]` accordingly.

Refer to §4 of `CONTENT.md` for the exact pseudocode.

### 5.2 Implementation tips

- An **iterative** loop is simpler than recursion here, and costs less stack. Nothing about search mutates the tree, so you do not need the recursive return path.
- Factor out a private helper `Entry<K,V> findEntry(K key)`. Both `get` and `containsKey` should call it — that is how you avoid the null-value pitfall from §3.
- The comparison helper (`Comparable` vs. supplied `Comparator`) appears in every operation. Write it once.

### Checkpoint 5.1

- [ ] `get` and `containsKey` both pass their basic tests (see §8).
- [ ] `get(k)` returns `null` on an empty tree without throwing.
- [ ] Null values are stored and retrieved correctly: `put(1, null)` then `containsKey(1)` returns `true`.

---

## 6. Phase 3 — `put`: Insertion

This is where the 2-3 tree earns its keep. Read §5 of `CONTENT.md` carefully before starting. Insertion has two non-obvious ideas:

1. New keys always land in a **leaf**. You never create a child below an existing node; that is how the balance invariant is preserved.
2. The tree grows **upward**, never downward. Growth happens only when a split cascades past the root.

### 6.1 Recursive skeleton

Use a recursive helper that returns a **split result** when it splits, and `null` when it does not:

```java
private SplitResult<K,V> insert(Node<K,V> node, K key, V value, PutContext<V> ctx) {
    // 1. If the key is already in `node`, overwrite its value and return null.
    // 2. If `node` is a leaf, delegate to `insertIntoLeaf` and return its result.
    // 3. Otherwise, descend into the correct child.
    // 4. If the recursive call returned a split, decide whether this node can
    //    absorb it (becomes a 3-node) or must itself split.
}
```

`SplitResult` carries two things: the **key promoted upward** and the **new right sibling** that the split produced.

`PutContext` is a small mutable struct for the recursion: it lets you return both the promoted key *and* the previous value (needed by `put`'s return contract) without wrapping everything in tuples.

### 6.2 The three cases you must implement

1. **`insertIntoLeaf`**
   - **Leaf is a 2-node:** insert in sorted order; the leaf becomes a 3-node. No split.
   - **Leaf is a 3-node:** three keys overflow into `a < b < c`. Keep `a` in the leaf, put `c` in a fresh right sibling, **promote `b`**.

2. **`absorbOrSplit`** (called on an internal node when a child just split)
   - **Parent is a 2-node:** absorb the promoted key and attach the new right sibling. The parent becomes a 3-node. No split.
   - **Parent is a 3-node:** the parent itself overflows. Form the logical sequence `a < b < c` (two existing keys plus the promotion), split the four children among the two resulting 2-nodes, and **promote `b`**.
   - Three sub-cases of the split, depending on whether the child that split was at index 0, 1, or 2. Work each one out on paper before typing.

3. **Root growth.** If the top-level call returns a non-null `SplitResult`, create a new root holding the promoted key, with the old root and the new right sibling as its two children. **This is the only way the tree's height increases.**

### 6.3 Common pitfalls

- Forgetting to null out slots you vacated (`entries[1] = null`, `children[2] = null`). This bites you in deletion tests, weeks later.
- Mis-ordering the child redistribution during a 3-node split. Draw the four configurations (child split at index 0, 1, 2) on paper before coding.
- Incrementing `size` when the put was actually an *overwrite*. Use the `PutContext.replaced` flag.

### Checkpoint 6.1

Trace §6 of `CONTENT.md` (the `10, 20, 30, …, 80` example) through your implementation. At each step, print or inspect `keysInOrder()` — it should match the sorted sequence on every iteration. If it does, your insertion is almost certainly correct.

---

## 7. Phase 4 — `remove`: Deletion

Deletion is the deepest part of the exercise. Read §7 of `CONTENT.md` before you start writing — then re-read it. There are two conceptual tricks:

1. **Reduce to leaf deletion.** If the key lives in an internal node, swap it with its **inorder successor** (the leftmost key of the right subtree) and then delete the swapped key from the leaf instead.
2. **Handle underflow from below.** Removing a key from a 2-node leaf leaves the leaf empty. Fix it by either **redistribution** (borrow from a 3-node sibling) or **fusion** (merge with a 2-node sibling, pulling the separator down from the parent). Fusion may cascade upward.

Insertion propagates *overflow* upward via splits; deletion propagates *underflow* upward via fusions. These are duals — holding that duality in your head will keep you oriented.

### 7.1 Recursive skeleton

```java
private RemoveResult<V> delete(Node<K,V> node, K key) {
    // 1. If the key is in this node:
    //    - Leaf: remove it; signal underflow if the leaf is now empty.
    //    - Internal: swap with the inorder successor, recurse into the right child
    //      to remove the successor, then fix underflow if it propagated.
    // 2. Else if this node is a leaf: key not found; no underflow.
    // 3. Else: recurse into the correct child; fix underflow if the child signals it.
}
```

`RemoveResult` carries: the value removed (or `null`), a "did-we-actually-remove" flag (for the `size` counter), and a **`underflow`** flag signalling to the parent that *this* node is now empty.

### 7.2 Finding the inorder successor

For a key at index `idx` of an internal node, the successor is the **leftmost key of the subtree rooted at `children[idx + 1]`**. Walk down `children[0]` from there until you hit a leaf. The successor is that leaf's first key. Swap, then recurse into `children[idx + 1]` to remove the successor's key.

### 7.3 Fixing underflow — `fixUnderflow(parent, emptyChildIdx)`

An empty child means the parent must rearrange itself. Try the cases **in this order**:

1. **Redistribute from a 3-node left sibling**, if one exists.
2. Otherwise, **redistribute from a 3-node right sibling**, if one exists.
3. Otherwise, **fuse** with an adjacent sibling.

**Redistribution from left sibling (sketch):**
```
Before:  [ P ]                After:  [ L2 ]
        /     \                      /      \
   [L1|L2]   [ ]                  [L1]     [ P ]
```
The separator `P` rotates down into the empty node; the sibling's rightmost key `L2` rotates up to replace `P`. For internal nodes, one child pointer also moves across. (The right-sibling case is symmetric.)

**Fusion (sketch):**
```
Before:  [ P ]                After:  [ ] (parent loses one key)
        /     \                         |
      [ ]    [S]                    [ P | S ]
```
Pull `P` down, combine with the non-empty sibling into a single 3-node, and drop the pointer to the empty child from the parent. **If the parent was a 2-node, it is now empty** — propagate underflow upward by returning `underflow = true` from the current call.

### 7.4 Root shrinking

After `delete` returns, check: if `root.numKeys == 0`, replace the root with its single remaining child (`root.children[0]`, which may itself be `null` if the tree is now empty). **This is the only way the tree's height decreases** — the dual of "the tree grows upward on insertion".

### 7.5 Common pitfalls

- **Wrong child index after a fusion.** After fusing children `i` and `i+1` into the child at `i`, the parent's `entries` and `children` arrays must be compacted.
- **Redistribution direction.** The separator that moves is the one **between** the empty child and the sibling you are borrowing from — not an arbitrary separator.
- **Stale child pointers.** After redistribution on an internal node, the moved child pointer must end up on the correct side.
- **Forgetting to propagate underflow.** If the parent became empty via fusion, its parent must fix it in turn.

### Checkpoint 7.1

Trace §8 of `CONTENT.md` through your implementation — especially Example 2, which exercises a full fusion cascade to the root, and Example 3, which exercises internal-key deletion via the inorder successor.

---

## 8. Testing Your Implementation

A suite lives under `src/test/java/com/sebdeveloper6952/TwoThreeTreeTest.java`. It is intentionally layered:

| Section | Purpose |
|---|---|
| Basics | Empty tree, single `put`/`get`, overwrite semantics, `null` handling. |
| Worked examples | Reproduces §6 and §8 of `CONTENT.md`, verifying inorder keys after each. |
| Randomized stress | 5 000 random operations compared against `java.util.TreeMap`. If this passes, your implementation is almost certainly correct. |

Run:

```bash
./gradlew test
```

The stress test is your strongest signal. If it fails, bisect: set a smaller operation count, fix the seed, and print `keysInOrder()` plus the operation that triggered the first divergence.

### Suggested workflow

1. Write `Map.java` and the `Node`/`Entry` types. Compile.
2. Implement `get`; make the "basics" tests pass.
3. Implement `put`; make the worked-example insertion test pass.
4. Implement `remove`; make the deletion tests pass.
5. **Only then** run the randomized stress test. If it fails, you have a specific bug, not a design flaw.

---

## 9. Deliverables and Grading

Submit the contents of `src/main/java/com/sebdeveloper6952/`. You are graded on:

| Area | Weight |
|---|---|
| Correctness (all provided tests pass, including the stress test) | **50%** |
| Complexity — operations observably `O(log n)`; no silent `O(n)` traversals | **15%** |
| Code quality — naming, decomposition, absence of duplicated logic, sensible visibility | **15%** |
| Invariants respected — no persistent 4-nodes, no unbalanced leaves, clean null handling | **10%** |
| Explanation — a short write-up (½ page) describing the three hardest bugs you hit and how you fixed them | **10%** |

You **may not** use `java.util.TreeMap`, `java.util.Map`, or any balanced-tree library in your implementation. You *may* use them in your tests.

---

## 10. Stretch Goals

Once your tests are green, try any of the following. They deepen the ideas and sometimes change your design.

1. **Iterative insertion.** Replace the recursive `insert` with an explicit parent-pointer stack. Confirm complexity is unchanged.
2. **In-order iterator.** Expose `Iterator<K>` that yields keys in ascending order in `O(n)` amortized with `O(log n)` extra space.
3. **Range queries.** Implement `keysBetween(K lo, K hi)` returning all keys in `[lo, hi]`. Aim for `O(log n + k)` where `k` is the output size.
4. **Generalize to a B-tree of order *m*.** The branching factor becomes a constructor parameter; the 2-3 tree is the special case `m = 3`.
5. **Convert to a left-leaning red-black tree.** Using the correspondence in §11 of `CONTENT.md`, reimplement the same `Map` interface on a binary tree with color bits. Compare line counts with your 2-3 implementation.

---

*Good luck. Draw pictures.*

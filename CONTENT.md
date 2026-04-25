# 2-3 Trees

**Course:** Data Structures & Algorithms
**Format:** Two lecture sessions (approx. 90 min each)
**Prerequisites:** Binary search trees, asymptotic analysis, recursion

---

## Table of Contents

**Session 1 — Fundamentals, Search, and Insertion**
1. Motivation
2. Definition and Properties
3. Height Analysis
4. The Search Operation
5. The Insertion Operation
6. Worked Example: Building a 2-3 Tree

**Session 2 — Deletion, Analysis, and Connections**
7. The Deletion Operation
8. Worked Example: Deletion
9. Complexity Analysis Summary
10. Connection to B-Trees
11. Connection to Red-Black Trees
12. Practical Considerations
13. Exercises
14. References

---

# SESSION 1 — Fundamentals, Search, and Insertion

## 1. Motivation

Recall from our study of binary search trees (BSTs) that the three fundamental operations — *search*, *insert*, and *delete* — each run in time proportional to the height of the tree, `h`. In the **best case**, a perfectly balanced BST has `h = ⌊log₂ n⌋`, so these operations take `O(log n)` time. But in the **worst case** — for instance, inserting keys in sorted order `1, 2, 3, ...` — a BST degenerates into a linked list with `h = n − 1`, and every operation costs `O(n)`.

This is a serious problem. A data structure whose asymptotic performance depends on the order in which the user inserts data is fragile. What we want is a tree that **guarantees** `O(log n)` performance no matter what input it receives. This is the central idea of **balanced search trees**.

Several balanced-tree designs exist — AVL trees, red-black trees, B-trees, splay trees, and others. Today we study a particularly elegant one: the **2-3 tree**, introduced by **John Hopcroft in 1970**. Its balance condition is surprisingly simple and its algorithms reveal ideas that generalize directly to B-trees (used in databases and filesystems) and red-black trees (used in most standard library implementations of ordered maps).

---

## 2. Definition and Properties

A **2-3 tree** is a tree in which every internal node is either:

- A **2-node**: contains **one key** and has **two children**, or
- A **3-node**: contains **two keys** and has **three children**.

And which satisfies two global invariants:

- **(Balance)** All leaves are at the **same depth** from the root.
- **(Order)** The keys are arranged so that an inorder traversal yields them in sorted order.

Let us make the order property precise.

### 2.1 Ordering in a 2-node

Let a 2-node contain key `a`, with left child `L` and right child `R`. Then:

```
           [ a ]
          /     \
         L       R
```

- All keys in `L` are **less than** `a`.
- All keys in `R` are **greater than** `a`.

### 2.2 Ordering in a 3-node

Let a 3-node contain keys `a < b`, with left child `L`, middle child `M`, and right child `R`. Then:

```
          [ a | b ]
         /    |    \
        L     M     R
```

- All keys in `L` are **less than** `a`.
- All keys in `M` lie strictly **between** `a` and `b`.
- All keys in `R` are **greater than** `b`.

### 2.3 A note on 4-nodes

During insertion, a node may **temporarily** hold three keys and have four children — we call this a **4-node**. Such nodes are transient; they are always split before the operation completes. No 4-node is ever persistently stored in a 2-3 tree. (Note: if we *do* allow them to persist, we get a **2-3-4 tree**, which is a closely related structure.)

### 2.4 A small example

```
                [ 30 | 60 ]
               /     |     \
          [ 20 ]   [ 40 ]   [ 70 | 90 ]
         /   \    /    \    /    |    \
      [10] [25] [35] [50] [65] [80]  [95|99]
```

Verify: every internal node has 2 or 3 children; all leaves are at depth 3; keys in each subtree respect the ordering constraint.

---

## 3. Height Analysis

Before presenting any algorithm, let us bound the height of a 2-3 tree on `n` keys. This will let us state the complexity of every operation in one shot.

Let `T` be a 2-3 tree of height `h` containing `n` keys. (We define height as the number of edges on the longest root-to-leaf path; a single-node tree has height 0.)

### 3.1 Upper bound on height

**Claim.** `h ≤ log₂(n + 1)`.

**Proof sketch.** In the worst case — i.e., when `h` is as large as possible for a given `n` — every internal node is a 2-node. This makes the tree a perfect binary tree. A perfect binary tree of height `h` has `2^(h+1) − 1` nodes, so `n ≥ 2^(h+1) − 1`, hence `h ≤ log₂(n + 1) − 1`.

### 3.2 Lower bound on height

**Claim.** `h ≥ log₃(n + 1) − 1`.

**Proof sketch.** When `h` is as *small* as possible, every internal node is a 3-node. Each internal node has 3 children and 2 keys. At depth `i` there are `3^i` nodes, each holding 2 keys — so the total number of keys satisfies `n ≤ 2·(3^(h+1) − 1) / (3 − 1) = 3^(h+1) − 1`, giving `h ≥ log₃(n + 1) − 1`.

### 3.3 Conclusion

Combining both bounds:

> **log₃(n+1) − 1 ≤ h ≤ log₂(n+1) − 1**, so **h = Θ(log n)**.

This is the key result: regardless of how keys are inserted, a 2-3 tree's height always grows logarithmically with `n`. Every operation we design will run in `O(h) = O(log n)` time.

---

## 4. The Search Operation

Search generalizes BST search in a natural way: at each node, compare the query key against the one or two keys in the node and descend into the correct subtree.

### 4.1 Pseudocode

```
function SEARCH(node, k):
    if node is null:
        return NOT_FOUND

    if node is a 2-node with key a:
        if k = a:        return node
        else if k < a:   return SEARCH(node.left, k)
        else:            return SEARCH(node.right, k)

    if node is a 3-node with keys a < b:
        if k = a or k = b:   return node
        else if k < a:       return SEARCH(node.left, k)
        else if k < b:       return SEARCH(node.middle, k)
        else:                return SEARCH(node.right, k)
```

### 4.2 Complexity

Each recursive call descends one level, doing `O(1)` work per node (at most two comparisons). Since the tree has height `O(log n)`, **search runs in `O(log n)` time**.

---

## 5. The Insertion Operation

Insertion in a 2-3 tree is more interesting than in a BST. There are two key ideas to keep in mind:

1. **New keys always land in a leaf.** We never create a new internal node "below" an existing one. This is how we preserve the invariant that all leaves are at the same depth.
2. **The tree grows upward, not downward.** When a leaf cannot accommodate a new key, it splits and pushes a key up to its parent. If the parent cannot accommodate the pushed-up key either, it splits too. Occasionally this cascade reaches the root, which splits into a new root — and *this* is the only way the tree's height increases.

### 5.1 The algorithm

**Step 1. Locate the target leaf.** Perform a search for the key `k`. The search will end at a leaf node `L` (we assume `k` is not already present; duplicates are typically disallowed or handled separately).

**Step 2. Insert into the leaf.**

- **Case A — `L` is a 2-node.** `L` has room. Add `k` to `L`, making it a 3-node. Done.

- **Case B — `L` is a 3-node.** `L` is full. Temporarily place `k` in `L`, making it a 4-node containing keys `a < b < c`. Now **split** `L`: promote the middle key `b` to the parent, leaving two 2-nodes `[a]` and `[c]` as its children in place of `L`.

**Step 3. Handle the promotion.** The parent of `L` now receives the promoted key `b`. Three sub-cases:

- **If the parent was a 2-node**, it becomes a 3-node, absorbing `b`. Done.
- **If the parent was a 3-node**, it temporarily becomes a 4-node, must split, and promotes *its* middle key one level up. Recurse.
- **If we promote off the top of the tree**, create a new root containing only the promoted key. The tree grows in height by one.

### 5.2 Pseudocode

```
function INSERT(T, k):
    if T is empty:
        T.root ← new 2-node containing k
        return

    (promoted_key, new_right_child) ← INSERT_RECURSIVE(T.root, k)

    if promoted_key is not null:
        // The root split; grow the tree upward.
        T.root ← new 2-node with key=promoted_key,
                              left=T.root, right=new_right_child


function INSERT_RECURSIVE(node, k):
    // Returns (promoted_key, new_right_child_subtree) if node split,
    // otherwise (null, null).

    if node is a leaf:
        return INSERT_INTO_LEAF(node, k)

    child ← choose correct child of node for k
    (promoted_key, new_right) ← INSERT_RECURSIVE(child, k)

    if promoted_key is null:
        return (null, null)          // nothing to do here
    else:
        return ABSORB_OR_SPLIT(node, promoted_key, new_right)


function INSERT_INTO_LEAF(leaf, k):
    if leaf is a 2-node:
        insert k into leaf in sorted order   // becomes a 3-node
        return (null, null)
    else:   // leaf is a 3-node → overflow
        let a < b < c be the three keys (existing two plus k)
        leaf.keys ← [a]                      // leaf becomes a 2-node [a]
        new_node ← new 2-node with key c
        return (b, new_node)                 // promote middle


function ABSORB_OR_SPLIT(node, promoted_key, new_right):
    if node is a 2-node:
        // Absorb: node becomes a 3-node.
        insert promoted_key into node in sorted order
        attach new_right as the appropriate child
        return (null, null)
    else:
        // node is a 3-node → becomes temporary 4-node and splits.
        let a < b < c be (node.keys ∪ {promoted_key}) in sorted order
        split node into two 2-nodes [a] and [c] with the four
            children redistributed so ordering is preserved
        return (b, [c])
```

### 5.3 Complexity

The recursion descends to a leaf (`O(log n)` steps) and, in the worst case, performs a split at every level on the way back up. Each split is `O(1)` work. Total: **`O(log n)`**.

---

## 6. Worked Example: Building a 2-3 Tree

Let us insert the sequence **10, 20, 30, 40, 50, 60, 70, 80** into an initially empty tree.

### Step 1 — Insert 10
```
[ 10 ]
```

### Step 2 — Insert 20
The root is a 2-node with room; it becomes a 3-node.
```
[ 10 | 20 ]
```

### Step 3 — Insert 30
The root is a 3-node; adding 30 creates a temporary 4-node `[10 | 20 | 30]`. Split: middle key 20 becomes the new root, with 10 and 30 as children. **The tree grows upward.**
```
      [ 20 ]
      /    \
   [10]   [30]
```

### Step 4 — Insert 40
Search descends to the right leaf `[30]`. It has room and becomes `[30 | 40]`.
```
      [ 20 ]
      /    \
   [10]  [30|40]
```

### Step 5 — Insert 50
Search descends to `[30 | 40]`, which is a 3-node. Temporary 4-node `[30 | 40 | 50]`. Split: promote 40 to parent.

The parent is `[20]`, a 2-node with room, so it absorbs 40 and becomes `[20 | 40]`.
```
      [ 20 | 40 ]
      /    |    \
   [10] [30]  [50]
```

### Step 6 — Insert 60
Search descends to `[50]`. Room — becomes `[50 | 60]`.
```
      [ 20 | 40 ]
      /    |    \
   [10] [30]  [50|60]
```

### Step 7 — Insert 70
Search descends to `[50 | 60]`, a 3-node. Temporary 4-node `[50 | 60 | 70]`. Split: promote 60.

The parent `[20 | 40]` is itself a 3-node. Adding 60 creates a temporary 4-node `[20 | 40 | 60]`. Split: promote 40, leaving 2-nodes `[20]` and `[60]`. The children are redistributed:
- `[20]` takes the original leftmost two children: `[10]` and `[30]`.
- `[60]` takes the two remaining: `[50]` and `[70]`.

Since we promoted off the top of the tree, a new root `[40]` is created. **Tree grows upward again.**
```
           [ 40 ]
          /      \
       [20]      [60]
       /  \      /   \
    [10] [30] [50] [70]
```

### Step 8 — Insert 80
Search descends to `[70]`. Room — becomes `[70 | 80]`.
```
           [ 40 ]
          /      \
       [20]      [60]
       /  \      /    \
    [10] [30] [50] [70|80]
```

Notice how the tree stayed **perfectly balanced** throughout: every leaf is at the same depth. This is the defining property of the 2-3 tree.

---

# SESSION 2 — Deletion, Analysis, and Connections

## 7. The Deletion Operation

Deletion is, admittedly, the trickiest operation on a 2-3 tree. But it breaks down cleanly once we isolate two ideas:

1. **Reduce to leaf deletion.** If the key to delete is in an internal node, swap it with its inorder successor (or predecessor), which always lives in a leaf. Then the actual removal happens at a leaf.
2. **Handle underflow by redistribution or fusion.** Removing a key from a leaf may leave the leaf empty. An empty node is not allowed (except transiently), so we must either *borrow* a key from a sibling via the parent (redistribution) or *merge* with a sibling (fusion), which may propagate underflow upward.

Insertion propagates *overflow* upward via splits; deletion propagates *underflow* upward via fusions. The two operations are beautifully dual.

### 7.1 Step 1 — Reduce to leaf deletion

Let `k` be the key to delete.

- Search for `k`. If not found, return.
- If `k` is in an **internal node** `v`:
  - Find the inorder successor of `k`. In a 2-3 tree, this is the leftmost key of the appropriate subtree — just keep following left children from the child immediately to the right of `k`. The successor always lives in a **leaf**.
  - **Swap** `k` with its successor. Now `k` is in a leaf and the tree's ordering is temporarily violated in exactly the way we are about to fix by removing `k`.
- Delete `k` from the leaf.

### 7.2 Step 2 — Delete from a leaf

Let `L` be the leaf containing `k`.

- **Case A — `L` is a 3-node.** Simply remove `k`; `L` becomes a 2-node. Done.

- **Case B — `L` is a 2-node.** Removing `k` leaves `L` empty. This is an **underflow** that we must resolve.

### 7.3 Step 3 — Resolving underflow

Let `U` be an underflowed (empty) node, and let `P` be its parent. We look at `U`'s **immediate siblings** (the siblings sharing `P`).

#### Case B1 — A sibling is a 3-node (REDISTRIBUTION)

If any immediate sibling `S` of `U` is a 3-node, we can redistribute keys *through the parent* to fix the underflow:

- Rotate the separating key from `P` down into `U`.
- Rotate the adjacent key from `S` up into the slot vacated in `P`.
- If `U` and `S` have children, adjust one child pointer accordingly.

After redistribution, `U` contains one key (is a healthy 2-node), `S` is now a 2-node, and `P` is unchanged in size. **No further propagation is needed.**

```
Before:   [ p ]               After:    [ s₁ ]
         /     \                        /     \
      [ ]     [s₁|s₂]                 [ p ]   [s₂]
```
(Here `U` was the left child; the mirror case for right child is symmetric. The middle-child case in a 3-node parent is similar — borrow from left or right sibling through the adjacent separator in the parent.)

#### Case B2 — All immediate siblings are 2-nodes (FUSION)

If no sibling can spare a key, we must **merge** (fuse) `U` with an adjacent sibling `S`, pulling the separating key down from `P`:

- Pull the separating key from `P` down.
- Combine it with `S`'s key to form a single 3-node.
- Remove the pointer to `U` from `P` (since `U` has been absorbed).

Now `P` has lost one key. If `P` was a 3-node, it becomes a 2-node and we're done. **If `P` was a 2-node, it is now empty — underflow has propagated.** Recurse on `P`.

```
Before:   [ p ]               After:   [ pulled down ]
         /     \                              |
      [ ]     [s₁]                         [ p | s₁ ]
                              (P loses one key and one child pointer)
```

#### Case B3 — Underflow reaches the root

If the underflow propagates all the way up and the root becomes empty, simply **discard** the empty root and make its single remaining child the new root. **The tree shrinks in height by one.** This is the only way the height can decrease — the dual of "the tree grows upward during insertion."

### 7.4 Pseudocode

```
function DELETE(T, k):
    node ← SEARCH(T.root, k)
    if node is null: return    // k not in tree

    if node is an internal node:
        succ_leaf ← find leaf containing inorder successor of k
        swap k with its successor in succ_leaf
        node ← succ_leaf

    DELETE_FROM_LEAF(node, k)

    if T.root is empty:
        T.root ← T.root's only remaining child   // may be null
        // Tree height decreased by one.


function DELETE_FROM_LEAF(leaf, k):
    if leaf is a 3-node:
        remove k from leaf        // leaf becomes a 2-node
        return
    else:
        remove k from leaf        // leaf is now empty
        FIX_UNDERFLOW(leaf)


function FIX_UNDERFLOW(U):
    if U is the root:
        return   // handled by caller

    P ← parent(U)
    S ← an immediate sibling of U

    if any immediate sibling S of U is a 3-node:
        REDISTRIBUTE(U, S, P)     // Case B1
    else:
        FUSE(U, S, P)             // Case B2
        if P is now empty:
            FIX_UNDERFLOW(P)      // propagate up
```

### 7.5 Complexity

Searching for `k`: `O(log n)`. Finding the inorder successor: `O(log n)`. Propagating underflow up through fusions: at most `O(log n)` fusions, each `O(1)`. Total: **`O(log n)`**.

---

## 8. Worked Example: Deletion

Start with the tree we built in Section 6:

```
           [ 40 ]
          /      \
       [20]      [60]
       /  \      /    \
    [10] [30] [50] [70|80]
```

### Example 1 — Delete 80 (easy case)

Key 80 lives in the leaf `[70 | 80]`, a 3-node. Simply remove it.

```
           [ 40 ]
          /      \
       [20]      [60]
       /  \      /    \
    [10] [30] [50]  [70]
```

### Example 2 — Delete 70 (redistribution)

Key 70 is in the leaf `[70]`, a 2-node. Removing it creates underflow. Look at 70's siblings: the only one is `[50]`, also a 2-node. No redistribution possible *from the sibling alone*. But note the parent `[60]` is a 2-node with two children; we need to look elsewhere.

Actually, in this configuration we must fuse `[70]` (empty) with its sibling `[50]`, pulling the separator `60` from the parent:

```
           [ 40 ]
          /      \
       [20]     [ ]        ← P is now empty!
       /  \       |
    [10] [30]  [50|60]
```

Parent `[60]` became empty (it was a 2-node; it lost its only key). Underflow propagates to this node. Its sibling is `[20]`, also a 2-node — again no redistribution. Fuse with `[20]`, pulling down separator `40`:

```
        [ ]              ← root is now empty!
         |
     [20|40]
     /  |  \
   [10][30][50|60]
```

The root is empty. Discard it and make its only child the new root. **Tree height decreases by one:**

```
     [ 20 | 40 ]
     /    |    \
  [10]  [30]  [50|60]
```

### Example 3 — Delete an internal key

Take the original tree again and delete `40` (the root's key).

Since 40 is internal, find its inorder successor. Descend to the right subtree (rooted at `[60]`), then keep going left: `[60]` → `[50]`. So the successor is 50.

Swap 40 and 50:

```
           [ 50 ]
          /      \
       [20]      [60]
       /  \      /    \
    [10] [30] [40]  [70|80]
```

Now delete 40 from its new leaf location. Leaf `[40]` is a 2-node → underflow. Its sibling `[70|80]` is a 3-node → **redistribution!** Rotate 60 (the separator in parent) down into the underflow position, and rotate 70 up from the sibling:

```
           [ 50 ]
          /      \
       [20]      [70]
       /  \      /    \
    [10] [30] [60]   [80]
```

Done. Three operations, all `O(log n)`.

---

## 9. Complexity Analysis Summary

| Operation | Time Complexity | Space Complexity |
|-----------|----------------|------------------|
| Search    | `Θ(log n)`     | `O(log n)` (recursion stack) or `O(1)` (iterative) |
| Insert    | `Θ(log n)`     | `O(log n)` |
| Delete    | `Θ(log n)`     | `O(log n)` |

The `Θ` bound (not merely `O`) is justified because, in the worst case, an operation may traverse a full root-to-leaf path and perform `O(1)` work at each level.

**Space complexity of the data structure itself:** `Θ(n)` — each key is stored exactly once.

---

## 10. Connection to B-Trees

A 2-3 tree is precisely a **B-tree of order 3**. B-trees generalize the same idea to arbitrary branching factor `m`, allowing each internal node to hold between `⌈m/2⌉ − 1` and `m − 1` keys, and between `⌈m/2⌉` and `m` children.

Why would we want high branching? In **external storage** — disks, SSDs — the cost of accessing a node dominates the cost of processing it. If we can pack 100 keys into a single node that fits in one disk block, the height of the tree is `log₁₀₀(n)` instead of `log₂(n)`, drastically reducing the number of disk I/Os. This is why B-trees (and their variant **B+ trees**) are the backbone of virtually every database index and filesystem (Ext4, NTFS, PostgreSQL, MySQL, SQLite).

The 2-3 tree is the smallest nontrivial B-tree and therefore the cleanest setting in which to learn the split-and-propagate pattern before scaling up.

---

## 11. Connection to Red-Black Trees

This is arguably the most important pedagogical payoff of studying 2-3 trees. Red-black trees — the balanced-tree implementation used in `std::map` (C++), `TreeMap` (Java), and countless other libraries — are *notoriously* hard to motivate on their own. The color rules seem arbitrary. The rotation cases feel ad hoc.

**But:** a red-black tree is nothing more than a **binary representation of a 2-3 tree** (or a 2-3-4 tree, depending on the variant).

### 11.1 The encoding

Represent each 3-node `[a | b]` in a 2-3 tree as *two* binary nodes connected by a special "red" edge:

```
2-3 tree 3-node:                     Red-black representation:

   [ a | b ]                              b (black)
   /   |   \                             / \
  L    M    R                  (red) a    R
                                    / \
                                   L   M
```

A 2-node in the 2-3 tree simply maps to a single black node in the red-black tree. Thus every 2-3 tree has a canonical red-black representation, and every red-black tree can be "squashed" back into a 2-3 tree by collapsing each red edge.

### 11.2 Where the RB rules come from

All the mysterious rules of red-black trees now have a clear origin:

- **"No two consecutive red edges"** — because a 2-3 tree has no 4-nodes; three keys at one level would require two red edges in a row.
- **"All root-to-leaf paths have the same number of black nodes"** — because all leaves in a 2-3 tree are at the same depth, and only black edges contribute to depth in the 2-3 view.
- **Rotations and color flips** — are precisely the binary-tree moves that correspond to splits and merges in the underlying 2-3 tree.

This correspondence was formalized by Guibas and Sedgewick in their 1978 paper *"A Dichromatic Framework for Balanced Trees"*. Sedgewick later refined it into the **left-leaning red-black tree** (2008), which enforces a strict 1-to-1 correspondence with 2-3 trees and whose insertion algorithm can be written in roughly 30 lines of code.

**Takeaway for students:** If you ever need to implement or debug a red-black tree, mentally translate it back to its 2-3 tree view. Everything becomes clear.

---

## 12. Practical Considerations

### 12.1 When to use a 2-3 tree directly

Honestly, rarely. In practice, one usually prefers:

- **Red-black trees** — equivalent guarantees, but a binary implementation fits more naturally on modern memory hierarchies (single pointer size per child, uniform node layout, cache-friendly).
- **B-trees / B+ trees** — when data lives on disk or when branching factor can be tuned to a block size.
- **AVL trees** — when you need slightly faster lookups at the cost of slightly slower insertions/deletions (AVL trees are more strictly balanced than red-black trees).

The 2-3 tree's great virtue is **pedagogical**: every idea in B-trees and red-black trees appears in its purest form here.

### 12.2 Comparison of balanced BSTs

| Structure       | Height bound          | Implementation complexity | Typical use |
|-----------------|----------------------|---------------------------|-------------|
| AVL tree        | `≤ 1.44 log₂(n+2)`   | Moderate (binary + balance factor) | Read-heavy workloads |
| Red-black tree  | `≤ 2 log₂(n+1)`      | Moderate (binary + color bit) | General-purpose ordered maps |
| 2-3 tree        | `≤ log₂(n+1)`        | Higher (mixed node types) | Teaching, academic reference |
| 2-3-4 tree      | `≤ log₂(n+1)`        | Higher (three node types) | Teaching, red-black derivation |
| B-tree (order m)| `≤ log_{⌈m/2⌉}(n)`   | Higher (variable-size nodes) | Disk-based storage |

---

## 13. Exercises

**Conceptual**

1. Prove that in any 2-3 tree with `n` keys, the number of internal nodes is at least `⌈n/2⌉` and at most `n`.
2. Explain why deletion can only decrease the height of a 2-3 tree, and only when the root becomes empty. Why can't the height decrease in any other way?
3. In a 2-3 tree, can two sibling nodes have different "types" (one 2-node, one 3-node)? Give an example or prove it is impossible.

**Operational**

4. Starting from an empty 2-3 tree, insert the following keys in order: `5, 10, 15, 20, 25, 30, 35, 40, 45, 50`. Draw the tree after each insertion.
5. From the final tree in exercise 4, delete in order: `20, 35, 50, 10`. Draw the tree after each deletion.
6. Construct a 2-3 tree containing exactly 7 keys such that:
   - (a) Its height is minimized.
   - (b) Its height is maximized.
   What are those heights?

**Deeper**

7. Modify the 2-3 tree definition to disallow 3-nodes (i.e., every node must be a 2-node). What structure do you get? What is its worst-case height in terms of `n`?
8. Design an algorithm that, given two 2-3 trees `T₁` and `T₂` where every key in `T₁` is less than every key in `T₂`, produces their **concatenation** in `O(log n)` time. (Hint: consider their heights.)
9. Convert the small 2-3 tree from Section 2.4 into its corresponding red-black tree, then verify that all red-black properties hold.

---

## 14. References

The definitive academic sources for 2-3 trees are:

1. **Knuth, D. E.** (1998). *The Art of Computer Programming, Volume 3: Sorting and Searching* (2nd ed.). Addison-Wesley. — See Sections 6.2.3 (Balanced Trees) and 6.2.4 (Multiway Trees). The most complete and rigorous treatment, including historical notes and detailed analyses of variants.

2. **Aho, A. V., Hopcroft, J. E., & Ullman, J. D.** (1983). *Data Structures and Algorithms*. Addison-Wesley. — Hopcroft himself is a co-author, making this essentially a primary source for 2-3 trees. Chapter 5 ("Advanced Set Representation Methods") covers them in detail.

3. **Cormen, T. H., Leiserson, C. E., Rivest, R. L., & Stein, C.** (2022). *Introduction to Algorithms* (4th ed.). MIT Press. — CLRS treats balanced trees via red-black trees (Chapter 13) and B-trees (Chapter 18); the 2-3 tree appears implicitly as the pedagogical motivation for both.

4. **Sedgewick, R., & Wayne, K.** (2011). *Algorithms* (4th ed.). Addison-Wesley. — Section 3.3 presents the cleanest modern pedagogical treatment, explicitly building red-black BSTs from 2-3 trees via the left-leaning correspondence.

5. **Goodrich, M. T., Tamassia, R., & Goldwasser, M. H.** (2014). *Data Structures and Algorithms in Java* (6th ed.). Wiley. — Covers (2,4) trees with very similar reasoning and clear diagrams.

### Historical and seminal papers

6. **Guibas, L. J., & Sedgewick, R.** (1978). "A Dichromatic Framework for Balanced Trees". *Proceedings of the 19th Annual Symposium on Foundations of Computer Science* (FOCS), pp. 8–21. — The seminal paper connecting 2-3 (and 2-3-4) trees to red-black trees.

7. **Bayer, R., & McCreight, E.** (1972). "Organization and Maintenance of Large Ordered Indexes". *Acta Informatica*, 1(3), 173–189. — The original B-tree paper; the 2-3 tree is a special case (order 3).

8. **Sedgewick, R.** (2008). "Left-leaning Red-Black Trees". Technical report, Princeton University. — Modern simplification that preserves an exact 1-1 correspondence with 2-3 trees.

### Online visualization tools (recommended for classroom use)

- **David Galles' B-Tree Visualizer** (University of San Francisco) — with "Max. Degree = 3" setting it becomes a 2-3 tree simulator: `https://www.cs.usfca.edu/~galles/visualization/BTree.html`
- **OpenDSA Data Structures and Algorithms eTextbook** — interactive 2-3 tree visualizations: `https://opendsa-server.cs.vt.edu/ODSA/Books/CS3/html/TwoThreeTree.html`

---

*End of lecture notes.*

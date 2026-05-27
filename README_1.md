# Multithreaded K-Means Clustering in Java

A Java implementation of the K-Means clustering algorithm accelerated with multi-threading, featuring a Swing-based visualizer for both raw data points and clustered results.

---

## What is K-Means?

K-Means is an unsupervised machine learning algorithm that partitions a dataset into **K distinct clusters**. The goal is to group data points so that points within the same cluster are as close to each other as possible, while points in different clusters are as far apart as possible

### How It Works

The algorithm follows an iterative loop of two steps:

**1. Assignment Step**
Each data point is assigned to the nearest centroid. "Nearest" is measured by Euclidean distance:

```
distance(p1, p2) = √((x₁ - x₂)² + (y₁ - y₂)²)
```

**2. Update Step**
Each centroid is recalculated as the mean (average) of all points currently assigned to it:

```
centroid = (Σxᵢ / n,  Σyᵢ / n)
```

These two steps repeat until the centroids stop moving — that is, the total shift between old and new centroids falls below a small convergence threshold. At that point, the algorithm has found a stable partitioning of the data.

### Initialization

Before the loop starts, K initial centroids must be chosen. This implementation uses **random initialization**: K data points are picked at random from the dataset and used as the starting centroids. While simple, this means results can vary between runs depending on the initial seed.

### Convergence

The algorithm is considered converged when:

```
Σ distance(oldCentroid[i], newCentroid[i]) < threshold
```

In this project the threshold is set to `0.001`, which means the algorithm stops once all centroids have collectively moved less than one-thousandth of a unit.

---

## Project Structure

```
src/main/java/org/example/
├── Main.java               # Entry point — runs K-Means and controls the main loop
├── ClusterWorker.java      # Runnable task — assigns points to centroids in parallel
├── ClusterVisualizer.java  # Swing panel — visualizes clustered results with coloring
└── PointVisualizer.java    # Swing panel — visualizes raw (pre-clustering) data points
```

---

## Thread Safety: Lock-Free Design

The assignment step — checking which centroid is closest for each of the 1,000 data points — is the most computationally expensive part of the algorithm. This project parallelizes it across all available CPU cores.

### The Concurrency Strategy

The dataset is divided into equal-sized chunks, one per thread. Each `ClusterWorker` thread is responsible for a contiguous slice `[start, end)` of the `data` array:

```java
int chunk_size = totalPoints / threadCount;

for (int i = 0; i < threadCount; i++) {
    int start = i * chunk_size;
    int end = (i == threadCount - 1) ? totalPoints : (i + 1) * chunk_size;

    ClusterWorker worker = new ClusterWorker(data, oldCentroids, labels, start, end);
    threads[i] = new Thread(worker);
    threads[i].start();
}
```

The last chunk absorbs any remainder when `totalPoints` is not evenly divisible by `threadCount`.

### Why No Locks Are Needed

The key design decision is **partition-based isolation**: each thread writes to a strictly non-overlapping range of the shared `labels[]` array.

```java
// Thread i only ever writes to labels[start] through labels[end - 1]
for (int i = start; i < end; i++) {
    labels[i] = findClosestCentroid(data[i]);  // writes to its own slice only
}
```

Because no two threads ever write to the same index, there is no write-write conflict. The `data[]` and `centroids[]` arrays are read-only during the assignment phase, so they also require no synchronization.

This means:

| Resource | Access Pattern | Synchronization Needed? |
|---|---|---|
| `data[]` | All threads read, none write | ✗ No |
| `centroids[]` | All threads read, none write | ✗ No |
| `labels[]` | Each thread writes its own slice | ✗ No (partitioned) |

No `synchronized` blocks, no `ReentrantLock`, no `AtomicInteger` — zero locking overhead.

### Barrier Synchronization with `join()`

Although the assignment step is lock-free, the **update step** (recalculating centroids) must not start until every thread has finished its assignments. This is enforced with `Thread.join()`:

```java
for (Thread t : threads) {
    t.join();  // main thread waits here until all workers complete
}

// Safe to proceed: labels[] is fully populated
double[][] newCentroids = updateCentroids(data, labels, k);
```

`join()` acts as a **barrier** — it guarantees a happens-before relationship between each worker's writes to `labels[]` and the main thread's subsequent read of the same array. This is the only synchronization primitive needed in the entire algorithm.

### Why This Works Correctly

The correctness of the approach rests on three properties:

1. **Disjoint write sets** — each thread owns its slice of `labels[]`, so no two threads race on the same memory location.
2. **Read-only shared data** — `data[]` and `centroids[]` are never modified during parallel execution, so reads are always consistent.
3. **`join()` as a memory barrier** — Java's memory model guarantees that after `t.join()` returns, all writes made by thread `t` are visible to the calling thread. The centroid update therefore always sees a fully-written `labels[]`.

---

## Running the Project

**Prerequisites:** Java 11+, a JDK with Swing support.

```bash
# Compile
javac -d out src/main/java/org/example/*.java

# Run
java -cp out org.example.Main
```

The console will print centroid positions and total shift at each iteration. A Swing window will display the clustered points once convergence is reached.

### Configuration (in `Main.java`)

| Parameter | Default | Description |
|---|---|---|
| `totalPoints` | `1000` | Number of random data points |
| `k` | `3` | Number of clusters |
| `convergeThreshold` | `0.001` | Minimum total centroid shift to continue |
| `threadCount` | CPU core count | Number of parallel worker threads |

---

## Visualization

Two visualizers are included:

**`PointVisualizer`** — shows the raw, unlabeled data points before clustering begins.

**`ClusterVisualizer`** — shows the final clustered result, with each cluster rendered in a different color and centroids marked as black squares.

Both panels include labeled X/Y axes scaled to the data range (0–100).

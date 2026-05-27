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
src/main/java/
├── org/example/
│   ├── Main.java               # Entry point — runs the parallel K-Means and controls the main loop
│   ├── ClusterWorker.java      # Runnable task — assigns points to centroids in parallel
│   ├── ClusterVisualizer.java  # Swing panel — visualizes clustered results with coloring
│   └── PointVisualizer.java    # Swing panel — visualizes raw (pre-clustering) data points
└── Lock_Implemetation/
    ├── Cluster_LockVersion.java # Two-phase ping-pong K-Means using ReentrantLock + Condition
    └── Driver_LockVersion.java  # Entry point for the lock-based teaching version
```

The project ships **two independent implementations** that demonstrate different aspects of concurrent programming:

| Version | Goal | Concurrency Primitive |
|---|---|---|
| `org.example.Main` | **Performance** — real data parallelism | `Thread` + `join()` barrier (lock-free) |
| `Lock_Implemetation` | **Coordination** — explicit synchronization showcase | `ReentrantLock` + `Condition` |

The two versions are intentionally complementary: the first shows *where* parallelism is profitable in K-Means, the second shows *how* synchronization primitives coordinate phase-dependent threads.

---

## Thread Safety: Lock-Free Design (Performance Version)

The assignment step — checking which centroid is closest for each of the 1,000 data points — is the most computationally expensive part of the algorithm. This project parallelizes it across all available CPU cores.

### Why `threadCount = availableProcessors()`

In `Main.java` the worker thread count is set to the number of physical/logical CPU cores reported by the JVM:

```java
int cpuCores = Runtime.getRuntime().availableProcessors();
int threadCount = cpuCores;
```

This is a deliberate design choice rather than an arbitrary number:

1. **One worker per core saturates the CPU.** Since the assignment step is purely CPU-bound (no I/O, no blocking), a thread can keep a core busy 100% of the time. Spawning *exactly* one thread per core extracts the maximum possible parallel throughput.
2. **Avoids context-switching overhead.** Creating more threads than cores forces the OS scheduler to time-slice them onto the same physical cores, which incurs context switches, cache invalidation, and TLB flushes. With `threadCount == cpuCores`, every thread can ideally run on its own core for the entire assignment phase, eliminating most of this overhead.
3. **Avoids under-utilization.** Using fewer threads than cores would leave hardware idle and cap the achievable speedup below what the machine can deliver.
4. **Portable across machines.** The value is queried at runtime, so the program automatically scales: a 4-core laptop runs 4 workers, a 16-core workstation runs 16, with no code change required.

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

## Lock-Based Implementation (Teaching / Coordination Version)

The `Lock_Implemetation` package contains an alternative version of K-Means built around `ReentrantLock` and `Condition`. **It is not intended to outperform the lock-free version** — in fact, it runs slightly slower than a single-threaded baseline. Its purpose is to demonstrate how explicit synchronization primitives can coordinate two threads that have a strict phase dependency.

### Design: Two-Phase Ping-Pong

Two dedicated threads cooperate on one shared `Cluster_LockVersion` instance:

| Thread | Responsibility |
|---|---|
| `AssignPoints` | Runs the assignment step — labels every data point with its nearest centroid |
| `updateCentroids` | Runs the update step — recomputes each centroid as the mean of its assigned points |

Because K-Means imposes a strict ordering (`assign → update → assign → ...`), the two threads must execute *alternately*, never simultaneously. This is enforced by a single `ReentrantLock`, a single `Condition`, and an explicit phase variable:

```java
private enum Phase { ASSIGN, UPDATE }
private Phase phase = Phase.UPDATE;   // initial phase decides who runs first
```

Each thread, on entering its method, checks whether it is currently its turn. If not, it calls `condition.await()` and releases the lock. When the other thread finishes its phase, it flips the `phase` variable and calls `signalAll()`, waking up its counterpart.

```java
// Inside UpdateCentroids(...)
while (notConverged && phase != Phase.UPDATE) {
    condition.await();         // release the lock and sleep until signalled
}
// ... do the update work ...
phase = Phase.ASSIGN;          // hand control to the assign thread
condition.signalAll();
```

The symmetric block lives in `assignCluster()`. Together they form a deterministic ping-pong: `update → assign → update → assign → ...`

### Choosing Who Goes First

The starting phase is controlled entirely by the initial value of the `phase` field. Setting it to `Phase.UPDATE` (the current default) makes the update thread run first; setting it to `Phase.ASSIGN` would reverse the order. **No `Thread.sleep()` and no thread-start ordering tricks are needed** — correctness comes from the synchronization logic itself, not from timing assumptions.

### Why `while` and Not `if` Around `await()`

The two waiting blocks use `while` rather than `if`:

```java
while (notConverged && phase != Phase.UPDATE) {
    condition.await();
}
```

This is the textbook-correct pattern for two reasons:

1. **Spurious wakeups.** Java's `Condition.await()` is permitted to return without a corresponding `signal()` (a quirk of the underlying OS primitives). A `while` loop forces the thread to re-check the predicate after every wakeup, preventing it from proceeding when the condition is not actually satisfied.
2. **`signalAll()` wakes both threads.** Since the two threads share one `Condition`, every `signalAll()` rouses both of them. The loop ensures that the wrong-turn thread immediately goes back to sleep, preserving strict alternation.

### Why This Version Does Not Aim For Speedup

The two threads can never run in parallel — the lock guarantees mutual exclusion, and the phase variable enforces ordering. At any instant, at most one thread is doing useful work; the other is blocked on `await()`. This is a deliberate property of the design: K-Means' two phases are inherently sequential at the *phase-pair* level (you cannot update centroids before assignments are finished, nor reassign before centroids are updated), so partitioning work *across phases* offers no speedup.

Real parallelism for K-Means must come from partitioning *within* a phase — which is exactly what the lock-free `Main.java` version does. The lock-based version is therefore best understood as a **synchronization-primitive case study**: a clean, correct demonstration of how `ReentrantLock` + `Condition` + a phase variable solve the classical "strictly alternating threads" problem.

### Running the Lock Version

```bash
# Compile
javac -d out src/main/java/Lock_Implemetation/*.java

# Run
java -cp out Lock_Implemetation.Driver_LockVersion
```

The program prompts for the number of data points and the number of centroids, then runs until convergence and prints each iteration.

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
| `threadCount` | `Runtime.getRuntime().availableProcessors()` | One worker per CPU core to maximize throughput and minimize context switches |

---

## Visualization

Two visualizers are included:

**`PointVisualizer`** — shows the raw, unlabeled data points before clustering begins.

**`ClusterVisualizer`** — shows the final clustered result, with each cluster rendered in a different color and centroids marked as black squares.

Both panels include labeled X/Y axes scaled to the data range (0–100).

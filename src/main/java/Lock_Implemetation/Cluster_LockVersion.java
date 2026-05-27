package Lock_Implemetation;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Cluster_LockVersion {
// Phase state: decides which thread is allowed to run in the current turn
private enum Phase { ASSIGN, UPDATE }

private final double[][] data;
private double convergeThreshold; // users better to set convergeThreshold less than 0.01
private final double[][] centroids;
private int[] labels;
private boolean notConverged = true;   // true = not converged yet, keep iterating
private Phase phase = Phase.UPDATE;    // initial phase decides who runs first; UPDATE goes first here
private int iterations;
// lock & condition for two-phase ping-pong synchronization
private final Lock lock = new ReentrantLock();
private final Condition condition = lock.newCondition();


public Cluster_LockVersion(double[][] data, double[][] centroids, double convergeThreshold){
    this.data = data;
    this.centroids = centroids;
    this.convergeThreshold = convergeThreshold;
    this.labels = new int[data.length];
}


// update centroid based on average x y among the cluster
public void  UpdateCentroids(int k) {
    lock.lock();
    try {
        while (notConverged) {
            // Wait while it is not this thread's turn (while-loop guards against spurious wakeups)
            while (notConverged && phase != Phase.UPDATE) {
                condition.await();
            }
            if (!notConverged) break;

            iterations++;

            int[] count = new int[k];
            double[][] sum = new double[k][2];

            for (int i = 0; i < data.length; i++) {
                int c = labels[i];
                count[c]++;
                sum[c][0] += data[i][0];
                sum[c][1] += data[i][1];
            }

            double[][] newCentroids = new double[k][2];

            for (int i = 0; i < k; i++) {
                // avoid zero points cluster happen
                if (count[i] == 0) {
                    newCentroids[i] = centroids[i].clone();
                    continue;
                }
                newCentroids[i][0] = sum[i][0] / count[i];
                newCentroids[i][1] = sum[i][1] / count[i];
            }

            // Per-iteration log output
            System.out.println("This is the " + iterations+ " iterations");
            System.out.println("==================================");
            for (int i = 0; i < k; i++) {
                System.out.printf("Centroid %d: (%.2f, %.2f)%n", (i+1), newCentroids[i][0], newCentroids[i][1]);
            }
            System.out.printf("Total shift: %.4f | Threshold: %.3f%n", calculateTotalDiff(newCentroids, centroids), convergeThreshold);
            System.out.println("==================================\n");

            boolean converged = CheckConverge(newCentroids, centroids, convergeThreshold);
            this.notConverged = !converged;

            System.arraycopy(newCentroids, 0, centroids, 0, k);

            // Hand off to the assign phase
            phase = Phase.ASSIGN;
            condition.signalAll();
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    } finally {
        lock.unlock();
    }
}


//assign points to the cluster
public void assignCluster(){
    lock.lock();
    try {
        while (notConverged) {
            // Wait while it is not this thread's turn (while-loop guards against spurious wakeups)
            while (notConverged && phase != Phase.ASSIGN) {
                condition.await();
            }
            if (!notConverged) break;

            for (int i = 0; i < data.length; i++) {
                double[] point = data[i];
                int closest = 0;
                double min_value = Double.MAX_VALUE;

                for (int j = 0; j < centroids.length; j++) {
                    double dis = euclideanDistance(point, centroids[j]);
                    if (dis < min_value) {
                        min_value = dis;
                        closest = j;
                    }
                }
                labels[i] = closest;
            }

            // Hand off to the update phase
            phase = Phase.UPDATE;
            condition.signalAll();
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    } finally {
        lock.unlock();
    }
}


public boolean  CheckConverge(double[][] newCentroid,double[][] oldCentroids,double convergeThreshold){
    double diff = 0;
   for(int i=0;i< newCentroid.length;i++){
      double x =  newCentroid[i][0] - oldCentroids[i][0];
      double y = newCentroid[i][1] - oldCentroids[i][1];
      diff += Math.sqrt(x*x+y*y);
   }

   if (diff< convergeThreshold){
       return true;
   }else {
       return false;
   }

}


public double euclideanDistance(double[] p1, double[] p2){
    double dx = p1[0] - p2[0];
    double dy = p1[1] - p2[1];
    return Math.sqrt(dx*dx+dy*dy);
}


private double calculateTotalDiff(double[][] newCentroid, double[][] oldCentroids){
        double totalDiff = 0;
        for(int i=0;i< newCentroid.length;i++){
            double x = newCentroid[i][0] - oldCentroids[i][0];
            double y = newCentroid[i][1] - oldCentroids[i][1];
            totalDiff += Math.sqrt(x*x + y*y);
        }
        return totalDiff;
    }
}



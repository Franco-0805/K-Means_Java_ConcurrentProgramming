package org.example;

//TIP 要<b>运行</b>代码，请按 <shortcut actionId="Run"/> 或
// 点击装订区域中的 <icon src="AllIcons.Actions.Execute"/> 图标。

import java.util.Random;

// this main class used to implement the all functionalities and analysis
public class Main {
    public static void main(String[] args) throws InterruptedException {

 // create the threads array
    int cupCores = Runtime.getRuntime().availableProcessors();
     int threadCount = cupCores;
     Thread[] threads = new Thread[threadCount];

//configure
    int totalPoints = 1000;
    int k = 3; // number of initial centroids
    double convergeThreshold = 0.001;


//generating random data points
double[][] data = new double[totalPoints][2];
Random random  = new Random();
for (int i = 0; i < totalPoints; i++) {
    data[i][0] = random.nextDouble() * 100;
    data[i][1] = random.nextDouble() * 100;
        }


//initialize centroids
 double[][] oldCentroids = initializeCentroids(data,k);


boolean converged = false;
int iteration = 0;

while(!converged){
    iteration++;
    System.out.println("This is the "+iteration+" iterations");

    int[] labels = new int[totalPoints];
    int chunk_size = totalPoints / threadCount;

    for(int i=0;i<threadCount;i++){
      int start = i*chunk_size;

      int end;

        if (i == threadCount - 1) {
            end = totalPoints;
        } else {
            end = (i + 1) * chunk_size; //last chunk Handle non-divisible cases
        }

        //threads creation
        ClusterWorker worker = new ClusterWorker(data, oldCentroids, labels, start, end);
        threads[i] = new Thread(worker);
        threads[i].start();
    }


    for(Thread t:threads){
        t.join();
    }
 // already found the cloest centroid for each data point


  //update centroids
  double[][] newCentroids = updateCentroids(data, labels, k);

    // output statement print
    System.out.println("===================================");
    for (int i = 0; i < k; i++) {
        System.out.printf("Centroid %d: (%.2f, %.2f)%n",
                i + 1,
                newCentroids[i][0],
                newCentroids[i][1]);
    }

    double totalDiff = 0;
    for (int i = 0; i < oldCentroids.length; i++) {
        double dx = oldCentroids[i][0] - newCentroids[i][0];
        double dy = oldCentroids[i][1] - newCentroids[i][1];
        totalDiff += Math.sqrt(dx * dx + dy * dy);
    }
    System.out.printf("Total shift: %.4f | Threshold: %.3f%n", totalDiff, convergeThreshold);
    System.out.println("===================================\n");

    converged = checkConverge(oldCentroids, newCentroids, convergeThreshold); // modify the boolean stamp "converged" status
    oldCentroids = newCentroids; // update the centroid
}
System.out.println("already converged！ program end");
    }




// randomly choose a data point as centroid
private static double[][] initializeCentroids(double[][] data, int K)     {
        double[][] centroids = new double[K][2];
        Random random = new Random();

        for (int i = 0; i < K; i++) {
            // 随机选一个点的索引（0 ~ 总点数-1）
            int randomIndex = random.nextInt(data.length);

            // 把这个随机点当作簇心
            centroids[i][0] = data[randomIndex][0];
            centroids[i][1] = data[randomIndex][1];
        }
        return centroids;
    }


private static  double[][] updateCentroids(double[][] data,int[] labels,int k){
    int[] count = new int[k]; // to record the number of data points in each cluster
    double[][] sum = new double[k][2]; // initiate  the sum array for each cluster


    for (int i = 0; i < data.length; i++) {
            int c = labels[i]; // accept the label number
            count[c]++; // increment
            sum[c][0] += data[i][0];
           sum[c][1] += data[i][1];
    }

    double[][] newCentroids = new double[k][2];

    for (int i = 0; i < k; i++) {
        newCentroids[i][0] = sum[i][0] / count[i];
        newCentroids[i][1] = sum[i][1] / count[i];
    }
    return newCentroids;
}





private static boolean checkConverge(double[][] oldC, double[][] newC, double threshold) {
        double totalDiff = 0;
        for (int i = 0; i < oldC.length; i++) {
            double dx = oldC[i][0] - newC[i][0];
            double dy = oldC[i][1] - newC[i][1];
            totalDiff += Math.sqrt(dx*dx + dy*dy);
        }
        return totalDiff < threshold;
    }
}
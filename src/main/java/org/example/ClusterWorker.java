package org.example;

public class ClusterWorker implements  Runnable{
    private  final double[][] data;
    private final double[][] centroids; // to track each centroid
    private final int[] labels;  // to mark each point belongs which centroid
    private final int start;
    private final int end;

    public ClusterWorker(double[][] data, double[][] centroids, int[] labels, int start, int end) {
        this.data = data;
        this.centroids = centroids;
        this.labels = labels;
        this.start = start;
        this.end = end;
    }

    @Override
    public void run() {
        for (int i = start; i < end; i++) {
            labels[i] = findClosestCentroid(data[i]);
        }
    }


// the sub-threads are not responsible for calculating the  centroids
    private int findClosestCentroid(double[] point){
        int closest = 0;
        double minDist = Double.MAX_VALUE;

        for(int i=0;i<centroids.length;i++){
            double dist = euclideanDistance(point,centroids[i]);

            if (dist<minDist){
                minDist = dist;
                closest = i;
            }

        }
      return closest;
    }

    private double euclideanDistance(double[] p1, double[] p2){
        double dx =p1[0] - p2[0];
        double dy =p1[1] - p2[1];
        return Math.sqrt(dx*dx+dy*dy);
    }

}

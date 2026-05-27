package Lock_Implemetation;

import java.util.Random;
import java.util.Scanner;

public class Driver_LockVersion {
    public static void main(String[] args){

//
Thread[] threads = new Thread[2];

 // random points generating
        Scanner  scanner = new Scanner(System.in);
   System.out.println("Please enter the number of data points ");
   int number_of_dataPoints = scanner.nextInt();
 double[][] data_resource = new double[number_of_dataPoints][2];

        Random random = new Random();
  for (int i=0;i<number_of_dataPoints;i++){
      data_resource[i][0] = random.nextDouble()*100;
      data_resource[i][1] = random.nextDouble()*100;
  }

System.out.println("Please enter the number of centroids");
  int k = scanner.nextInt();

// randomly choose 3 centroids
double[][] centroids = initial_centroids(data_resource,k);



Cluster_LockVersion clusterLockVersion = new Cluster_LockVersion(data_resource,centroids,0.001);
        threads[0] = new Thread(new AssignPoints(clusterLockVersion));
        threads[1] = new Thread(new updateCentroids(clusterLockVersion,k));

        for (Thread t:threads){
            t.start();
        }

        // Wait for both threads to finish before exiting main
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("already converged! program end");
    }


//initial 3 random non-centroids points as centroids
private static double[][] initial_centroids(double[][] data,int k){
      double[][] centroid = new double[k][2];
      Random random = new Random(data.length);
      for (int i=0;i<k;i++){
          int index = random.nextInt(data.length);
        centroid[i][0] = data[index][0];
        centroid[i][1] = data[index][1];
      }
    return centroid;
    }

}


class AssignPoints implements Runnable{
   private  Cluster_LockVersion clusterLockVersion;


  public AssignPoints(Cluster_LockVersion clusterLockVersion){
      this.clusterLockVersion = clusterLockVersion;

  }

    @Override
    public void run() {
      clusterLockVersion.assignCluster();
    }
}



class updateCentroids implements Runnable{
    private  Cluster_LockVersion clusterLockVersion;
    private int k;
    public updateCentroids(Cluster_LockVersion clusterLockVersion,int k){
        this.clusterLockVersion = clusterLockVersion;
        this.k = k;
    }

    @Override
    public void run() {
        clusterLockVersion.UpdateCentroids(k);
    }
}

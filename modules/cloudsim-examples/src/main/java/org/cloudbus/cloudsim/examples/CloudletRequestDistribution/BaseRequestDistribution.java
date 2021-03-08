package org.cloudbus.cloudsim.examples.CloudletRequestDistribution;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.UtilizationModelStochastic;
import org.cloudbus.cloudsim.container.core.ContainerCloudlet;
import org.cloudbus.cloudsim.examples.container.ConstantsExamples;
import org.cloudbus.cloudsim.examples.container.ContainerCloudSimExample1;
import org.cloudbus.cloudsim.examples.container.UtilizationModelPlanetLabInMemoryExtended;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class BaseRequestDistribution {

    private int interval_length;
    private int terminated_time;
    private int Poisson_lambda;
    private double Gaussian_mean;
    private double Gaussian_var;

    private List<ContainerCloudlet> required_workloads;


//    public void SetIntervalLength(int interval_length){
//        this.interval_length = interval_length;
//    }

//    public static void main(String[] args) {
//        BaseRequestDistribution X = new BaseRequestDistribution(101, 10,
//                10,
//                1000, 100);
//
//        for(Cloudlet c : X.GetWorkloads()) {
//            Log.formatLine("cloudlet id: " + c.getCloudletId() + " Start execution time: "
//                    + c.getExecStartTime() + " length: " + c.getCloudletLength());
//        }
//    }

    public BaseRequestDistribution(int terminated_time, int interval_length, int Poisson_lambda, double Gaussian_mean, double Gaussian_var) throws IOException {
       // SetIntervalLength(interval_length);
        this.terminated_time = terminated_time;
        this.interval_length = interval_length;
        this.Poisson_lambda = Poisson_lambda;
        this.Gaussian_mean = Gaussian_mean;
        this.Gaussian_var = Gaussian_var;
        PoissonDistribution RequestNum_distribution = new PoissonDistribution(Poisson_lambda);
        required_workloads = new ArrayList<ContainerCloudlet>();
        int CloudletID = 0;
        //Container Cloudlet generation.
//        String inputFolderName = ContainerCloudSimExample1.class.getClassLoader().getResource("workload/planetlab").getPath();
//        ArrayList<String> file_path = new ArrayList<String>();
//        java.io.File inputFolder1 = new java.io.File(inputFolderName);
//        java.io.File[] files1 = inputFolder1.listFiles();
//        for (java.io.File aFiles1 : files1) {
//            java.io.File inputFolder = new java.io.File(aFiles1.toString());
//            java.io.File[] files = inputFolder.listFiles();
//            for (int i = 0; i < files.length; ++i){
////                Log.formatLine(files[i].getAbsolutePath());
//                file_path.add(files[i].getAbsolutePath());
//            }
//        }
//        Log.formatLine("File Path: " + file_path.size());
        Random rand = new Random();
        for(int cur_time = 0; cur_time < terminated_time; cur_time += interval_length) {
            int RequestNum = RequestNum_distribution.GetNextPoisson();
            NormalDistribution CloudletLength_distribution = new NormalDistribution(Gaussian_mean, Gaussian_var);
            UtilizationModelStochastic UtilizationModelStochastic = new UtilizationModelStochastic();
            for (int i = 0; i < RequestNum; i++) {
                double CloudletLength = CloudletLength_distribution.GetNextGaussian();
                ContainerCloudlet tmp = new ContainerCloudlet(CloudletID++, (long) CloudletLength, ConstantsExamples.CLOUDLET_PES, 300L, 300L,
                        //new UtilizationModelPlanetLabInMemoryExtended(file_path.get(CloudletID), ConstantsExamples.SCHEDULING_INTERVAL),
                        UtilizationModelStochastic,
                        UtilizationModelStochastic, UtilizationModelStochastic);
                tmp.setExecStartTime(cur_time + rand.nextInt(interval_length));
                required_workloads.add(tmp);
            }
        }
    }

    public List<ContainerCloudlet> GetWorkloads(){
        return required_workloads;
    }

    public int GetTerminatedTime(){
        return this.terminated_time;
    }
    public int GetIntervalLength(){
        return this.interval_length;
    }
    public int GetPoissonLambda(){
        return this.Poisson_lambda;
    }
    public double GetGaussianMean(){
        return this.Gaussian_mean;
    }
    public double GetGaussianVar(){
        return this.Gaussian_var;
    }

}

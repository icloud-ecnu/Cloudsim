package org.cloudbus.cloudsim.examples.CloudletRequestDistribution;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.UtilizationModelStochastic;
import org.cloudbus.cloudsim.container.core.ContainerCloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.examples.container.ConstantsExamples;
import org.cloudbus.cloudsim.examples.container.ContainerCloudSimExample1;
import org.cloudbus.cloudsim.examples.container.PredictationTest;
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
    //Fetch more info for new generation method.
    private List<ContainerCloudlet> required_workloads;
    private List<List<ContainerCloudlet>> IntervalWorkloads;    //全部的数据


    public BaseRequestDistribution(int terminated_time, int interval_length, int Poisson_lambda, double Gaussian_mean, double Gaussian_var) throws IOException {
       // SetIntervalLength(interval_length);
        this.terminated_time = terminated_time;
        this.interval_length = interval_length;
        this.Poisson_lambda = Poisson_lambda;
        this.Gaussian_mean = Gaussian_mean;
        this.Gaussian_var = Gaussian_var;
        PoissonDistribution RequestNum_distribution = new PoissonDistribution(Poisson_lambda);
        required_workloads = new ArrayList<ContainerCloudlet>();
        IntervalWorkloads = new ArrayList<List<ContainerCloudlet>>();
        int CloudletID = 0;

        Random rand = new Random();
        for(int cur_time = 0; cur_time < terminated_time; cur_time += interval_length) {
            int RequestNum = RequestNum_distribution.GetNextPoisson();
            NormalDistribution CloudletLength_distribution = new NormalDistribution(Gaussian_mean, Gaussian_var);
            UtilizationModelStochastic UtilizationModelStochastic = new UtilizationModelStochastic();
            List<ContainerCloudlet> IntervalWorkloadList = new ArrayList<ContainerCloudlet>();
            Random randForX = new Random();
            for (int i = 0; i < RequestNum; i++) {
                double CloudletLength = CloudletLength_distribution.GetNextGaussian();
                ContainerCloudlet tmp = new ContainerCloudlet(CloudletID++, (long) CloudletLength, ConstantsExamples.CLOUDLET_PES, 300L, 300L,
                        //new UtilizationModelPlanetLabInMemoryExtended(file_path.get(CloudletID), ConstantsExamples.SCHEDULING_INTERVAL),
                        UtilizationModelStochastic,
                        UtilizationModelStochastic, UtilizationModelStochastic);
                tmp.setExecStartTime(cur_time + rand.nextInt(interval_length));
                tmp.setCallPositionX((int)(randForX.nextGaussian() * Math.sqrt(250000)
                        + (cur_time * 1.0) /(24 * 60  * 60) * 10000 ));
                tmp.setCallPositionY(rand.nextInt(10000));
                tmp.UpdateHistoricalHangOnTimeList((int)(CloudSim.ConvertLengthToTime(CloudletLength)));       //+++++++UpdateHistoricalHangOnTimeList++++++++++++
                IntervalWorkloadList.add(tmp);
                required_workloads.add(tmp);
                //IntervalWorkloadList每次存储的是一个时间段的所有的连接，
            }
            IntervalWorkloads.add(IntervalWorkloadList);    //将一个时间段作为一个list元素进行存储，IntervalWorkloads最后存储的是一天的，是一个二维的，横向是每个连接，竖向是每个时间段
        }
    }

    public List<List<ContainerCloudlet>> Get2DxWorkloads(){
        return IntervalWorkloads;
    }
    public List<ContainerCloudlet> GetWorkloads(){return required_workloads;}
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

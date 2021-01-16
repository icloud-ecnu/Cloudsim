package org.cloudbus.cloudsim.examples.CloudletRequestDistribution;
import org.cloudbus.cloudsim.Cloudlet;

import java.util.ArrayList;


public class BaseRequestDistribution {
    //variables requested: interval length, total_request, cloudlet.length,
    //function requested:
    // fuc1, cloudletlist_generation (invoke func2 and func3)
    // two parts: fuc2(cloudlet number across intervals based on Poisson) func3 (cloudlet length within interval based on Gaussian)
    private int total_request_num;
    private int interval_length;
    private ArrayList<int> interval_num_list;
    private ArrayList<int> cloudlet_length_list
    public cloudlet<list> request_distribution;
    BaseRequestDistribution(int total_request_num, int interval_length){
        SetTotalRequest(total_request_num);
        SetIntervalLength(interval_length);
        request_distribution = new ArrayList<cloudlet>();
    }

    public void SetTotalRequest(int total_request_num){
        this.total_request_num = total_request_num;
    }

    public void SetIntervalLength(int interval_length){
        this.interval_length = interval_length;
    }

    protected cloudlet<list> CloudletsGeneration(){
        for num in interval_num_list
    }




}

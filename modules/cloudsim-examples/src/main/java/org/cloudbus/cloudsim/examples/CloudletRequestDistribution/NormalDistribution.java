package org.cloudbus.cloudsim.examples.CloudletRequestDistribution;

import org.cloudbus.cloudsim.Log;

public class NormalDistribution {
    private double mean = 0.0;
    private double variance = 1.0;
    private java.util.Random random;
    NormalDistribution(){
        Log.formatLine(Log.Opr.Base,"Prompt: You have not set the parameters of Gaussian distribution, DEFAULT is the standard Normal Distribution");
        random = new java.util.Random();
    }

    NormalDistribution(double mean, double variance){
        this.mean = mean;
        this.variance = variance;
        random = new java.util.Random();
    }

    public double GetNextGaussian(){
        return random.nextGaussian() * Math.sqrt(variance) + mean;
    }
}

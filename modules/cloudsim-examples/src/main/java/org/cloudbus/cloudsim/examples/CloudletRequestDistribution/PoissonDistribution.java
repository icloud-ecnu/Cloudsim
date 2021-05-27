package org.cloudbus.cloudsim.examples.CloudletRequestDistribution;

import org.cloudbus.cloudsim.Log;

public class PoissonDistribution {
    private double lambda = 10;
    PoissonDistribution(){
        Log.formatLine(Log.Opr.Base, "Prompt: You have not set the lambda value for the Poisson distribution, and adopt the DEFAULT: 10 !!!");
    }

    PoissonDistribution(double lambda){
        this.lambda = lambda;
    }
//    public static void main(String[] args) {
//        PoissonDistribution X = new PoissonDistribution(10);
//        for(int i = 0; i < 10; i++){
//            System.out.println(X.GetNextPoisson());
//        }
//    }
    public int GetNextPoisson(){
        int sum = 0, n = -1;
        while(sum < lambda){
            n += 1;
            sum -= Math.log(Math.random());
        }
        return n;
    }
}

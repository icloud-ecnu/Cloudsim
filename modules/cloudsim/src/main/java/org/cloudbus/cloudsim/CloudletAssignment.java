package org.cloudbus.cloudsim;

import org.cloudbus.cloudsim.core.CloudSim;

public class CloudletAssignment extends CloudletSchedulerDynamicWorkload{


    public CloudletAssignment(double mips, int numberOfPes){
        super(mips, numberOfPes);

    }

    @Override
    public double cloudletSubmit(Cloudlet cl){
        ResCloudlet rcl = new ResCloudlet(cl);
        return 0;
    }
}

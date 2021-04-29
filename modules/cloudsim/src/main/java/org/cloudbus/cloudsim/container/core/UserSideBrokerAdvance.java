package org.cloudbus.cloudsim.container.core;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.lists.ContainerList;
import org.cloudbus.cloudsim.container.utils.IDs;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.lists.VmList;

import java.text.DecimalFormat;
import java.util.*;

public class UserSideBrokerAdvance extends UserSideBroker{
    private List<Map<Integer, Double>> IntervalDataCenterList;
    public UserSideBrokerAdvance(String name, double overBookingfactor,
                          Container e, double[] coordinate, int interval) throws Exception{
        super(name, overBookingfactor, e, coordinate, interval);
        IntervalDataCenterList = new ArrayList<Map<Integer, Double>>();
    }

    public void setIntervalDataCenters(List<Map<Integer, Double>> array){
        IntervalDataCenterList = array;
    }
}

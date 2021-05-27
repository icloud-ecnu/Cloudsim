package org.cloudbus.cloudsim.container.core;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sareh on 10/07/15.
 */
public class ContainerCloudlet extends Cloudlet  implements Comparable<ContainerCloudlet>, Serializable {
    private static final long serialVersionUID = 42L;

    public int containerId = -1;
    private double DelayFactor = -1;
    private int hostId = -1;
    //chris tuning:
    //-----------------------------------------
    private double CallPositionX = -1, CallPositionY = -1;
    public List<Integer> HistoricalHangOnTimeList ;

    public void setCallPositionX(int x){CallPositionX = x;}
    public double getCallPositionX(){ return CallPositionX;}

    public void setCallPositionY(int y){CallPositionY = y;}
    public double getCallPositionY(){ return CallPositionY;}

    public List<Integer> getHistoricalHangOnTimeList(){return HistoricalHangOnTimeList;}
    public void UpdateHistoricalHangOnTimeList(Integer x){HistoricalHangOnTimeList.add(x);}


    public int compareTo(ContainerCloudlet x){
        return getCloudletId() - x.getCloudletId();
    }

    //-----------------------------------------

    public int getContainerId() {return containerId; }
    public void setContainerId(int containerId) {this.containerId = containerId; }
    public void setHostId(int id){ hostId = id; }
    public int getHostId(){return hostId;}
    public double getDelayFactor(){ return this.DelayFactor; }
    public void setDelayFactor(double delay){this.DelayFactor = delay; }

    public ContainerCloudlet(int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize, long cloudletOutputSize, UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam, UtilizationModel utilizationModelBw) {
        super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu, utilizationModelRam, utilizationModelBw);
        HistoricalHangOnTimeList = new ArrayList<Integer>();
    }

    public ContainerCloudlet(int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize, long cloudletOutputSize, UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam, UtilizationModel utilizationModelBw, boolean record, List<String> fileList) {
        super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu, utilizationModelRam, utilizationModelBw, record, fileList);
        HistoricalHangOnTimeList = new ArrayList<Integer>();
    }

    public ContainerCloudlet(int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize, long cloudletOutputSize, UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam, UtilizationModel utilizationModelBw, List<String> fileList) {
        super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu, utilizationModelRam, utilizationModelBw, fileList);
        HistoricalHangOnTimeList = new ArrayList<Integer>();
    }

    public ContainerCloudlet(int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize, long cloudletOutputSize, UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam, UtilizationModel utilizationModelBw, boolean record) {
        super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu, utilizationModelRam, utilizationModelBw, record);
        HistoricalHangOnTimeList = new ArrayList<Integer>();
    }


/*    private double PredicConnectionDuration(ContainerCloudlet cl){

        double a = 0;

        for (int i = 0; i < HistoricalHangOnTimeList.size(); i++) {
            News s = (News)list.get(i);
            System.out.println(s.getId()+"  "+s.getTitle()+"  "+s.getAuthor());
　　　　}

        return a;
    }*/

}

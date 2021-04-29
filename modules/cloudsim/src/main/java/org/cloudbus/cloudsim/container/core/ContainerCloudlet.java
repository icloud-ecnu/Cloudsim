package org.cloudbus.cloudsim.container.core;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sareh on 10/07/15.
 */
public class ContainerCloudlet extends Cloudlet {
    public int containerId = -1;
    private double DelayFactor = -1;
    private int hostId = -1;
    //chris tuning:
    //-----------------------------------------
    private double CallPositionX = -1, CallPositionY = -1;
    private List<Integer> HistoricalHangOnTimeList ;

    public void setCallPositionX(int x){CallPositionX = x;}
    public double getCallPositionX(){ return CallPositionX;}

    public void setCallPositionY(int y){CallPositionY = y;}
    public double getCallPositionY(){ return CallPositionY;}

    public List<Integer> getHistoricalHangOnTimeList(){return HistoricalHangOnTimeList;}
    public void UpdateHistoricalHangOnTimeList(Integer x){HistoricalHangOnTimeList.add(x);}

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


}

package org.cloudbus.cloudsim.container.core;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.lists.ContainerList;
import org.cloudbus.cloudsim.container.utils.IDs;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.lists.VmList;

//import static org.cloudbus.cloudsim.examples.container.XOR_using_NeuralNet.*;

import java.text.DecimalFormat;
import java.util.*;

public class UserSideBrokerAdvance extends UserSideBroker{
    //each element includes A map, the Key(DataCenterID) + value(ratio).
    private List<Map<Integer, Double>> IntervalDataCenterList;
    private Integer IntervalDataCentersIndex;  //+
    private Map<Integer, Integer> DataCentersCloudLetLimit;    //+datacenter中容器任务的限制

    //+
    public UserSideBrokerAdvance(String name, double overBookingfactor,
                                 Container e, double[] coordinate, int interval) throws Exception{
        super(name, overBookingfactor, e, coordinate, interval);
        IntervalDataCenterList = new ArrayList<Map<Integer, Double>>();
        IntervalDataCentersIndex = 0;
        DataCentersCloudLetLimit = new HashMap<Integer, Integer>();

    }


        @Override
        public void ProcessContainerScalabilitySync(SimEvent ev){  //+弹性伸缩按以往总时间区间预测接下来的连接数
        if(CloudSim.clock() > LastUpdateTime + DatacenterStatusUpdateInterval){
            Log.printLine(CloudSim.TimeFormat(CloudSim.clock())+ " Broker: Interval " + IntervalDataCentersIndex + " ends.");
            //notify all datacenters to remove their empty containers.
            for(int i = 0; i < getDatacenterIdsList().size(); i++){
                sendNow(getDatacenterIdsList().get(i), containerCloudSimTags.SCALE_DOWN);
            }
            Status_stale = true;
            DataCentersCloudLetLimit.clear();
        }
        if(Status_stale) {  //+   如果是true的话，
            /* Interval间隔，区间
            Step 1: predict the container number.
            Step 2: allocate containers to the DataCenters according to, IntervalDataCenterList.
             */

            //update the IntervalDataCentersIndex to prepare for the CloudletBinding.
            IntervalDataCentersIndex++;
            int IntervalContainerNumber = 0;//passive extension method.
            if(CloudSim.initiative)
                IntervalContainerNumber =  (int) (predictContainerNumber(IntervalDataCentersIndex) * 1.5);

            //控制弹性变量开关，0为无弹性，注释掉此语句为使用本系统自动弹性扩容方案，给与定值为手动定值弹性扩容
//            IntervalContainerNumber = 0;

            Log.printLine(CloudSim.TimeFormat(CloudSim.clock())+ " Broker: Interval " + IntervalDataCentersIndex + " begins.");
            Map<Integer, Double> ThisIntervalDataCenters = IntervalDataCenterList.get(IntervalDataCentersIndex % 72);
            for (int destId : ThisIntervalDataCenters.keySet()){
                //send create containers in each dataCenter.
                int containerNum = (int)(ThisIntervalDataCenters.get(destId) * IntervalContainerNumber);
                //re-initialize the cloudLet number limitation for each dataCenter, 4 is cloudLet capacity in each container.
                DataCentersCloudLetLimit.put(destId, containerNum * 4);
                Log.formatLine(Log.Opr.ScaleUp, CloudSim.TimeFormat(CloudSim.clock())
                        + " Broker: SCALE UP !!! Create another " +  containerNum + " containers in dataCenter " + destId );
                for(int i = 0; i < containerNum ; i++)
                    CreateOneContainerInDatacenter(destId);
                //update the History data in cloudSim
                CloudSim.HistoricalContainerNumberInIntervals.add(IntervalContainerNumber);
            }
            LastUpdateTime = CloudSim.clock();
            Status_stale = false;
        }
    }



    protected int predictContainerNumber(int IntervalIndex){
        List<Integer> history = CloudSim.HistoricalContainerNumberInIntervals;
        List<Integer> thisIntervalNumber = new ArrayList<Integer>();
        for(int i = IntervalIndex; i < history.size(); i += 72){
            thisIntervalNumber.add(history.get(i));
        }
        int res = 0;
        for(int i = thisIntervalNumber.size() - 1, j = 0; j < 30; j++, i--){
            res += thisIntervalNumber.get(i);
        }

        return (res / 30) / 4;
    }

    protected void CreateOneContainerInDatacenter(int dataCenterid){
        List<PowerContainer> l = new ArrayList<PowerContainer>(1);
        PowerContainer con = new PowerContainer(IDs.pollId(Container.class), getId(), const_container.getMips(),
                const_container.getNumberOfPes() ,
                (int)const_container.getRam(),
                const_container.getBw(), const_container.getSize(),
                const_container.getContainerManager(), const_container.getContainerCloudletScheduler(),
                const_container.getSchedulingInterval());
        con.setAvailablePesNum(const_container.getNumberOfPes());
        l.add(con);
        submitContainerList(l);
        send(dataCenterid, CloudSim.getMinTimeBetweenEvents(), containerCloudSimTags.CONTAINER_SUBMIT, l);
    }

    @Override
    protected void processCloudletResubmit(SimEvent ev){
        ContainerCloudlet cl = (ContainerCloudlet)ev.getData();
        if(cl.containerId == -1) {
            CreateOneContainerInDatacenter(ev.getSource());
            Log.formatLine(Log.Opr.ScaleUp, CloudSim.clock() + " Create a new container in datacenter " + ev.getSource()
                    + ", and reconfigure the CloudLet delay.");
            cl.setDelayFactor(cl.getDelayFactor() + 20000);
            send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), containerCloudSimTags.CLOUDLET_BINDING, cl);
        }
        else{
            cloudletsSubmitted++;
            getCloudletSubmittedList().add(cl);
            getCloudletList().remove(cl);
            Log.formatLine(Log.Opr.InterDatacenterAllocation, "Broker gets cloudlet " + cl.getCloudletId() + " Rebinding Succeed...");
        }
    }

    @Override  //+
    protected void processCloudletBinding(SimEvent ev) {   //选取容器进行绑定，首先选取数据中心，怎么选取数据中心
        ContainerCloudlet cl = (ContainerCloudlet) ev.getData();

        ProcessContainerScalabilitySync(ev);
        //fetch the most appropriate DataCenter in IntervalDataCenterList to allocate.
        Log.formatLine(Log.Opr.InterDatacenterAllocation,
                CloudSim.clock()+ " BROKER:fetch the most appropriate DataCenter for CloudLet " + cl.getCloudletId());
        /*
        How to select the datacenter.
        Default: greedy strategy according to distance
        */
        Map<Integer, Double> d  = IntervalDataCenterList.get(IntervalDataCentersIndex % 72);
        int destDataCenterId = -1, sub_destDataCenterId = -1;
        double MAX_VALUE = Double.POSITIVE_INFINITY, SUB_MAX_VALUE = Double.POSITIVE_INFINITY;
        Log.printLine("Broker: Interval " + IntervalDataCentersIndex + ", Select the nearest datacenter to bind cloudLet " + cl.getCloudletId()  + " in " + d.keySet().size() + " datacenters.");
        //首先是要按照主动扩展的容器数所能承载的连接数量贪心的找最近的数据中心，当所有的数据中心都没有承受能力之后发到距离最近的数据中心
        //max_value为仍然有承载能力的数据中心的距离，sub_max_value为没有承载能力的数据中心的最短距离。
        for(Integer DataCenterID : d.keySet()){
//            Log.printLine(DataCenterID);
            if(DataCentersCloudLetLimit.get(DataCenterID) == null){
                DataCentersCloudLetLimit.put(DataCenterID, 10000);
                Log.printLine("Broker: Initialize the cloudLet limitation number in datacenter " + DataCenterID);
            }
            double[]pos = UserSideDatacenter.getLocationById(DataCenterID);
            double delta_x = pos[0] - cl.getCallPositionX();
            double delta_y = pos[1] - cl.getCallPositionY();
            double TransmissionDistance = Math.sqrt(delta_x * delta_x + delta_y * delta_y);
            if(MAX_VALUE > TransmissionDistance && DataCentersCloudLetLimit.get(DataCenterID) > 0 ){
                MAX_VALUE = TransmissionDistance;
                destDataCenterId = DataCenterID;
            }
            else if(SUB_MAX_VALUE > TransmissionDistance && DataCentersCloudLetLimit.get(DataCenterID) <= 0 ){
                SUB_MAX_VALUE = TransmissionDistance;
                sub_destDataCenterId = DataCenterID;
            }
        }

        if(destDataCenterId == -1){
            destDataCenterId = sub_destDataCenterId;
            cl.setDelayFactor(SUB_MAX_VALUE);
            Log.printLine("Broker Warning: MAY BE NO RESOURCES LEFT IN DATACENTERS. SEND THE CLOUDLET TO THE NEAREST DATACENTER " + destDataCenterId);
        }
        else{
            cl.setDelayFactor(MAX_VALUE);
            Log.formatLine(Log.Opr.InterDatacenterAllocation,
                    "BROKER: ready to bind cloudLet " + cl.getCloudletId()
                            + " to dataCenter "  + destDataCenterId);
        }
        sendNow(destDataCenterId,containerCloudSimTags.CLOUDLET_BINDING, cl);
        DataCentersCloudLetLimit.put(destDataCenterId, DataCentersCloudLetLimit.get(destDataCenterId) - 1);

    }

    public void setIntervalDataCenters(List<Map<Integer, Double>> array){
        IntervalDataCenterList = array;
    }
}

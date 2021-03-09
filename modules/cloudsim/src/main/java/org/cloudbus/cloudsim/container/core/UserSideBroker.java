package org.cloudbus.cloudsim.container.core;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.lists.ContainerList;
import org.cloudbus.cloudsim.container.utils.IDs;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.lists.VmList;

import java.util.*;

public class UserSideBroker extends ContainerDatacenterBroker{

    private Container const_container;
    private double[] coordinate = new double[]{0, 0};
    private int DatacenterStatusUpdateInterval;
    private double LastUpdateTime = 0;
    private boolean Status_stale = false;
    private int SynchronizationCount = 0;
    private int CurrentOptimalDatacenterId = -1;
    private double CurrentDelay;
    private ArrayList<SimEvent> SynchronizationList = new ArrayList<SimEvent>();

    @Override
    public void processEvent(SimEvent ev) {
        switch (ev.getTag()) {
            // Resource characteristics request
            case CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST:
                processResourceCharacteristicsRequest(ev);
                break;
            // Resource characteristics answer
            case CloudSimTags.RESOURCE_CHARACTERISTICS:
                processResourceCharacteristics(ev);
                break;
            // VM Creation answer
            case CloudSimTags.VM_CREATE_ACK:
                processVmCreate(ev);
                break;
            // New VM Creation answer
            case containerCloudSimTags.VM_NEW_CREATE:
                processNewVmCreate(ev);
                break;
            // A finished cloudlet returned
            case CloudSimTags.CLOUDLET_RETURN:
                processCloudletReturn(ev);
                break;
            // if the simulation finishes
            case CloudSimTags.END_OF_SIMULATION:
                shutdownEntity();
                break;
            case containerCloudSimTags.CONTAINER_CREATE_ACK:
                processContainerCreate(ev);
                break;
            case containerCloudSimTags.CLOUDLETS_RESUBMIT:
                processCloudletResubmit(ev);
                break;
            case containerCloudSimTags.CLOUDLET_BINDING:
                ContainerCloudlet cl = (ContainerCloudlet) ev.getData();
                sendNow(CurrentOptimalDatacenterId,containerCloudSimTags.CLOUDLET_BINDING, cl);
                break;
            case containerCloudSimTags.CONTAINER_SCALABILITY_SYNC:
                ProcessContainerScalabilitySync(ev);
                break;
            case containerCloudSimTags.CONTAINER_SCALABILITY_ACK:
                ProcessContainerScalabilityACK(ev);
                break;
            case containerCloudSimTags.DATACENTER_STATUS_UPDATE:
                processDatacenterStatusUpdate(ev);
                break;


            // other unknown tags are processed by this method
            default:
                processOtherEvent(ev);
                break;
        }
    }



    public UserSideBroker(String name, double overBookingfactor, Container e) throws Exception {
        super(name, overBookingfactor);
        this.const_container = e;
    }

    public UserSideBroker(String name, double overBookingfactor, Container e, double[] coordinate, int interval) throws Exception {
        super(name, overBookingfactor);
        this.const_container = e;
        this.coordinate[0] = coordinate[0];
        this.coordinate[1] = coordinate[1];
        this.DatacenterStatusUpdateInterval =  interval / 10;

    }

    public void setLocation(double[] a) {
        this.coordinate[0] = a[0];
        this.coordinate[1] = a[1];
    }

    public double[] getLocation(){
        return this.coordinate;
    }



    @Override
    protected void submitCloudlets(){
        List<ContainerCloudlet> successfullySubmitted = new ArrayList<>();
        List<ContainerCloudlet> FailedSubmitted = new ArrayList<>();
        for (ContainerCloudlet clt : getCloudletList()) {
            if(clt.getContainerId() == -1){
                if(CurrentOptimalDatacenterId == -1)
                    CurrentOptimalDatacenterId = getDatacenterIdsList().get(0);
                Log.formatLine(Log.Opr.InterDatacenterAllocation, CloudSim.clock() + "Broker: cloudlet " + clt.getCloudletId()
                                + " has not been bound. Communicate with datacenter " + CurrentOptimalDatacenterId);
                send(getId(), clt.getExecStartTime(), containerCloudSimTags.CLOUDLET_BINDING, clt);
                FailedSubmitted.add(clt);
            }
            else{
                cloudletsSubmitted++;
                getCloudletSubmittedList().add(clt);
                successfullySubmitted.add(clt);
            }
        }
        getCloudletList().removeAll(successfullySubmitted);
        successfullySubmitted.clear();
        if(FailedSubmitted.size() > 0){
            send(getDatacenterIdsList().get(0), DatacenterStatusUpdateInterval, containerCloudSimTags.SCALABILITY_CHECK, FailedSubmitted.get(0));
        }
    }

    protected void processCloudletResubmit(SimEvent ev){
        ContainerCloudlet cl = (ContainerCloudlet)ev.getData();
        if(cl.containerId == -1) {
            send(getId(), DatacenterStatusUpdateInterval, containerCloudSimTags.CLOUDLET_BINDING, cl);
//            send(getDatacenterIdsList().get(0), DatacenterStatusUpdateInterval, containerCloudSimTags.SCALABILITY_CHECK, cl);
//            Log.formatLine(Log.Opr.InterDatacenterAllocation, CloudSim.clock() + " Datacenter " + ev.getSource() + " has no left resources, waiting...");
        }
        else{
            cloudletsSubmitted++;
            getCloudletSubmittedList().add(cl);
            getCloudletList().remove(cl);
            Log.formatLine(Log.Opr.InterDatacenterAllocation, "Broker gets cloudlet " + cl.getCloudletId() + " Rebinding Succeed...");
        }
    }



    protected void processDatacenterStatusUpdate(SimEvent ev){
        SynchronizationCount++;
        SynchronizationList.add(ev);
        Log.formatLine(Log.Opr.InterDatacenterAllocation, CloudSim.clock() + " Synchronization get info from datacenter " + ev.getSource());
        if(SynchronizationCount == getDatacenterIdsList().size()){
            Log.formatLine(Log.Opr.InterDatacenterAllocation, CloudSim.clock() + " Synchronization processing... " );
            double max_value = Double.MAX_VALUE;
            for(SimEvent obj : SynchronizationList){
                double[] data = (double[])obj.getData();
                double approximate_res = data[1];
                if(max_value > approximate_res){
                    max_value = approximate_res;
                    CurrentOptimalDatacenterId = (int)data[0];
                    CurrentDelay = approximate_res;
                }
            }
            Log.formatLine(Log.Opr.Synchronization, CloudSim.clock() +
                    " Synchronization RESULT: SET THE OPTIMAL DATACENTER ID: " + CurrentOptimalDatacenterId );
            SynchronizationCount = 0;
            SynchronizationList.clear();
        }
    }




    @Override
    protected void processResourceCharacteristics(SimEvent ev) {
        ContainerDatacenterCharacteristics characteristics = (ContainerDatacenterCharacteristics) ev.getData();
        getDatacenterCharacteristicsList().put(characteristics.getId(), characteristics);

        if (getDatacenterCharacteristicsList().size() == getDatacenterIdsList().size()) {
//            getDatacenterCharacteristicsList().clear();
//            setDatacenterRequestedIdsList(new ArrayList<Integer>());
              createVmsInDatacenter(getDatacenterIdsList().get(0));
        }
    }

    @Override
    protected void createVmsInDatacenter(int datacenterId) {
        // send as much vms as possible for this datacenter before trying the next one
        int requestedVms = 0;
        int i = 0;
        for (ContainerVm vm : getVmList()) {
            int datacenterID = getDatacenterIdsList().get(i % getDatacenterIdsList().size());
            String datacenterName = CloudSim.getEntityName(datacenterID);
            if (!getVmsToDatacentersMap().containsKey(vm.getId())) {
                Log.formatLine(String.format("%s: %s: Trying to Create VM #%d in %s", CloudSim.clock(), getName(), vm.getId(), datacenterName));
                sendNow(datacenterID, CloudSimTags.VM_CREATE_ACK, vm);
                requestedVms++;
            }
            i++;
        }
        getDatacenterRequestedIdsList().addAll(getDatacenterIdsList());
        setVmsRequested(requestedVms);
        setVmsAcks(0);
    }


    public void ProcessContainerScalabilitySync(SimEvent ev){
        //check the datacenter status whether to modify
        if(CloudSim.clock() > LastUpdateTime + DatacenterStatusUpdateInterval)
            Status_stale = true;
        if(Status_stale) {
            for (int i = 0; i < getDatacenterIdsList().size(); i++)
                sendNow(getDatacenterIdsList().get(i), containerCloudSimTags.DATACENTER_STATUS_UPDATE, getLocation());
            LastUpdateTime = CloudSim.clock();
            Log.formatLine(Log.Opr.ScaleUp, CloudSim.clock() + " Create another " + CloudSim.LinearScaleUpNum + " containers.");
            for (int i = 0; i < CloudSim.LinearScaleUpNum; i++) {
                send(getId(),CloudSim.getMinTimeBetweenEvents(), containerCloudSimTags.CONTAINER_SCALABILITY_ACK);
            }
            Status_stale = false;
        }
    }


    protected void ProcessContainerScalabilityACK(SimEvent ev){
        //select the result (optimal datacenter) to create the containers.
        List<Container> l = new ArrayList<Container>(1);
        Container con = new Container(IDs.pollId(Container.class), getId(), const_container.getWorkloadTotalMips(),
                const_container.getNumberOfPes() ,
                (int)const_container.getRam(),
                const_container.getBw(), const_container.getSize(),
                const_container.getContainerManager(), const_container.getContainerCloudletScheduler(),
                const_container.getSchedulingInterval());
        con.setAvailablePesNum(const_container.getNumberOfPes());
        l.add(con);
        submitContainerList(l);
        if(CurrentOptimalDatacenterId == -1)
            CurrentOptimalDatacenterId = getDatacenterIdsList().get(0);
        send(CurrentOptimalDatacenterId, CloudSim.getMinTimeBetweenEvents(), containerCloudSimTags.CONTAINER_SUBMIT, l);
        send(getId(), DatacenterStatusUpdateInterval, containerCloudSimTags.SCALABILITY_CHECK);
    }





    @Override
    protected void processCloudletReturn(SimEvent ev) {
        ContainerCloudlet cloudlet = (ContainerCloudlet) ev.getData();
        //chris add for update available PEs for containers.
        Container con = ContainerList.getById(getContainersCreatedList(),cloudlet.getContainerId());
        if(con != null){
            con.setAvailablePesNum(con.getAvailablePesNum() + cloudlet.getNumberOfPes());
           // Log.formatLine(2, "Chris note: Scale down Judge. Current size: " + con.getAvailablePesNum() + " vs " + con.getNumberOfPes() );
            if(con.getAvailablePesNum() == con.getNumberOfPes()){
                //Here we can adopt the corresponding strategy to clear the empty container.
                //But the source codes sometimes leverage the characteristics of the list <ContainersCreatedList>,
                //and thus it will lead to some annoying bug.
                int rmId = con.getId();
                List<Container> new_list = getContainersCreatedList();
                Iterator<Container> iterator = new_list.iterator();
                while (iterator.hasNext()) {
                    Container c = iterator.next();
                    if (con.equals(c)) {
                        iterator.remove();//使用迭代器的删除方法删除
                    }
                }
                setContainersCreatedList(new_list);
                sendNow(con.getVm().getHost().getDatacenter().getId(), containerCloudSimTags.CONTAINER_REMOVE, con);
                Log.formatLine(Log.Opr.ScaleDown, "Broker decides to remove the container ID: " + rmId
                        + " And notices the belonging datacenter...");
            }
        }

        getCloudletReceivedList().add(cloudlet);
        Log.formatLine(2, CloudSim.clock() + ": " + getName() + ": Cloudlet " + cloudlet.getCloudletId()+
                " returned" + " And the number of finished Cloudlets is:" + getCloudletReceivedList().size());
        cloudletsSubmitted--;


        if (getCloudletList().size() == 0 && cloudletsSubmitted == 0) { // all cloudlets executed
            Log.printConcatLine(CloudSim.clock(), ": ", getName(), ": All Cloudlets executed. Finishing...");
            clearDatacenters();
            finishExecution();
        } else { // some cloudlets haven't finished yet
            if (getCloudletList().size() > 0 && cloudletsSubmitted == 0) {
                // all the cloudlets sent finished. It means that some bount
                // cloudlet is waiting its VM be created
                clearDatacenters();
                createVmsInDatacenter(0);
            }

        }
    }


//    public int SelectDatacenter(Container con) {
//        Map<Integer, ContainerDatacenterCharacteristics> DatacenterCharacteristicsList = getDatacenterCharacteristicsList();
//        int OptimalDatacenter = getDatacenterIdsList().get(0);
//        double max_value = Double.MAX_VALUE;
//        for (Map.Entry<Integer, ContainerDatacenterCharacteristics> entry : DatacenterCharacteristicsList.entrySet()){
//            int datacenterId = entry.getKey();
//            ContainerDatacenterCharacteristics characteristic = entry.getValue();
//            if(characteristic.getHostWithFreePe(con.getNumberOfPes()) == null) {
//                continue;
//            }
//            double CpuUtilization = characteristic.getNumberOfFreePes() / characteristic.getNumberOfPes();
//            double MemoryUtilization, BwUtilization;
//            double AvailableMemory = 0, AvailableBw = 0;
//            double totalBW = 0, totalMemory = 0;
//            for(ContainerHost host : characteristic.getHostList()){
//                AvailableMemory += host.getAvailableRam();
//                totalMemory += host.getRam();
//                AvailableBw += host.getAvailableBw();
//                totalBW += host.getBw();
//            }
//            MemoryUtilization = AvailableMemory / totalMemory;
//            BwUtilization = AvailableBw / totalBW;
//            //Calculate the transmission delay to the optional datacenters.
//            double []loc = characteristic.getLocation();
//            double TransmissionDistance = Math.sqrt(loc[0] * loc[0] + loc[1] * loc[1]);
//            double ComprehensiveRes = FactorCombination(TransmissionDistance, CpuUtilization, MemoryUtilization, BwUtilization);
//            if(max_value > ComprehensiveRes){
//                max_value = ComprehensiveRes;
//                OptimalDatacenter = datacenterId;
//            }
//        }
//        //test code
////        Random rand = new Random();
////        OptimalDatacenter = getDatacenterIdsList().get(rand.nextInt(getDatacenterIdsList().size()));
////        Log.formatLine(5, "create a new container in datacenter: " + OptimalDatacenter);
//        return OptimalDatacenter;
//    }





}




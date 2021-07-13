package org.cloudbus.cloudsim.container.core;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.stat.descriptive.summary.Sum;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.ResCloudlet;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.container.lists.ContainerList;
import org.cloudbus.cloudsim.container.resourceAllocators.ContainerAllocationPolicy;
import org.cloudbus.cloudsim.container.resourceAllocators.ContainerVmAllocationPolicy;
import org.cloudbus.cloudsim.container.utils.CostumeCSVWriter;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.core.predicates.PredicateType;
import org.cloudbus.cloudsim.power.PowerHost;

import java.io.IOException;
import java.util.*;

public class UserSideDatacenter extends PowerContainerDatacenter{
    /**
     * The disable container migrations.
     */
    private boolean disableMigrations;
    public int containerMigrationCount;
    private CostumeCSVWriter newlyCreatedVmWriter;
    private int newlyCreatedVms;
    private List<Integer> newlyCreatedVmsList;
    private double vmStartupDelay;
    private double containerStartupDelay;
    public static Map<Integer, double[]> DatacentersLocation = new HashMap<Integer, double[]>();
    private static int ContainerPesNumber = -1;
    public static double TotalContainerCost = 0.0;
    public static ArrayList<Container> AllContainers = new ArrayList<Container>();
    public static Map<Integer, List<Pair<Double, Double>>> Balance_factor = new HashMap<Integer, List<Pair<Double, Double>>>();


    public UserSideDatacenter(String name, ContainerDatacenterCharacteristics characteristics,
                              ContainerVmAllocationPolicy vmAllocationPolicy,
                              ContainerAllocationPolicy containerAllocationPolicy, List<Storage> storageList,
                              double schedulingInterval, String experimentName, String logAddress,
                              double vmStartupDelay, double containerStartupDelay,
                              double []location) throws Exception {
        super(name, characteristics, vmAllocationPolicy, containerAllocationPolicy, storageList, schedulingInterval, experimentName, logAddress);
        String newlyCreatedVmsAddress;
        int index = getExperimentName().lastIndexOf("_");
        newlyCreatedVmsAddress = String.format("%s/NewlyCreatedVms/%s/%s.csv", getLogAddress(), getExperimentName().substring(0, index), getExperimentName());
        setNewlyCreatedVmWriter(new CostumeCSVWriter(newlyCreatedVmsAddress));
        setNewlyCreatedVms(0);
        setDisableMigrations(false);
        setNewlyCreatedVmsList(new ArrayList<Integer>());
        this.vmStartupDelay = vmStartupDelay;
        this.containerStartupDelay = containerStartupDelay;
        Random rand = new Random();
        //We assume the map is 10000km * 10000km
        DatacentersLocation.put(getId(),location);
    }

    public static double[] getLocationById(Integer x)
    {
        return DatacentersLocation.get(x);
    }


    @Override
    public void processEvent(SimEvent ev) {
        int srcId = -1;

        switch (ev.getTag()) {
            // Resource characteristics inquiry
            case CloudSimTags.RESOURCE_CHARACTERISTICS:
                srcId = ((Integer) ev.getData()).intValue();
                sendNow(srcId, ev.getTag(), getCharacteristics());
                break;

            // Resource dynamic info inquiry
            case CloudSimTags.RESOURCE_DYNAMICS:
                srcId = ((Integer) ev.getData()).intValue();
                sendNow(srcId, ev.getTag(), 0);
                break;

            case CloudSimTags.RESOURCE_NUM_PE:
                srcId = ((Integer) ev.getData()).intValue();
                int numPE = getCharacteristics().getNumberOfPes();
                sendNow(srcId, ev.getTag(), numPE);
                break;

            case CloudSimTags.RESOURCE_NUM_FREE_PE:
                srcId = ((Integer) ev.getData()).intValue();
                int freePesNumber = getCharacteristics().getNumberOfFreePes();
                sendNow(srcId, ev.getTag(), freePesNumber);
                break;

            // New Cloudlet arrives
            case CloudSimTags.CLOUDLET_SUBMIT:
                processCloudletSubmit(ev, false);
                break;

            // New Cloudlet arrives, but the sender asks for an ack
            case CloudSimTags.CLOUDLET_SUBMIT_ACK:
                processCloudletSubmit(ev, true);
                break;

            // Cancels a previously submitted Cloudlet
            case CloudSimTags.CLOUDLET_CANCEL:
                processCloudlet(ev, CloudSimTags.CLOUDLET_CANCEL);
                break;

            // Pauses a previously submitted Cloudlet
            case CloudSimTags.CLOUDLET_PAUSE:
                processCloudlet(ev, CloudSimTags.CLOUDLET_PAUSE);
                break;

            // Pauses a previously submitted Cloudlet, but the sender
            // asks for an acknowledgement
            case CloudSimTags.CLOUDLET_PAUSE_ACK:
                processCloudlet(ev, CloudSimTags.CLOUDLET_PAUSE_ACK);
                break;

            // Resumes a previously submitted Cloudlet
            case CloudSimTags.CLOUDLET_RESUME:
                processCloudlet(ev, CloudSimTags.CLOUDLET_RESUME);
                break;

            // Resumes a previously submitted Cloudlet, but the sender
            // asks for an acknowledgement
            case CloudSimTags.CLOUDLET_RESUME_ACK:
                processCloudlet(ev, CloudSimTags.CLOUDLET_RESUME_ACK);
                break;

            // Moves a previously submitted Cloudlet to a different resource
            case CloudSimTags.CLOUDLET_MOVE:
                processCloudletMove((int[]) ev.getData(), CloudSimTags.CLOUDLET_MOVE);
                break;

            // Moves a previously submitted Cloudlet to a different resource
            case CloudSimTags.CLOUDLET_MOVE_ACK:
                processCloudletMove((int[]) ev.getData(), CloudSimTags.CLOUDLET_MOVE_ACK);
                break;

            // Checks the status of a Cloudlet
            case CloudSimTags.CLOUDLET_STATUS:
                processCloudletStatus(ev);
                break;

            // Ping packet
            case CloudSimTags.INFOPKT_SUBMIT:
                processPingRequest(ev);
                break;

            case CloudSimTags.VM_CREATE:
                processVmCreate(ev, false);
                break;

            case CloudSimTags.VM_CREATE_ACK:
                processVmCreate(ev, true);
                break;

            case CloudSimTags.VM_DESTROY:
                processVmDestroy(ev, false);
                break;

            case CloudSimTags.VM_DESTROY_ACK:
                processVmDestroy(ev, true);
                break;

            case CloudSimTags.VM_MIGRATE:
                processVmMigrate(ev, false);
                break;

            case CloudSimTags.VM_MIGRATE_ACK:
                processVmMigrate(ev, true);
                break;

            case CloudSimTags.VM_DATA_ADD:
                processDataAdd(ev, false);
                break;

            case CloudSimTags.VM_DATA_ADD_ACK:
                processDataAdd(ev, true);
                break;

            case CloudSimTags.VM_DATA_DEL:
                processDataDelete(ev, false);
                break;

            case CloudSimTags.VM_DATA_DEL_ACK:
                processDataDelete(ev, true);
                break;

            case CloudSimTags.VM_DATACENTER_EVENT:
                updateCloudletProcessing();
                checkCloudletCompletion();
                break;
            case containerCloudSimTags.CONTAINER_SUBMIT:
                List<Container> data = (List<Container>) ev.getData();
                if(ContainerPesNumber == -1 && data.size() > 0)
                    ContainerPesNumber = data.get(0).getNumberOfPes();
                processContainerSubmit(ev, true);
                break;

            case containerCloudSimTags.CONTAINER_MIGRATE:
                processContainerMigrate(ev, false);
                // other unknown tags are processed by this method
                break;

            case containerCloudSimTags.SCALABILITY_CHECK:
                ContainerScalabilityCheck(ev);
                break;

            case containerCloudSimTags.DATACENTER_STATUS_UPDATE:
//                GetLatestDatacenterInfoAndSendBack(ev);
                break;

            case containerCloudSimTags.CONTAINER_REMOVE:
                RemoveContainerFromDatacenter(ev);
                break;

            case containerCloudSimTags.CLOUDLET_BINDING:
                processCloudletBinding(ev);
                break;

            case containerCloudSimTags.SCALE_DOWN:
                processScaleDown();
                break;

            default:
                processOtherEvent(ev);
                break;
        }
    }


    protected void processScaleDown() {
        List<Container> ToRemove = new ArrayList<Container>();
        for(Container con : getContainerList()){
            if(con.getAvailablePesNum() == con.getNumberOfPes()){
                ToRemove.add(con);
            }
        }
        RemoveContainerFromDatacenter(ToRemove);
        Log.formatLine(Log.Opr.ScaleDown, CloudSim.clock() + " Datacenter " + getId() +
                " : check all containers and clear all empty containers. Removed Container number: " + ToRemove.size());
    }

    protected void RemoveContainerFromDatacenter(SimEvent ev){
        Container con = (Container)ev.getData();
        RemoveContainerFromDatacenter(con);
    }

    protected void RemoveContainerFromDatacenter(List<Container> ToRemoveContainerList){
        for(Container con : ToRemoveContainerList)
            RemoveContainerFromDatacenter(con);
    }

    protected void RemoveContainerFromDatacenter(Container con){
        int RmConID = con.getId() ;
        //Remove container from its located vm
        for(ContainerVm vm : getContainerVmList()){
            if(con.getVm() != null && vm.getId() == con.getVm().getId()){
                AccumulateCostOfContainer(con);
                vm.containerDestroy(con);
                break;
            }
        }
        //Remove container from this datacenter.
        List<Container>containerList = getContainerList();
        int size =  containerList.size();
        for(int i = 0, len = containerList.size(); i < len; i++){
            if(containerList.get(i).getId() == con.getId()){
                containerList.remove(i);
                len--;
                i--;
            }
        }
        setContainerList(containerList);
        if(size == getContainerList().size() + 1)
            Log.formatLine(Log.Opr.ScaleDown, "Datacenter " + getId() + ": Container " + RmConID + " has been removed from datacenter "  + getId());
        else{
            Log.formatLine(Log.Opr.ScaleDown, "Datacenter " + getId() + ": Warning: the container " + RmConID + " might still exists.");
        }
    }



    protected void processCloudletBinding(SimEvent ev){  //这个实体是叫datacenter,  选择一个数据中心进行绑定（把连接绑定在哪个数据中心上）
        ContainerCloudlet cl = (ContainerCloudlet) ev.getData();
        Map<ContainerHost, List<Container>>Optional = new HashMap<ContainerHost, List<Container>>();
        List <Double> CpuUtil = new ArrayList<Double>();
        for(Container con : getContainerList()){
            CpuUtil.add(1 - (double)(con.getAvailablePesNum() / cl.getNumberOfPes()));
            if (con.getAvailablePesNum() >= cl.getNumberOfPes()){
                if(con.getVm() == null){
                    Log.formatLine(Log.Opr.InnerDatacenterAllocation, CloudSim.clock()  +
                            " Warning: This container has not been allocated, Container ID: " + con.getId());
                    continue;
                }
                ContainerHost host = con.getVm().getHost();
                if(Optional.get(host) == null) //Map value initialization.
                    Optional.put(host, new ArrayList<Container>());
                Optional.get(host).add(con);
            }
        }
        //calculate balance factor and update the static variable.
        if(CpuUtil.size() >0){
            double var = 0;
            double mean = 0;
            for(Double x : CpuUtil) mean += x;
            mean /= CpuUtil.size();
            for(Double x : CpuUtil)
                var +=  (Math.pow((x - mean), 2));
            var /= CpuUtil.size();

            List<Pair<Double, Double>> Factor;
            if( Balance_factor.get(getId()) == null)
                Factor = new ArrayList<Pair<Double, Double>>();
            else
                Factor = Balance_factor.get(getId());
            Pair<Double, Double> tmp = Pair.of(CloudSim.clock(), Math.sqrt(var));
            Factor.add(tmp);
            Balance_factor.put(getId(), Factor);
        }

        if(Optional.size() > 0){
            Container BindingCon =  SelectOptionalContainer(cl, Optional);
            if(BindingCon != null){
                cl.setContainerId(BindingCon.getId());
                cl.setVmId(BindingCon.getVm().getId());
                cl.setHostId(BindingCon.getVm().getHost().getId());
                Log.formatLine(Log.Opr.InnerDatacenterAllocation, CloudSim.clock() + " Datacenter " + getId()
                        + ":  Optimal choice: Container id: " + BindingCon.getId() + " has "
                        +  BindingCon.getAvailablePesNum() + " PEs <vs> requests "  + cl.getNumberOfPes()
                        + " PEs. So bind Cloudlet " + cl.getCloudletId() + "  to container " + BindingCon.getId());
                BindingCon.setAvailablePesNum(BindingCon.getAvailablePesNum() - cl.getNumberOfPes());
                sendNow(getId(), CloudSimTags.CLOUDLET_SUBMIT, cl);
                return;
            }
        }
        //if no resources left, return the original cl
        send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), containerCloudSimTags.CLOUDLETS_RESUBMIT, cl);
        Log.formatLine(Log.Opr.InterDatacenterAllocation, CloudSim.clock()
                + "  Currently datacenter " + getId() +" has no left resources. Binding failed." );

    }

    /* ATTENTION:
    Modify here, ought to obtain the left running time for each CloudLet on each container.
    Combing with the predicted length of the binding CloudLet, choose one appropriate container to allocate.
     */
    protected Container SelectOptionalContainer(ContainerCloudlet cl, Map<ContainerHost, List<Container>>Optional){
    //Default strategy: select the optimal container with the lowest cpu utilization

//        List<ContainerHost> OptionalHosts = new ArrayList<>(Optional.keySet());
//        Collections.sort(OptionalHosts);
//        return  Optional.get(OptionalHosts.get(0)).get(0);

        //New strategy: find a container whose distinct distance to this new CloudLet is the smallest.
        Container res = null;
        res = SelectContainerBasedOnStrategy(cl, Optional, CloudSim.LoadBalanceStrategy);
        if(res == null){
            res = SelectContainerBasedOnStrategy(cl, Optional, 100);
            Log.printLine("MONITOR: LOAD BALANCE STRATEGY NOT WORKS.");
        }
        else{
            Log.printLine("MONITOR: LOAD BALANCE STRATEGY WORKS.");
        }
        return res;
    }

    private Container SelectContainerBasedOnStrategy(ContainerCloudlet cl, Map<ContainerHost, List<Container>>Optional, int strategy){
        Container res = null;
        double MinDistance = Double.POSITIVE_INFINITY;
        for(Map.Entry<ContainerHost, List<Container>> entry : Optional.entrySet()){
            for(Container c : entry.getValue()){
                double distance = CalculateDistanceBetweenCloudLetAndContainer(cl, c, strategy);
                Log.printLine(CloudSim.clock() + " CLOUDLET LOCATION INFO: cloudLet id: " + cl.getCloudletId() +
                        " To container " + c.getId() + " distance:" + distance);
                if(MinDistance > distance){
                    res = c;
                    MinDistance = distance;
                }
            }
        }
        return res;
    }
    //待定
    private double CalculateDistanceBetweenCloudLetAndContainer(ContainerCloudlet cl, Container c, int strategy){
        c.updateContainerProcessing(CloudSim.clock(),
                getContainerAllocationPolicy().getContainerVm(c).getContainerScheduler().getAllocatedMipsForContainer(c));
        double finishTime;
        if(cl.GetPredictFinishTime() > 0 )
            finishTime = cl.GetPredictFinishTime();
        else{
            finishTime = predictFinishTime(cl);
            cl.SetPredictFinishTime(CloudSim.clock() + finishTime);
        }
        double diff; //The lowest distance is better for binding cloudLets.
        double CpuUtilization = 1 - (double)c.getAvailablePesNum() / c.getNumberOfPes();
        double MinOverTime = Double.POSITIVE_INFINITY, MaxOverTime = Double.NEGATIVE_INFINITY;
        boolean change_res = false;
        for(ResCloudlet ll : c.getContainerCloudletScheduler().getCloudletExecList()){
            ContainerCloudlet tmp =  (ContainerCloudlet)ll.getCloudlet();
            if(tmp.getContainerId() == c.getId()){
                change_res = true;
                double x =  CloudSim.ConvertLengthToTime(ll.getRemainingCloudletLength());
                Log.printLine(CloudSim.clock() + " REMAINING LENGTH INFO: CloudLet id: "
                        + ll.getCloudletId() + " remaining time: " + x);
                if(MinOverTime > x)
                    MinOverTime = x;
                if(MaxOverTime < x)
                    MaxOverTime = x;
            }
        }
        if(c.getContainerCloudletScheduler().getCloudletExecList().size() > 0 && change_res){
            if(finishTime >=  MinOverTime && finishTime <= MaxOverTime)
                diff = 0;
            else if(finishTime > MaxOverTime)
                diff = finishTime - MaxOverTime;
            else
                diff = MinOverTime - finishTime;
        }
        else{
            diff = 0;
        }
        if(strategy == 0) {
            // our strategy
            double TimeRatio = diff / 60 - 5;
            TimeRatio = 1 / (1 + Math.pow(Math.E, -1 * TimeRatio));
            double TimeWeight = 1 / (1.1 - TimeRatio);
            double LoadWeight =  1 / (1.1 - CpuUtilization);
            return TimeWeight * LoadWeight;
//            return TimeRatio * CpuUtilization;
        }
        else if(strategy == 1) //no load balance
            return diff;
        else
            return 1 / (1 - CpuUtilization); // load balance

    }



    private double predictFinishTime(ContainerCloudlet cl){
        List<Integer> HistoryHangOnTime = cl.HistoricalHangOnTimeList;
        int res = 0;
        for(int i = 0; i < HistoryHangOnTime.size(); i++)
            res += HistoryHangOnTime.get(i);
        return (double)res / HistoryHangOnTime.size();
    }


    protected void AccumulateCostOfContainer(Container con){
        double duration = CloudSim.clock() - con.getStartUpTime(); // we add this attribute.
        double cost = duration * getCharacteristics().getCostPerSecond();
        Log.formatLine(Log.Opr.ScaleDown,CloudSim.clock() + " Container " + con.getId() + " is to be removed. Its accumulated cost is: " + cost);
        con.setDestroyedTime(CloudSim.clock());
        con.setTotalCost(cost);
        con.setDataCenterId(getId());
        AllContainers.add(con);
        TotalContainerCost += cost;
    }


    @Override
    protected  void updateCloudletProcessing(){
        //do nothing, this part of original codes are devoted to do vm migrating, which is not required in this project.
    }


    @Override
    protected void processCloudletSubmit(SimEvent ev, boolean ack) {
//        updateCloudletProcessing();
        ContainerScalabilityCheck(ev);
        try {
            ContainerCloudlet cl = (ContainerCloudlet) ev.getData();
            // checks whether this Cloudlet has finished or not
            if (cl.isFinished()) {
                String name = CloudSim.getEntityName(cl.getUserId());
                Log.printConcatLine(getName(), ": Warning - Cloudlet #", cl.getCloudletId(), " owned by ", name,
                        " is already completed/finished.");
                Log.printLine("Therefore, it is not being executed again");
                Log.printLine();

                // NOTE: If a Cloudlet has finished, then it won't be processed.
                // So, if ack is required, this method sends back a result.
                // If ack is not required, this method don't send back a result.
                // Hence, this might cause CloudSim to be hanged since waiting
                // for this Cloudlet back.
                if (ack) {
                    int[] data = new int[3];
                    data[0] = getId();
                    data[1] = cl.getCloudletId();
                    data[2] = CloudSimTags.FALSE;

                    // unique tag = operation tag
                    int tag = CloudSimTags.CLOUDLET_SUBMIT_ACK;
                    sendNow(cl.getUserId(), tag, data);
                }
                sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);
                return;
            }

            // process this Cloudlet to this CloudResource
            cl.setResourceParameter(getId(), getCharacteristics().getCostPerSecond(), getCharacteristics()
                    .getCostPerBw());

            //Chris tuning container:
            int containerId = cl.getContainerId();

            int vmId = cl.getVmId();
            if(vmId < 0){
                Log.formatLine("Assign the cloudLet to the located container now.");
                getContainerList();
                if(ContainerList.getById(getContainerList(),containerId) == null){
                    Log.formatLine("Container %d, has not been created and allocated.");
                    return;
                }
                cl.setVmId(ContainerList.getById(getContainerList(),containerId).getVm().getId());
                vmId = cl.getVmId();
            }
            int userId = cl.getUserId();
            Log.formatLine(Log.Opr.InnerDatacenterAllocation, "Datacenter " + getId()
                    + " : Cloudlet submits succeed: cloudlet id: " + cl.getCloudletId() + " container id: " + containerId
                    + " in " + getName() +  " start time: " + cl.getExecStartTime());
            // time to transfer the files
            double fileTransferTime = predictFileTransferTime(cl.getRequiredFiles());

            ContainerHost host = getVmAllocationPolicy().getHost(vmId, userId);
            ContainerVm vm = host.getContainerVm(vmId, userId);

            Container container = vm.getContainer(containerId, userId);
            double estimatedFinishTime = container.getContainerCloudletScheduler().cloudletSubmit(cl, fileTransferTime);

            container.updateContainerProcessing(CloudSim.clock(),
                    getContainerAllocationPolicy().getContainerVm(container).getContainerScheduler().getAllocatedMipsForContainer(container));

            // if this cloudlet is in the exec queue
            if (estimatedFinishTime > 0.0 && !Double.isInfinite(estimatedFinishTime)) {
                estimatedFinishTime += fileTransferTime;
                send(getId(), estimatedFinishTime, CloudSimTags.VM_DATACENTER_EVENT);
            }

            if (ack) {
                int[] data = new int[3];
                data[0] = getId();
                data[1] = cl.getCloudletId();
                data[2] = CloudSimTags.TRUE;
                int tag = CloudSimTags.CLOUDLET_SUBMIT_ACK;
                sendNow(cl.getUserId(), tag, data);
            }

        } catch (ClassCastException c) {
            Log.printLine(String.format("%s.processCloudletSubmit(): ClassCastException error.", getName()));
            c.printStackTrace();
        } catch (Exception e) {
            Log.printLine(String.format("%s.processCloudletSubmit(): Exception error.", getName()));
            e.printStackTrace();
        }
        checkCloudletCompletion();
    }

    @Override
    protected void processVmCreate(SimEvent ev, boolean ack) {
        ContainerVm containerVm = (ContainerVm) ev.getData();
        boolean result = getVmAllocationPolicy().allocateHostForVm(containerVm);
        if (ack) {
            int[] data = new int[3];

            data[0] = getId();
            data[1] = containerVm.getId();

            if (result) {
                data[2] = CloudSimTags.TRUE;
            } else {
                data[2] = CloudSimTags.FALSE;
            }
            send(containerVm.getUserId(), CloudSim.getMinTimeBetweenEvents(), CloudSimTags.VM_CREATE_ACK, data);
        }

        if (result) {
            Log.formatLine(Log.Opr.Base, String.format("%s VM ID #%d has been allocated on Host #%d in Datacenter #%d",
                    CloudSim.clock(), containerVm.getId(), containerVm.getHost().getId(),containerVm.getHost().getDatacenter().getId()));
            getContainerVmList().add(containerVm);
            if (containerVm.isBeingInstantiated()) {
                containerVm.setBeingInstantiated(false);
            }
            containerVm.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(containerVm).getContainerVmScheduler()
                    .getAllocatedMipsForContainerVm(containerVm));
        }

    }

    protected void ContainerScalabilityCheck(SimEvent ev) {
        ContainerCloudlet cl = (ContainerCloudlet) ev.getData();
        double currentTime = CloudSim.clock();
        Log.formatLine(Log.Opr.InterDatacenterAllocation, "Container Scalability checking...");
        // if some time passed since last processing
        if (currentTime > getLastProcessTime() + getSchedulingInterval()) {
            //System.out.print(currentTime + " ");
            Log.formatLine(Log.Opr.InterDatacenterAllocation, "A new interval starts: current time, " + currentTime
                    + " diff from previous:" + (currentTime - getLastProcessTime()));
            sendNow(cl.getUserId(), containerCloudSimTags.CONTAINER_SCALABILITY_SYNC);
            setLastProcessTime(currentTime);

            for (PowerContainerHost host : this.<PowerContainerHost>getHostList()) {
                // Update the energy usage
                host.updateContainerVmsProcessing(currentTime); // inform VMs to update processing
            }
            double timeDiff = currentTime - getLastProcessTime();
            double timeFrameDatacenterEnergy = 0;
            for (ContainerHost h : getHostList()) {
                PowerContainerHost host = (PowerContainerHost)h;
                double previousUtilizationOfCpu = host.getPreviousUtilizationOfCpu();
                double utilizationOfCpu = host.getUtilizationOfCpu();
                double timeFrameHostEnergy = host.getEnergyLinearInterpolation(
                        previousUtilizationOfCpu,
                        utilizationOfCpu,
                        timeDiff);
                timeFrameDatacenterEnergy += timeFrameHostEnergy;
            }
            setPower(getPower() + timeFrameDatacenterEnergy);
        }

    }







    /**
     * Increment migration count.
     */
    protected void incrementContainerMigrationCount() {
        setContainerMigrationCount(getContainerMigrationCount() + 1);
    }

    /**
     * Increment migration count.
     */
    protected void incrementNewlyCreatedVmsCount() {
        setNewlyCreatedVms(getNewlyCreatedVms() + 1);
    }

    /**
     * Checks if is disable migrations.
     *
     * @return true, if is disable migrations
     */
    public boolean isDisableMigrations() {
        return disableMigrations;
    }

    /**
     * Sets the disable migrations.
     *
     * @param disableMigrations the new disable migrations
     */
    public void setDisableMigrations(boolean disableMigrations) {
        this.disableMigrations = disableMigrations;
    }


    public int getContainerMigrationCount() {
        return containerMigrationCount;
    }

    public void setContainerMigrationCount(int containerMigrationCount) {
        this.containerMigrationCount = containerMigrationCount;
    }

    public CostumeCSVWriter getNewlyCreatedVmWriter() {
        return newlyCreatedVmWriter;
    }

    public void setNewlyCreatedVmWriter(CostumeCSVWriter newlyCreatedVmWriter) {
        this.newlyCreatedVmWriter = newlyCreatedVmWriter;
    }

    public int getNewlyCreatedVms() {
        return newlyCreatedVms;
    }

    public void setNewlyCreatedVms(int newlyCreatedVms) {
        this.newlyCreatedVms = newlyCreatedVms;
    }

    public List<Integer> getNewlyCreatedVmsList() {
        return newlyCreatedVmsList;
    }

    public void setNewlyCreatedVmsList(List<Integer> newlyCreatedVmsList) {
        this.newlyCreatedVmsList = newlyCreatedVmsList;
    }
}

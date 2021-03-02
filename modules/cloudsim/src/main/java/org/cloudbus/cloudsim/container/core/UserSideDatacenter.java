package org.cloudbus.cloudsim.container.core;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.container.lists.ContainerList;
import org.cloudbus.cloudsim.container.resourceAllocators.ContainerAllocationPolicy;
import org.cloudbus.cloudsim.container.resourceAllocators.ContainerVmAllocationPolicy;
import org.cloudbus.cloudsim.container.utils.CostumeCSVWriter;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.core.predicates.PredicateType;

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
    private double[] DatacenterLocation;
    private static int ContainerPesNumber = -1;

    public UserSideDatacenter(String name, ContainerDatacenterCharacteristics characteristics,
                              ContainerVmAllocationPolicy vmAllocationPolicy,
                              ContainerAllocationPolicy containerAllocationPolicy, List<Storage> storageList,
                              double schedulingInterval, String experimentName, String logAddress,
                              double vmStartupDelay, double containerStartupDelay
                             ) throws Exception {
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
    }


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
        this.DatacenterLocation = location;
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
                GetLatestDatacenterInfoAndSendBack(ev);
                break;

            case containerCloudSimTags.CONTAINER_REMOVE:
                Container con = (Container)ev.getData();
                for(ContainerVm vm : getContainerVmList()){
                    if(vm.getId() == con.getVm().getId()){
                        vm.containerDestroy(con);
                        break;
                    }
                }
                break;

            default:
                processOtherEvent(ev);
                break;
        }
    }


    protected void GetLatestDatacenterInfoAndSendBack(SimEvent ev){
        double []data = new double[3];
        int UsedPesNumber  = 0;

        UsedPesNumber = getContainerList().size() * ContainerPesNumber;
        int TotalPesNumber = getHostList().size() * getHostList().get(0).getNumberOfPes();
        double CpuUtilization = (double)UsedPesNumber / (double)TotalPesNumber;
        double distance = Math.sqrt(DatacenterLocation[0] * DatacenterLocation[0] + DatacenterLocation[1] * DatacenterLocation[1]);
        data[0] = getId();
        data[1] = distance;
        data[2] = CpuUtilization;
        sendNow(ev.getSource(),ev.getTag(),data);
    }


    @Override
    protected  void updateCloudletProcessing(){
        //do nothing
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
                Log.formatLine("Assign the cloudlet to the located container now.");
                getContainerList();
                if(ContainerList.getById(getContainerList(),containerId) == null){
                    Log.formatLine("Container %d, has not been created and allocated.");
                    return;
                }
                cl.setVmId(ContainerList.getById(getContainerList(),containerId).getVm().getId());
                vmId = cl.getVmId();
            }
            int userId = cl.getUserId();
            Log.formatLine("chris note: cloudlet id: " + cl.getCloudletId() + " container id: " + containerId
                    + " VM id: " + cl.getVmId() +  " start time: " + cl.getExecStartTime());
            // time to transfer the files
            double fileTransferTime = predictFileTransferTime(cl.getRequiredFiles());

            ContainerHost host = getVmAllocationPolicy().getHost(vmId, userId);
            ContainerVm vm = host.getContainerVm(vmId, userId);

            Container container = vm.getContainer(containerId, userId);
            double estimatedFinishTime = container.getContainerCloudletScheduler().cloudletSubmit(cl, fileTransferTime);
            Log.formatLine("chris note: cloudlet id:" + cl.getCloudletId() + "estimated finish time: " + estimatedFinishTime);


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

                // unique tag = operation tag
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
            Log.formatLine(4, String.format("%s VM ID #%d has been allocated on Host #%d in Datacenter #%d",
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
        // if some time passed since last processing
        if (currentTime > getLastProcessTime() + getSchedulingInterval()) {
            //System.out.print(currentTime + " ");
            Log.formatLine(4, "A new interval starts: current time, " + currentTime + " diff:" + (currentTime - getLastProcessTime()));
            sendNow(cl.getUserId(), containerCloudSimTags.CONTAINER_SCALABILITY);
            setLastProcessTime(currentTime);
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

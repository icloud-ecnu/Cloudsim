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
    protected void processCloudletSubmit(SimEvent ev, boolean ack) {
        updateCloudletProcessing(ev);

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
            if(containerId < 0){
                Log.formatLine("Chris BINDING CLOUDLET: " + CloudSim.clock());
                sendNow(cl.getUserId(), containerCloudSimTags.BINDING_CLOUDLET, cl);
                return;
            }
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


    protected void updateCloudletProcessing(SimEvent ev) {
        ContainerCloudlet cl = (ContainerCloudlet) ev.getData();
        if (getCloudletSubmitted() == -1 || getCloudletSubmitted() == CloudSim.clock()) {
            CloudSim.cancelAll(getId(), new PredicateType(CloudSimTags.VM_DATACENTER_EVENT));
            schedule(getId(), getSchedulingInterval(), CloudSimTags.VM_DATACENTER_EVENT);
            return;
        }
        double currentTime = CloudSim.clock();

        // if some time passed since last processing
        if (currentTime > getLastProcessTime() + getSchedulingInterval()) {
            //System.out.print(currentTime + " ");
            Log.formatLine(4, "A new interval starts: current time, " + currentTime + " diff:" + (currentTime - getLastProcessTime()));
            sendNow(cl.getUserId(), containerCloudSimTags.CONTAINER_SCALABILITY);
            setLastProcessTime(currentTime);
        }

    }

    @Override
    protected void processVmCreate(SimEvent ev, boolean ack) {
        if (ev.getData() instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) ev.getData();
            ContainerVm containerVm = (ContainerVm) map.get("vm");
            ContainerHost host = (ContainerHost) map.get("host");
            boolean result = getVmAllocationPolicy().allocateHostForVm(containerVm, host);
//                set the containerVm in waiting state
            containerVm.setInWaiting(true);
//                containerVm.addMigratingInContainer((Container) map.get("container"));
            ack = true;
            if (ack) {
                Map<String, Object> data = new HashMap<String, Object>();
                data.put("vm", containerVm);
                data.put("result", containerVm);
                data.put("datacenterID", getId());

                if (result) {
                    data.put("result", CloudSimTags.TRUE);
                } else {
                    data.put("result", CloudSimTags.FALSE);
                }
                send(2, CloudSim.getMinTimeBetweenEvents(), containerCloudSimTags.VM_NEW_CREATE, data);
            }

            if (result) {
                Log.printLine(String.format("%s VM ID #%d is created on Host #%d", CloudSim.clock(), containerVm.getId(), host.getId()));
                incrementNewlyCreatedVmsCount();
                getContainerVmList().add(containerVm);


                if (containerVm.isBeingInstantiated()) {
                    containerVm.setBeingInstantiated(false);
                }

                containerVm.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(containerVm).getContainerVmScheduler()
                        .getAllocatedMipsForContainerVm(containerVm));
            }

        } else {
            super.processVmCreate(ev, ack);
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

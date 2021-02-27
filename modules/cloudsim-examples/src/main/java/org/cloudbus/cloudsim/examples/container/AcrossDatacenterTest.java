package org.cloudbus.cloudsim.examples.container;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.container.containerProvisioners.ContainerBwProvisionerSimple;
import org.cloudbus.cloudsim.container.containerProvisioners.ContainerPe;
import org.cloudbus.cloudsim.container.containerProvisioners.ContainerRamProvisionerSimple;
import org.cloudbus.cloudsim.container.containerProvisioners.CotainerPeProvisionerSimple;
import org.cloudbus.cloudsim.container.containerVmProvisioners.ContainerVmBwProvisionerSimple;
import org.cloudbus.cloudsim.container.containerVmProvisioners.ContainerVmPe;
import org.cloudbus.cloudsim.container.containerVmProvisioners.ContainerVmPeProvisionerSimple;
import org.cloudbus.cloudsim.container.containerVmProvisioners.ContainerVmRamProvisionerSimple;
import org.cloudbus.cloudsim.container.core.*;
import org.cloudbus.cloudsim.container.hostSelectionPolicies.HostSelectionPolicy;
import org.cloudbus.cloudsim.container.hostSelectionPolicies.HostSelectionPolicyFirstFit;
import org.cloudbus.cloudsim.container.resourceAllocatorMigrationEnabled.PowerContainerVmAllocationPolicyMigrationAbstractHostSelection;
import org.cloudbus.cloudsim.container.resourceAllocators.ContainerAllocationPolicy;
import org.cloudbus.cloudsim.container.resourceAllocators.ContainerVmAllocationPolicy;
import org.cloudbus.cloudsim.container.resourceAllocators.PowerContainerAllocationPolicySimple;
import org.cloudbus.cloudsim.container.schedulers.ContainerCloudletSchedulerDynamicWorkload;
import org.cloudbus.cloudsim.container.schedulers.ContainerSchedulerTimeSharedOverSubscription;
import org.cloudbus.cloudsim.container.schedulers.ContainerVmSchedulerTimeSharedOverSubscription;
import org.cloudbus.cloudsim.container.utils.IDs;
import org.cloudbus.cloudsim.container.vmSelectionPolicies.PowerContainerVmSelectionPolicy;
import org.cloudbus.cloudsim.container.vmSelectionPolicies.PowerContainerVmSelectionPolicyMaximumUsage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.text.DecimalFormat;
import java.util.*;

public class AcrossDatacenterTest {


    private static List<Cloudlet> cloudletList;

    private static List<ContainerVm> vmlist;

    private static List<ContainerHost> hostList;

    private static List<Container> containerlist;

    private static ContainerScalabilityBroker broker;

    //host description
    private static int ram = 2048; //host memory (MB)
    private static long storage = 1000000; //host storage
    private static int bw = 10000;
    private static int pesNumber = 20;
    private static int mips = 1000;
    /**
     * Creates main() to run this example
     */
    public static void main(String[] args) {

        Log.printLine("Starting Across Datacenter Container Create Test...");

        try {

            double overUtilizationThreshold = 0.80;
            double underUtilizationThreshold = 0.70;
            int overBookingFactor = 80;
            // First step: Initialize the CloudSim package. It should be called
            // before creating any entities.
            int num_user = 1;   // number of cloud users
            boolean trace_flag = false;  // mean trace events
            String logAddress = "~/Results";

            Log.set_log_level(2);
            Calendar calendar = Calendar.getInstance();
            // Initialize the CloudSim library
            CloudSim.init(num_user, calendar, trace_flag);
            hostList = new ArrayList<ContainerHost>();
            hostList = createHostList(4);

            broker = createBroker(overBookingFactor);
            int brokerId = broker.getId();

            vmlist = new ArrayList<ContainerVm>();
            vmlist = createVmList(brokerId, 4);

            containerlist = createContainerList(brokerId, 16);


            ContainerAllocationPolicy containerAllocationPolicy = new PowerContainerAllocationPolicySimple();
            PowerContainerVmSelectionPolicy vmSelectionPolicy = new PowerContainerVmSelectionPolicyMaximumUsage();
            HostSelectionPolicy hostSelectionPolicy = new HostSelectionPolicyFirstFit();
            ContainerVmAllocationPolicy vmAllocationPolicy = new
                    PowerContainerVmAllocationPolicyMigrationAbstractHostSelection(hostList, vmSelectionPolicy,
                    hostSelectionPolicy, overUtilizationThreshold, underUtilizationThreshold);


            // Second step: Create Datacenters
            //Datacenters are the resource providers in CloudSim. We need at list one of them to run a CloudSim simulation
            @SuppressWarnings("unused")
            PowerContainerDatacenter e1 = (PowerContainerDatacenter) createDatacenter("datacenter0",
                    PowerContainerDatacenterCM.class, hostList.subList(0,hostList.size() / 2), vmAllocationPolicy, containerAllocationPolicy,
                    getExperimentName("ACROSS_DATACENTER_TEST", String.valueOf(overBookingFactor)),
                    ConstantsExamples.SCHEDULING_INTERVAL, logAddress,
                    ConstantsExamples.VM_STARTTUP_DELAY, ConstantsExamples.CONTAINER_STARTTUP_DELAY);
            @SuppressWarnings("unused")
            PowerContainerDatacenter e2 = (PowerContainerDatacenter) createDatacenter("datacenter1",
                    PowerContainerDatacenterCM.class, hostList.subList(hostList.size() / 2, hostList.size()), vmAllocationPolicy, containerAllocationPolicy,
                    getExperimentName("ACROSS_DATACENTER_TEST", String.valueOf(overBookingFactor)),
                    ConstantsExamples.SCHEDULING_INTERVAL, logAddress,
                    ConstantsExamples.VM_STARTTUP_DELAY, ConstantsExamples.CONTAINER_STARTTUP_DELAY);
            //Third step: Create Broker



            //submit vm list to the broker
            broker.submitVmList(vmlist);

            broker.submitContainerList(containerlist);


            CloudSim.startSimulation();
            printContainerList(broker.getContainersCreatedList());
//            for(Container con : broker.getContainersCreatedList()){
//                Log.printLine("Container id: " + con.getId() + " Vm id: " + con.getVm().getId()
//                        + " Host id: " + con.getVm().getHost().getId() + " Datacenter Id: " + con.getVm().getHost().getDatacenter().getId());
//            }

            CloudSim.stopSimulation();
            Log.printLine("Across Datacenter Container Create finished!");
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
        }
    }

    private static String getExperimentName(String... args) {
        StringBuilder experimentName = new StringBuilder();

        for (int i = 0; i < args.length; ++i) {
            if (!args[i].isEmpty()) {
                if (i != 0) {
                    experimentName.append("_");
                }

                experimentName.append(args[i]);
            }
        }

        return experimentName.toString();
    }

    public static ContainerDatacenter createDatacenter(String name, Class<? extends ContainerDatacenter> datacenterClass,
                                                       List<ContainerHost> hostList,
                                                       ContainerVmAllocationPolicy vmAllocationPolicy,
                                                       ContainerAllocationPolicy containerAllocationPolicy,
                                                       String experimentName, double schedulingInterval, String logAddress, double VMStartupDelay,
                                                       double ContainerStartupDelay) throws Exception {
        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double time_zone = 10.0D;
        double cost = 3.0D;
        double costPerMem = 0.05D;
        double costPerStorage = 0.001D;
        double costPerBw = 0.0D;
        ContainerDatacenterCharacteristics characteristics = new
                ContainerDatacenterCharacteristics(arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage,
                costPerBw);
        ContainerDatacenter datacenter = new PowerContainerDatacenterCM(name, characteristics, vmAllocationPolicy,
                containerAllocationPolicy, new LinkedList<Storage>(), schedulingInterval, experimentName, logAddress,
                VMStartupDelay, ContainerStartupDelay);

        return datacenter;
    }

    public static List<ContainerHost> createHostList(int hostsNumber) {

        ArrayList<ContainerHost> hostList = new ArrayList<ContainerHost>();
        for (int i = 0; i < hostsNumber; ++i) {
            int hostType = i / (int) Math.ceil((double) hostsNumber / ConstantsExamples.HOST_TYPES);

            ArrayList<ContainerVmPe> peList = new ArrayList<ContainerVmPe>();
            for(int j = 0; j < pesNumber; j++)
                peList.add(new ContainerVmPe(j, new ContainerVmPeProvisionerSimple(mips)));


            hostList.add(new PowerContainerHostUtilizationHistory(IDs.pollId(ContainerHost.class),
                    new ContainerVmRamProvisionerSimple(ram),
                    new ContainerVmBwProvisionerSimple(1000000L), storage, peList,
                    new ContainerVmSchedulerTimeSharedOverSubscription(peList),
                    ConstantsExamples.HOST_POWER[hostType]));
        }

        return hostList;
    }

    private static ArrayList<ContainerVm> createVmList(int brokerId, int containerVmsNumber) {
        //VM description
        long size = 10000; //image size (MB)
        String vmm = "Xen"; //VMM name
        ArrayList<ContainerVm> containerVms = new ArrayList<ContainerVm>();

        for (int i = 0; i < containerVmsNumber; ++i) {
            ArrayList<ContainerPe> peList = new ArrayList<ContainerPe>();
            int vmType = i / (int) Math.ceil((double) containerVmsNumber / ConstantsExamples.VM_TYPES);
            for (int j = 0; j < pesNumber; ++j) {
                peList.add(new ContainerPe(j,
                        new CotainerPeProvisionerSimple(mips)));
            }
            containerVms.add(new PowerContainerVm(IDs.pollId(ContainerVm.class), brokerId,
                    mips, ram,
                    bw, size, "Xen",
                    new ContainerSchedulerTimeSharedOverSubscription(peList),
                    new ContainerRamProvisionerSimple(ram),
                    new ContainerBwProvisionerSimple(bw),
                    peList, ConstantsExamples.SCHEDULING_INTERVAL));
        }
        return containerVms;
    }

    public static List<Container> createContainerList(int brokerId, int containersNumber) {
        long size = 10000; //image size (MB)
        ArrayList<Container> containers = new ArrayList<Container>();
        for (int i = 0; i < containersNumber; ++i) {
            int containerType = i / (int) Math.ceil((double) containersNumber / ConstantsExamples.CONTAINER_TYPES);
            containers.add(new PowerContainer(IDs.pollId(Container.class),
                    brokerId,
                    mips/4,
                    pesNumber/4, ram/4, bw/4, 0L, "Xen",
                    new ContainerCloudletSchedulerDynamicWorkload(mips/4, pesNumber/4), ConstantsExamples.SCHEDULING_INTERVAL));
        }
        return containers;
    }



    private static ContainerScalabilityBroker createBroker(int overBookingFactor) {

        ContainerScalabilityBroker broker = null;

        try {
            Container c = new PowerContainer(IDs.pollId(Container.class),
                    -1,
                    (double) ConstantsExamples.CONTAINER_MIPS[0],
                    ConstantsExamples.CONTAINER_PES[0], ConstantsExamples.CONTAINER_RAM[0], ConstantsExamples.CONTAINER_BW, 0L, "Xen",
                    new ContainerCloudletSchedulerDynamicWorkload(ConstantsExamples.CONTAINER_MIPS[0], ConstantsExamples.CONTAINER_PES[0]), ConstantsExamples.SCHEDULING_INTERVAL);
            broker = new ContainerScalabilityBroker("Broker", overBookingFactor, c);
        } catch (Exception var2) {
            var2.printStackTrace();
            System.exit(0);
        }

        return broker;
    }




    //We strongly encourage users to develop their own broker policies, to submit vms and cloudlets according
    //to the specific rules of the simulated scenario
    private static DatacenterBroker createBroker(){

        DatacenterBroker broker = null;
        try {
            broker = new DatacenterBroker("Broker");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return broker;
    }

    /**
     * Prints the Cloudlet objects
     * @param list  list of Cloudlets
     */
    private static void printContainerList(List<Container> list) {
        int size = list.size();

        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Container ID" + indent + "VM ID" + indent + "HOST ID" + indent +
                "Datacenter ID" );

        DecimalFormat dft = new DecimalFormat("###.##");
        for (int i = 0; i < size; i++) {
            Container con = list.get(i);
            Log.printLine( indent + con.getId() + indent + indent  + indent
                    + con.getVm().getId() + indent + indent + con.getVm().getHost().getId()
                    + indent + indent + indent  + con.getVm().getHost().getDatacenter().getId());
            }


    }

}

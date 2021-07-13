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
import org.cloudbus.cloudsim.container.core.Container;
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
import org.cloudbus.cloudsim.examples.CloudletRequestDistribution.BaseRequestDistribution;
import org.cloudbus.cloudsim.examples.JavaSwingTool.Draw;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

import java.io.FileOutputStream;
import java.io.PrintStream;

public class ScalabilityAndLoadBalanceTest {


    private static List<ContainerCloudlet> cloudletList;

    private static List<ContainerVm> vmlist;

    private static List<PowerContainerHost> hostList;

    private static List<PowerContainer> containerlist;

    private static List<UserSideDatacenter> datacenterList;

    private static UserSideBroker broker;

    private static Map<Integer, ContainerDatacenterCharacteristics> local_characteristics;

    //host and vm description. We just concentrate on the containers.
    private static int ram = 204800; //host memory (MB)
    private static long storage = 1000000; //host storage
    private static int bw = 100000;
    private static int pesNumber = 2000; // PEs number in one host/VM, we assume vm == host
    private final static int mips = 10; //in one PE
    private static int ContainerNumPerVm = 50;
    private static int CloudletNumPerContainer = 5;
    private static int CloudletPesNum = 8;
//    private final static int CloudletPesNum = pesNumber / (ContainerNumPerVm * CloudletNumPerContainer);


    //The variables in cloudLet distribution
    private static int terminated_time = 6 * 60 * 60;
    private static int interval_length = 1200;
    private static int Poisson_lambda = 200;
    private static int Gaussian_mean = 60000;
    private static int Gaussian_var = 1000000;


    //Standard terminal output redirection path setting.
    private static String StdOutRedirectPath = "E://CloudSimOutput.txt";

    public static void main(String[] args) {

        Log.printLine("Starting Across Datacenter Container Create Test...");

        try {
            // First step: Initialize some variables.
            CloudSim.LinearScaleUpNum = 30;
            double overUtilizationThreshold = 0.80;
            double underUtilizationThreshold = 0.70;
            int overBookingFactor = 80;
            int DatacenterNumber = 10;
            int HostNumber = DatacenterNumber * 20;
            int VmNumber = HostNumber;
            int ContainerNumber = CloudSim.LinearScaleUpNum;
            int num_user = 1;   // number of cloud users
            boolean trace_flag = false;  // mean trace events
            String logAddress = "~/Results";
            local_characteristics = new HashMap<Integer, ContainerDatacenterCharacteristics>();
            //set the viewable logs, convi
//            Log.set_log_level(10);
//            Log.SetLogStdOut(Log.Opr.Base);
//            Log.SetLogStdOut(Log.Opr.ScaleUp);
//            Log.SetLogStdOut(Log.Opr.ScaleDown);
//            Log.SetLogStdOut(Log.Opr.Synchronization);
//            Log.SetLogStdOut(Log.Opr.InterDatacenterAllocation);
//            Log.SetLogStdOut(Log.Opr.InnerDatacenterAllocation);
//            Redirect the standard output to the specified file.
            PrintStream ps=new PrintStream(new FileOutputStream(StdOutRedirectPath));
            System.setOut(ps);

            CloudSim.LoadBalanceWeight = 0.3;
            CloudSim.TransmissionWeight = 0.7;
            Calendar calendar = Calendar.getInstance();
            // Initialize the CloudSim library
            CloudSim.init(num_user, calendar, trace_flag);
            //create host List
            hostList = new ArrayList<PowerContainerHost>();
            hostList = createHostList(HostNumber);
            //create broker.
            broker = createBroker(overBookingFactor);
            int brokerId = broker.getId();

            //create VMList, containerList.
            vmlist = new ArrayList<ContainerVm>();
            vmlist = createVmList(brokerId, VmNumber);
            containerlist = createContainerList(brokerId, ContainerNumber);


            //Set the default allocation policy, a new allocation strategy can be achieved by creating a new policy class.
            ContainerAllocationPolicy containerAllocationPolicy = new PowerContainerAllocationPolicySimple();
            PowerContainerVmSelectionPolicy vmSelectionPolicy = new PowerContainerVmSelectionPolicyMaximumUsage();
            HostSelectionPolicy hostSelectionPolicy = new HostSelectionPolicyFirstFit();



            //Create DatacenterList. Assign the hosts to each datacenter evenly .
            datacenterList = new ArrayList<UserSideDatacenter>();
            Random rand = new Random();
            for(int i = 0; i < DatacenterNumber; i++) {
                double[] location = new double[]{rand.nextInt(10000), rand.nextInt(10000)};
                List<PowerContainerHost> subhostList = hostList.subList(hostList.size() / DatacenterNumber * i, hostList.size() / DatacenterNumber * (i + 1));
                ContainerVmAllocationPolicy vmAllocationPolicy = new
                        PowerContainerVmAllocationPolicyMigrationAbstractHostSelection(subhostList, vmSelectionPolicy,
                        hostSelectionPolicy, overUtilizationThreshold, underUtilizationThreshold);
                UserSideDatacenter e1 = (UserSideDatacenter) createDatacenter("datacenter" + i,
                        PowerContainerDatacenterCM.class, subhostList,
                        vmAllocationPolicy, containerAllocationPolicy,
                        getExperimentName("IntervalScaleUpTest", String.valueOf(overBookingFactor)),
                        interval_length / 10, logAddress,
                        ConstantsExamples.VM_STARTTUP_DELAY, ConstantsExamples.CONTAINER_STARTTUP_DELAY,
                        location);
                datacenterList.add(e1);
            }

            broker.submitVmList(vmlist);
            broker.submitContainerList(containerlist);


            //Initialize the request distribution by setting the IntervalLength and some other parameters.
            Draw ex = new Draw(mips, CloudletPesNum);
            BaseRequestDistribution self_design_distribution = new BaseRequestDistribution(terminated_time, interval_length,
                    Poisson_lambda, Gaussian_mean, Gaussian_var);
            cloudletList = self_design_distribution.GetWorkloads(); //GetWorkloads()获取cloudletlist
            for(ContainerCloudlet cl : cloudletList){
                cl.setUserId(brokerId);
                Log.formatLine(Log.Opr.Base, "Initialization: cloudlet id: " + cl.getCloudletId()
                        + " length is " + cl.getCloudletLength());
            }
            ex.setInputDataPanel(self_design_distribution);



            // 需要整理今天的数据，把每个interval的信息放在一个cloudsim这个类的static变量，第n个放的是第n-1
            broker.submitCloudletList(cloudletList);   //截取，


            CloudSim.startSimulation();
            //calculate the total cost.
            printContainerList(UserSideDatacenter.AllContainers);
            List<ContainerCloudlet> newList = broker.getCloudletReceivedList();
            printCloudletList(newList);
            CloudSim.stopSimulation();
            Log.printLine("Cost: " +  UserSideDatacenter.TotalContainerCost);
            Log.printLine("Interval Scale Up Test finished!");
            ex.setEvaluationPanel(newList);
            // visualize the raw data
            EventQueue.invokeLater(() -> {
                //change the default font
                Font font = new Font("Arial", Font.PLAIN, 13);
                java.util.Enumeration keys = UIManager.getDefaults().keys();
                while (keys.hasMoreElements()) {
                    Object key = keys.nextElement();
                    Object value = UIManager.get(key);
                    if (value instanceof javax.swing.plaf.FontUIResource) {
                        UIManager.put(key, font);
                    }
                }
                ex.setVisible(true);
            });
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
                                                       List<PowerContainerHost> hostList,
                                                       ContainerVmAllocationPolicy vmAllocationPolicy,
                                                       ContainerAllocationPolicy containerAllocationPolicy,
                                                       String experimentName, double schedulingInterval, String logAddress, double VMStartupDelay,
                                                       double ContainerStartupDelay, double[] location) throws Exception {
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
                costPerBw, location);

        ContainerDatacenter datacenter = new UserSideDatacenter(name, characteristics, vmAllocationPolicy,
                containerAllocationPolicy, new LinkedList<Storage>(), schedulingInterval, experimentName, logAddress,
                VMStartupDelay, ContainerStartupDelay, location);
        local_characteristics.put(datacenter.getId(), characteristics);
        return datacenter;
    }

    public static List<PowerContainerHost> createHostList(int hostsNumber) {

        ArrayList<PowerContainerHost> hostList = new ArrayList<PowerContainerHost>();
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

    public static List<PowerContainer> createContainerList(int brokerId, int containersNumber) {
        long size = 10000; //image size (MB)
        ArrayList<PowerContainer> containers = new ArrayList<PowerContainer>();
        for (int i = 0; i < containersNumber; ++i) {
            int containerType = i / (int) Math.ceil((double) containersNumber / ConstantsExamples.CONTAINER_TYPES);
            containers.add(new PowerContainer(IDs.pollId(Container.class),
                    brokerId,
                    mips,
                    pesNumber/ContainerNumPerVm, ram/ContainerNumPerVm, bw/ContainerNumPerVm, 0L, "Xen",
                    new ContainerCloudletSchedulerDynamicWorkload(mips, CloudletPesNum),
                    ConstantsExamples.SCHEDULING_INTERVAL));
        }
        return containers;
    }



    private static UserSideBroker createBroker(int overBookingFactor) {

        UserSideBroker broker = null;

        try {
            double [] UserCoo = new double[]{0, 0};
            Container c =new PowerContainer(IDs.pollId(Container.class),
                    -1,
                    mips,
                    pesNumber/ContainerNumPerVm, ram/ContainerNumPerVm, bw/ContainerNumPerVm, 0L, "Xen",
                    new ContainerCloudletSchedulerDynamicWorkload(mips, CloudletPesNum),
                    ConstantsExamples.SCHEDULING_INTERVAL);
            broker = new UserSideBroker("Broker", overBookingFactor, c, UserCoo, interval_length);
        } catch (Exception var2) {
            var2.printStackTrace();
            System.exit(0);
        }
        return broker;
    }


    private static void printContainerList(List<Container> list) {
        List<Container> UnRemovedList = broker.getContainersCreatedList();
        for(int i = 0; i < UnRemovedList.size(); i++){
            Container con = UnRemovedList.get(i);
            con.setDestroyedTime(CloudSim.shutdownTime);
            con.setTotalCost((CloudSim.shutdownTime- con.getStartUpTime()) *  3.0D);
            list.add(con);
        }
        int size = list.size();
        String indent = "    ";
        Log.printLine();
        Log.printLine("==========CONTAINER INFO OUTPUT ==========");
        Log.printLine("Size: " + size);
        Log.printLine("Container ID" + indent  + " StartUpTime" + indent + "DestroyedTime" + indent + "Cost");
        DecimalFormat dft = new DecimalFormat("###.##");
        for (int i = 0; i < size; i++) {
            Container con = list.get(i);
            Log.printLine(String.format(indent + con.getId()
                    + indent + indent + dft.format(con.getStartUpTime())
                    + indent + indent + dft.format(con.getDestroyedTime())
                    + indent + indent + dft.format(con.getTotalCost()))
            );
        }

        Log.printLine(String.format("=======TOTAL COST=======\nFor container resources is: " + UserSideDatacenter.TotalContainerCost));

    }

    private static void printCloudletList(List<ContainerCloudlet> list) {
        int size = list.size();
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("The cloulet size is:" + size);
        ContainerCloudlet cloudlet;

        String indent = "    ";

        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent
                + "Datacenter ID" + indent + "Host ID" + indent
                + "VM ID" + indent + "Container ID" + indent
                + "Time" + indent
                + "Start Time" + indent
                + "Finish Time" + indent
                + "Delay Factor");
        //key: datacenterId   value:cloudlet number

        Map<Integer, Map<Integer, Integer>> Load = new HashMap<Integer, Map<Integer, Integer>>();
        DecimalFormat dft = new DecimalFormat("###.##");
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            Log.print(indent + cloudlet.getCloudletId() + indent + indent);
            if (cloudlet.getCloudletStatusString() == "Success") {
                Log.print("SUCCESS");
                if(Load.get(cloudlet.getResourceId()) == null)
                    Load.put(cloudlet.getResourceId(), new HashMap<Integer, Integer>());
                else{
                    int hostId = cloudlet.getHostId();
                    Map<Integer, Integer> hostMap = Load.get(cloudlet.getResourceId());
                    if(hostMap.get(hostId) == null)
                        hostMap.put(hostId, 1);
                    else
                        hostMap.put(hostId, hostMap.get(hostId) + 1);
                    Load.put(cloudlet.getResourceId(), hostMap);
                }
                Log.printLine(String.format(indent + indent + cloudlet.getResourceId()
                        + indent + indent + indent + cloudlet.getHostId()
                        + indent + indent + cloudlet.getVmId()
                        + indent + indent + cloudlet.getContainerId()
                        + indent + indent + indent  + dft.format(cloudlet.getActualCPUTime()) + indent
                        + indent + dft.format(cloudlet.getExecStartTime())
                        + indent + indent
                        + dft.format(cloudlet.getFinishTime())
                        + indent + indent
                        + dft.format(cloudlet.getDelayFactor())));
            }
        }

        for (Integer key : Load.keySet()) {
            Log.printLine();
            Map<Integer, Integer> hostMap = Load.get(key);
            int DatacenterSum = 0;
            for (Map.Entry<Integer, Integer> entry : hostMap.entrySet()) {
                DatacenterSum += entry.getValue();
                Log.printLine("Host ID = " + entry.getKey() + ", CloudLet NUMBER = " + entry.getValue());
            }
            Log.printLine("======= Datacenter ID:" + key + ", CloudLet NUMBER = " + DatacenterSum);
        }

        for(UserSideDatacenter d : datacenterList){
            double[] pos = UserSideDatacenter.getLocationById(d.getId());
            Log.printLine("Datacenter id: " + d.getId() + "  Pos: (" + pos[0] + ", " + pos[1] + ")");
        }

    }

}

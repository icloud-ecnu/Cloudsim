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
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

public class PredictationTest {


    private static List<List<ContainerCloudlet>> cloudlet2DXList;
    private static List<ContainerCloudlet> cloudletList;
    private static List<Map<Integer, Double>> IntervalDatacenterList;
    private static List<ContainerVm> vmlist;

    private static List<PowerContainerHost> hostList;

    private static List<PowerContainer> containerlist;

    private static List<UserSideDatacenter> datacenterList;

    private static UserSideBrokerAdvance broker;

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


    //The variables in cloudLet distribution
    private static int terminated_time = 24 * 60 * 60;
    private static int interval_length = 1200;
    private static int Poisson_lambda = 100;   //弹性100   负载1000
    private static int Gaussian_mean = 600;   //弹性600  负载60000
    private static int Gaussian_var = 100;  //弹性100  负载250000


    //Interval
    private static List<Integer> HistoricalContainerNumberInIntervals = new ArrayList<Integer>();


    //Standard terminal output redirection path setting.
    private static String StdOutRedirectPath = "./CloudSimOutput.txt";

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
            int ContainerNumber = 20;
            int num_user = 1;   // number of cloud users
            boolean trace_flag = false;  // mean trace events
            String logAddress = "~/Results";
            local_characteristics = new HashMap<Integer, ContainerDatacenterCharacteristics>();
            //set the viewable logs, convi
            Log.set_log_level(10);
//            Log.SetLogStdOut(Log.Opr.Base);
            Log.SetLogStdOut(Log.Opr.ScaleUp);
            Log.SetLogStdOut(Log.Opr.ScaleDown);
            Log.SetLogStdOut(Log.Opr.Synchronization);
            Log.SetLogStdOut(Log.Opr.InterDatacenterAllocation);
            Log.SetLogStdOut(Log.Opr.InnerDatacenterAllocation);
            //Redirect the standard output to the specified file.
            PrintStream ps=new PrintStream(new FileOutputStream(StdOutRedirectPath));
            System.setOut(ps);

            CloudSim.mips = mips;
            CloudSim.CloudletPesNum = CloudletPesNum;

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
                //We assume the datacenters distribute evenly due to simulating longitude.
                double[] location = new double[]{rand.nextInt(1000) + 1000 * i, 5000};
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


            //firstly used for generate simulation data.
//            GenerateSimulationData(ex, 100);
            ReadDataFromFiles();


            //whether to apply our strategy.
            CloudSim.initiative = true;
            CloudSim.LoadBalanceStrategy = 1;  //   0是本系统方案的负载均衡的  1是无负载均衡  大于1是最小连接
            CloudSim.startSimulation();
            //calculate the total cost.
            List<Container> res = UserSideDatacenter.AllContainers;
            printContainerList(res);
            //这里是处理容器列表信息的代码。
            class containerEvent implements Comparable<containerEvent>{
                double eventTime;
                int eventTag; //1代表创建，-1代表销毁
                int datacenterId;
                containerEvent(double eventTime, int eventTag, int datacenterId){
                    this.eventTime = eventTime;
                    this.eventTag = eventTag;
                    this.datacenterId = datacenterId;
                }
                @Override
                public int compareTo(containerEvent o) {
                    if(this.eventTime == o.eventTime)
                        return 0;
                    else
                        return this.eventTime < o.eventTime ? -1: 1;
                }
            }
            Map<Integer, List<containerEvent>> DataCenterEventList = new HashMap<Integer, List<containerEvent>>();
            List<containerEvent> eventList = new ArrayList<containerEvent>();
            for(Container con : res){
                containerEvent create = new containerEvent(con.getStartUpTime(), 1, con.getDataCenterId());
                containerEvent destroy = new containerEvent(con.getDestroyedTime(), -1, con.getDataCenterId());
                eventList.add(create);
                eventList.add(destroy);
            }
            Collections.sort(eventList);
            for(containerEvent e : eventList){
                if(DataCenterEventList.get(e.datacenterId) == null){
                    List<containerEvent> tmp = new ArrayList<containerEvent>();
                    tmp.add(e);
                    DataCenterEventList.put(e.datacenterId, tmp);
                }
                else{
                    DataCenterEventList.get(e.datacenterId).add(e);
                }
            }
            //第一层是数据中心id, 第二层是各个时间点对应的容器数量，map是无序的，如果需要排序可以将keyset提出来排序之后访问map获取值。
            Map<Integer, Map<Double, Integer>> DataCenterSeries = new HashMap<Integer, Map<Double, Integer>>();
            for (Map.Entry<Integer, List<containerEvent>> entry : DataCenterEventList.entrySet()) {
                int datacenterId = entry.getKey();
                Map<Double, Integer> TimeToContainerNumber = new HashMap<Double, Integer>();
                List<containerEvent> events = entry.getValue();
                int accumulated_number = 0;
                for(containerEvent e : events){
                        accumulated_number = accumulated_number + e.eventTag;
                        TimeToContainerNumber.put(e.eventTime, accumulated_number);
                }
                DataCenterSeries.put(datacenterId, TimeToContainerNumber);
            }


            List<ContainerCloudlet> newList = broker.getCloudletReceivedList();
            printCloudletList(newList);
            CloudSim.stopSimulation();
            Log.printLine("Cost: " +  UserSideDatacenter.TotalContainerCost);
            Log.printLine("Interval Scale Up Test finished!");

            ex.setEvaluationPanel(newList);
            ex.setResultPanel(DataCenterSeries);
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


    private static void ReadDataFromFiles(){
        try{
            FileInputStream fin = new FileInputStream("./submitCloudLetList.txt");
            ObjectInputStream sin = new ObjectInputStream(fin);
            Object obj = sin.readObject();
            cloudletList = (List<ContainerCloudlet>)obj;
            for(ContainerCloudlet cl : cloudletList){
                cl.setUserId(broker.getId());
                Log.formatLine(Log.Opr.Base, "Initialization: cloudlet id: " + cl.getCloudletId()
                        + " length is " + cl.getCloudletLength());
            }
            sin.close();
            Log.printLine("Broker: Submit " + cloudletList.size() + " cloudLets.");
            broker.submitCloudletList(cloudletList);


            //read intervalDatacenterList from one file and sync to the broker.
            ObjectInputStream sin1 = new ObjectInputStream( new FileInputStream("./intervalDatacenterList.txt"));
            IntervalDatacenterList = (List<Map<Integer, Double>>) sin1.readObject();
            broker.setIntervalDataCenters(IntervalDatacenterList);

            ObjectInputStream sin2 = new ObjectInputStream( new FileInputStream("./ContainerNumberInIntervals.txt"));
            HistoricalContainerNumberInIntervals = (List<Integer>) sin2.readObject();
            CloudSim.HistoricalContainerNumberInIntervals = HistoricalContainerNumberInIntervals;
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.printLine("File read occurs error.");
        }
    }

    private static void GenerateSimulationData(Draw ex, int Days) {
        //先生成一天的分布，
        try{
            BaseRequestDistribution BaseDistribution = new BaseRequestDistribution(terminated_time, interval_length,
                    Poisson_lambda, Gaussian_mean, Gaussian_var);
            cloudlet2DXList = BaseDistribution.Get2DxWorkloads();    //这个就是获取上面的BaseDistribution这个对象里，的一天的每个时间段的二维list
            cloudletList = BaseDistribution.GetWorkloads();      //这个就是一天所有的连接
            ex.setInputDataPanel(BaseDistribution);
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The fundamental data generation error.");
        }

        //Generate rational historical data.
        IntervalDatacenterList = new ArrayList<Map<Integer, Double>>();
        //In this simulation, the intervalDatacenterList will be formed completely.
        cloudletList = SimulateServalDays(cloudlet2DXList, 100);
        broker.setIntervalDataCenters(IntervalDatacenterList);
        try {
            FileOutputStream fout0 = new FileOutputStream("./intervalDatacenterList.txt");
            ObjectOutputStream sout0 = new ObjectOutputStream(fout0);
            sout0.writeObject(IntervalDatacenterList);
            sout0.flush();
            sout0.close();
            FileOutputStream fout1 = new FileOutputStream("./submitCloudLetList.txt");
            ObjectOutputStream sout = new ObjectOutputStream(fout1);
            sout.writeObject(cloudletList);
            sout.flush();
            sout.close();

            FileOutputStream fout2 = new FileOutputStream("./ContainerNumberInIntervals.txt");
            ObjectOutputStream sout2 = new ObjectOutputStream(fout2);
            sout2.writeObject(HistoricalContainerNumberInIntervals);
            sout2.flush();
            sout2.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.printLine("File stdout error.");
        }
    }

    private static List<ContainerCloudlet> SimulateServalDays(List<List<ContainerCloudlet>> workloads, int Days){    // workloads = cloudlet2DXList 二维的每个时间段的，每个连接数
        /*
        TWO GOALS:
            1. select three optimal DataCenters for each interval
            2. Generate data for some days and choose the ToSubmitCloudLetList

        */
        //1.To select three optimal DataCenters in each interval, we employ the naive statistics by comparing frequency
        for(List<ContainerCloudlet> IntervalWorkloads : workloads){
            List<Integer> DatacenterChosenTime = new ArrayList<Integer>(workloads.size());
            for(int i = 0; i < datacenterList.size() * 2; i++)DatacenterChosenTime.add(0);
            for(ContainerCloudlet connection : IntervalWorkloads){
                double minDistance = Double.POSITIVE_INFINITY;
                int OptimalDatacenterID = -1;
                for(UserSideDatacenter d : datacenterList){
                    int diffX = (int)(connection.getCallPositionX() - UserSideDatacenter.getLocationById(d.getId())[0]);
                    int diffY = (int)(connection.getCallPositionY() - UserSideDatacenter.getLocationById(d.getId())[1]);
                    double CurDistance = Math.sqrt(diffX * diffX + diffY * diffY);
                    if(CurDistance < minDistance){
                        minDistance = CurDistance;
                        OptimalDatacenterID = d.getId();
                    }
                }
                DatacenterChosenTime.set(OptimalDatacenterID, DatacenterChosenTime.get(OptimalDatacenterID) + 1);
            }
            //select three DataCenters with the highest frequency being nearest.
            Map<Integer, Double> IntervalBackUpDataCenters = new HashMap<>();
            for(int i = 0; i < 6; i++){
                int OptimalId = -1, Times = Integer.MIN_VALUE;
                for(int j = 0; j < DatacenterChosenTime.size(); j++){
                    if(DatacenterChosenTime.get(j) > Times){
                        Times = DatacenterChosenTime.get(j);
                        OptimalId = j;
                    }
                }
                /*
                Key: DataCenterID ---> Value: the ratio of all connections in this interval.
                The frequency is prepared to do the advanced ScaleUp operation.
                */
                if(DatacenterChosenTime.get(OptimalId) > 0)
                    IntervalBackUpDataCenters.put(OptimalId, 1.0 * DatacenterChosenTime.get(OptimalId) / IntervalWorkloads.size());
                DatacenterChosenTime.set(OptimalId, 0); //remove the optimal datacenter to find the suboptimal one.
            }
            IntervalDatacenterList.add(IntervalBackUpDataCenters);  //选取了3个数据中心
        }

        //2.
        List <ContainerCloudlet> ToSubmitCloudletsList = new ArrayList<ContainerCloudlet>(); //
        for(int i = 0;  i < Days; i++){
            ToSubmitCloudletsList.clear();
            Random RandForNumber = new Random();
            /*
            In each day, we randomly select the 1/3 ratio of CloudLets in each time interval to be submitted.
            We suppose that:
            1. The variable SELECT obeys a normal distribution whose mean and variance are len/3 and 100, respectively.
            2. The HangOnTime for one specific user (CloudLet) obeys a normal distribution.
                mean: generated by base distribution var default: 30
             */
            for(int j = 0; j < workloads.size(); j++){   //workloads = cloudlet2Dlist
                List<ContainerCloudlet> ConnectionInOneInterval = workloads.get(j);
                int len = ConnectionInOneInterval.size();   //一个时间阶段里所有连接的个数
                int select = len / 2 + (int)(RandForNumber.nextGaussian() * Math.sqrt(Gaussian_var)), remaining = len;
                if(select <= 0) {
                    HistoricalContainerNumberInIntervals.add(0);
                    continue;
                }
                else{
                    HistoricalContainerNumberInIntervals.add(select / 4 + 1 );
                    Random randForSelection = new Random();
                    Random randForGaussian = new Random();
                    for (int k = 0; k < len; k++) {
                        if (randForSelection.nextInt(len) % remaining < select) {
                            //select one connection and reassign the CloudLet length.
                            ContainerCloudlet x = ConnectionInOneInterval.get(k);
                            int mean = x.getHistoricalHangOnTimeList().get(0);
                            double variance = 30;
                            int NewHangOnTime = (int)(randForGaussian.nextGaussian() * Math.sqrt(variance) + mean);
                            x.UpdateHistoricalHangOnTimeList(NewHangOnTime);
                            x.setCloudletLength(NewHangOnTime * mips * CloudletPesNum);
                            ToSubmitCloudletsList.add(x);
                            select--;
                        }
                        remaining--;
                    }
                }
            }
        }

        /*
         Goal 3: store the historical cloudLet number for each interval in cloudsim for prediction.
         */

        CloudSim.HistoricalContainerNumberInIntervals = HistoricalContainerNumberInIntervals;

        return ToSubmitCloudletsList;
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



    private static UserSideBrokerAdvance createBroker(int overBookingFactor) {

        UserSideBrokerAdvance broker = null;

        try {
            double [] UserCoo = new double[]{0, 0};
            Container c =new PowerContainer(IDs.pollId(Container.class),
                    -1,
                    mips,
                    pesNumber/ContainerNumPerVm, ram/ContainerNumPerVm, bw/ContainerNumPerVm, 0L, "Xen",
                    new ContainerCloudletSchedulerDynamicWorkload(mips, CloudletPesNum),
                    ConstantsExamples.SCHEDULING_INTERVAL);
            broker = new UserSideBrokerAdvance("Broker", overBookingFactor, c, UserCoo, interval_length);
        } catch (Exception var2) {
            var2.printStackTrace();
            System.exit(0);
        }
        return broker;
    }


    private static void printContainerList(List<Container> list) {
        int size = list.size();
        Collections.sort(list);
        String indent = "    ";
        Log.printLine();
        Log.printLine("==========CONTAINER INFO OUTPUT ==========");
        Log.printLine("Container ID" + indent  + "DataCenter ID" + " StartUpTime" + indent + "DestroyedTime" + indent + "Cost");
        DecimalFormat dft = new DecimalFormat("###.##");
        for (int i = 0; i < size; i++) {
            Container con = list.get(i);
            Log.printLine(String.format(indent + con.getId()
                    + indent +indent + dft.format(con.getDataCenterId())
                    + indent + indent + dft.format(con.getStartUpTime())
                    + indent + indent + dft.format(con.getDestroyedTime())
                    + indent + indent + dft.format(con.getTotalCost()))
            );
        }
        Log.printLine("Size: " + size);
        Log.printLine(String.format("=======TOTAL COST=======\nFor container resources is: " + UserSideDatacenter.TotalContainerCost));

    }

    private static void printCloudletList(List<ContainerCloudlet> list) {
        int size = list.size();
        Collections.sort(list);
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
                + "Predicted Finish Time" + indent
                + "Delay Factor");
        //key: datacenterId   value:cloudlet number
        double total_delay = 0, cnt = 0;
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
                        + indent + indent + indent + cloudlet.getVmId()
                        + indent + indent + cloudlet.getContainerId()
                        + indent + indent + indent  + dft.format(cloudlet.getActualCPUTime()) + indent
                        + indent + dft.format(cloudlet.getExecStartTime())
                        + indent + indent
                        + dft.format(cloudlet.getFinishTime())
                        + indent + indent + indent
                        + dft.format(cloudlet.GetPredictFinishTime())
                        + indent + indent + indent + indent + indent
                        + dft.format(cloudlet.getDelayFactor())));
                total_delay += cloudlet.getDelayFactor();
                cnt++;
            }
        }
        Log.printLine("======The average delay is: " + (total_delay / cnt));

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
            double[] pos = UserSideDatacenter.getLocationById(d.getId());;
            Log.printLine("Datacenter id: " + d.getId() + "  Pos: (" + pos[0] + ", " + pos[1] + ")");
        }

    }

}

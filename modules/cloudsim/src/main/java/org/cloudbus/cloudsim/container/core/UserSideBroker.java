package org.cloudbus.cloudsim.container.core;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.lists.ContainerList;
import org.cloudbus.cloudsim.container.utils.IDs;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;

import java.util.*;

public class UserSideBroker extends ContainerDatacenterBroker{

    private Container const_container;
    private double[] coordinate = new double[]{0, 0};




    /**
     * Created a new Broker object.
     *
     * @param name to be associated with this entity (as required by Sim_entity class from
     *                          simjava package)
     * @param overBookingfactor
     * @throws Exception the exception
     * @pre name != null
     * @post $none
     */
    public UserSideBroker(String name, double overBookingfactor, Container e) throws Exception {
        super(name, overBookingfactor);
        this.const_container = e;
    }

    public UserSideBroker(String name, double overBookingfactor, Container e, double[] coordinate) throws Exception {
        super(name, overBookingfactor);
        this.const_container = e;
        this.coordinate[0] = coordinate[0];
        this.coordinate[1] = coordinate[1];
    }

    public void setCoordinate(double[] a) {
        this.coordinate[0] = a[0];
        this.coordinate[1] = a[1];
    }

    public double[] getCoordinate(){
        return this.coordinate;
    }


    public int SelectDatacenter(ContainerCloudlet cl) {

        Map<Integer, ContainerDatacenterCharacteristics> DatacenterCharacteristicsList = getDatacenterCharacteristicsList();
        int OptimalDatacenter = getDatacenterIdsList().get(0);
        double max_value = Double.MAX_VALUE;
        for (Map.Entry<Integer, ContainerDatacenterCharacteristics> entry : DatacenterCharacteristicsList.entrySet()){
            int datacenterId = entry.getKey();
            ContainerDatacenterCharacteristics characteristic = entry.getValue();
            if(characteristic.getHostWithFreePe(cl.getNumberOfPes()) == null) {
                continue;
            }
            double CpuUtilization = characteristic.getNumberOfFreePes() / characteristic.getNumberOfPes();
            double MemoryUtilization, BwUtilization;
            double AvailableMemory = 0, AvailableBw = 0;
            double totalBW = 0, totalMemory = 0;
            for(ContainerHost host : characteristic.getHostList()){
                AvailableMemory += host.getAvailableRam();
                totalMemory += host.getRam();
                AvailableBw += host.getAvailableBw();
                totalBW += host.getBw();
            }
            MemoryUtilization = AvailableMemory / totalMemory;
            BwUtilization = AvailableBw / totalBW;
            //Calculate the transmission delay to the optional datacenters.
            double []loc = characteristic.getLocation();
            double TransmissionDistance = Math.sqrt(loc[0] * loc[0] + loc[1] * loc[1]);
            double ComprehensiveRes = FactorCombination(TransmissionDistance, CpuUtilization, MemoryUtilization, BwUtilization);
            if(max_value > ComprehensiveRes){
                max_value = ComprehensiveRes;
                OptimalDatacenter = datacenterId;
            }
        }
        return OptimalDatacenter;
    }

    public double FactorCombination(double TransmissionDistance, double CpuUtilization, double MemoryUtilization, double BwUtilization) {
        double DelayNormalization = TransmissionDistance / 1000 * 1.14;
        return 0.5 * DelayNormalization +  0.3 * CpuUtilization + 0.1 * MemoryUtilization + 0.1 * BwUtilization;
    }




    public void ProcessBindingBeforeSubmit(SimEvent ev){
        ContainerCloudlet cl = (ContainerCloudlet) ev.getData();
        boolean binding = false;
        int DestDatacenterId = SelectDatacenter(cl);
        Log.formatLine(4, "Cloudlet id: " + cl.getCloudletId() + " Destination Datacenter Id: " + DestDatacenterId);


            // Log.formatLine(2, "chris note: created containers size: " + getContainersCreatedList().size());
//        for(Container container : getContainersCreatedList()){
//            Log.formatLine(4, "Cloudlet id: " + cl.getCloudletId()
//                    + " Container id: " + container.getId() + "  Vm id: " + container.getVm().getId()
//                    + " host id: " + container.getVm().getHost().getId()
//                    + " datacenter id: " + container.getVm().getHost().getDatacenter().getId());
//            ContainerDatacenter d0 = container.getVm().getHost().getDatacenter();
           //Container Selection can be divided into two steps. First Step: select an appropriate datacenter and then select a container.

            //First step: datacenter

//        }
    }


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
            case containerCloudSimTags.BINDING_CLOUDLET:
                ProcessBindingBeforeSubmit(ev);
                break;
            case containerCloudSimTags.CONTAINER_SCALABILITY:
                ProcessContainerScalability(ev);
                break;
            // other unknown tags are processed by this method
            default:
                processOtherEvent(ev);
                break;
        }
    }


    public void ProcessContainerScalability(SimEvent ev){

    }

//    public void processContainerCreate(ContainerCloudlet cl){
//        List<Container> l = new ArrayList<Container>(1);
//        Container con = new Container(IDs.pollId(Container.class), getId(), const_container.getWorkloadTotalMips(),
//                                                const_container.getNumberOfPes() - cl.getNumberOfPes(), //a little change to initialize
//                                                 (int)const_container.getRam(),
//                                                const_container.getBw(), const_container.getSize(),
//                                                const_container.getContainerManager(), const_container.getContainerCloudletScheduler(),
//                                                const_container.getSchedulingInterval());
//        cl.setContainerId(con.getId());
//        l.add(con);
//        submitContainerList(l);
//        Log.formatLine(2, "chris note: Binding Cloudlet " + cl.getCloudletId() + "  to the new container " + con.getId()
//                + " BUT it has not been allocated.");
//        //how to place container to VM. cannot invoke the non-static methods. unreasonable.
//        sendNow(datacenterIdsList.get(0), containerCloudSimTags.CONTAINER_SUBMIT, l);
//    }

//    public void ProcessBindingBeforeSubmit(SimEvent ev){
//        ContainerCloudlet cl = (ContainerCloudlet) ev.getData();
//        boolean binding = false;
//       // Log.formatLine(2, "chris note: created containers size: " + getContainersCreatedList().size());
//        for(Container container : getContainersCreatedList()){
//            if (container.getAvailablePesNum() >= cl.getNumberOfPes()) {
//                binding = true;
//                cl.setContainerId(container.getId());
//                Log.formatLine(2, "chris note: Container id: " + container.getId() + " has "
//                        +  container.getAvailablePesNum() + " PEs <vs> requests "  + cl.getNumberOfPes()
//                                + " PEs. So bind Cloudlet " + cl.getCloudletId() + "  to container " + container.getId());
//                cl.setVmId(container.getVm().getId());
//                //subtract the available PEs number.
//                container.setAvailablePesNum(container.getAvailablePesNum() - cl.getNumberOfPes());
//                break;
//            }
//        }
//        if(!binding){
//            //Log.formatLine(2, "Chris note: None of containers satisfy the request, create a new container.");
//            processContainerCreate(cl);
//        }
//        send(getDatacenterIdsList().get(0), CloudSim.getMinTimeBetweenEvents(),  CloudSimTags.CLOUDLET_SUBMIT, cl);
//    }


    @Override
    protected void submitCloudlets(){
        List<ContainerCloudlet> successfullySubmitted = new ArrayList<>();

        for (ContainerCloudlet clt : getCloudletList()) {
            send(getDatacenterIdsList().get(0), clt.getExecStartTime(), CloudSimTags.CLOUDLET_SUBMIT, clt);
            cloudletsSubmitted++;
            getCloudletSubmittedList().add(clt);
            successfullySubmitted.add(clt);
        }
        getCloudletList().removeAll(successfullySubmitted);
        successfullySubmitted.clear();
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
                Log.formatLine(2, "Chris note: Scale down. Current size: " + getContainersCreatedList().size()
                + " Remove container ID: " + rmId);
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






}




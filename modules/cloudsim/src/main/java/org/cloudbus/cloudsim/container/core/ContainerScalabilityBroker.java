package org.cloudbus.cloudsim.container.core;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.container.utils.IDs;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;

import java.util.ArrayList;
import java.util.List;

public class ContainerScalabilityBroker extends ContainerDatacenterBroker{

    private Container const_container;


    /**
     * Created a new Broker object.
     *
     * @param namename to be associated with this entity (as required by Sim_entity class from
     *                          simjava package)
     * @param overBookingfactor
     * @throws Exception the exception
     * @pre name != null
     * @post $none
     */
    public ContainerScalabilityBroker(String name, double overBookingfactor, Container e) throws Exception {
        super(name, overBookingfactor);
        this.const_container = e;
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
            // other unknown tags are processed by this method
            default:
                processOtherEvent(ev);
                break;
        }
    }





//
    public void processContainerCreate(ContainerCloudlet cl){
        List<Container> l = new ArrayList<Container>(1);
        l.add(new Container(IDs.pollId(Container.class), getId(), const_container.getWorkloadTotalMips()
                                                ,const_container.getNumberOfPes(), (int)const_container.getRam(),
                                                const_container.getBw(), const_container.getSize()
                                                , const_container.getContainerManager(), const_container.getContainerCloudletScheduler()
                                                , const_container.getSchedulingInterval()));
        //how to place container to VM. cannot invoke the non-static methods. unreasonable.
        sendNow(datacenterIdsList.get(0), containerCloudSimTags.CONTAINER_SUBMIT, l);
    }

    public void ProcessBindingBeforeSubmit(SimEvent ev){
        ContainerCloudlet cl = (ContainerCloudlet) ev.getData();
        boolean binding = false;
        for(Container container : getContainerList()){
            if (container.getAvailablePesNum() > cl.getNumberOfPes()) {
                binding = true;
                bindCloudletToContainer(cl.getCloudletId(), container.getId());
                bindCloudletToVm(cl.getCloudletId(), container.getVm().getId());
            }
        }
        if(!binding){
            processContainerCreate(cl);
//            if(new_container == null){
//                Log.formatLine("No more space left to scale up.");
//                return false;
//            }
//            else{
//                cl.setContainerId(new_container.getId());
//                cl.setVmId(new_container.getVm().getId());
//            }
        }
    }

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






}




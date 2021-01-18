package org.cloudbus.cloudsim.container.core;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;

import java.util.ArrayList;
import java.util.List;

public class ContainerScalabilityBroker extends ContainerDatacenterBroker{
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
    public ContainerScalabilityBroker(String name, double overBookingfactor) throws Exception {
        super(name, overBookingfactor);
    }

    public void BindingBeforeSubmit(Cloudlet cl){
        boolean binding = False;
        for(Container container : containerList){
            if (container.getAvailablePesNum() > cl.getNumberOfPes()) {
                binding = True;
                bindCloudletToContainer(cl.getCloudletId(), container.getId());
                bindCloudletToVm(cl.getCloudletId(), container.getVm().getId());
            }
        }
        if(!binding){
            processContainerCreate(new SimEvent());
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

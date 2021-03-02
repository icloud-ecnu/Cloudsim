package org.cloudbus.cloudsim.examples.container;

import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.power.models.PowerModelSpecPowerHpProLiantMl110G4Xeon3040;
import org.cloudbus.cloudsim.power.models.PowerModelSpecPowerHpProLiantMl110G5Xeon3075;
import org.cloudbus.cloudsim.power.models.PowerModelSpecPowerIbmX3550XeonX5670;

/**
 * In this class the specifications of the Cloudlets, Containers, VMs and Hosts are coded.
 * Regarding to the hosts, the powermodel of each type of the hosts are all included in this class.
 */
public class ConstantsExamples {

    /**
     * Simulation  parameters including the interval and limit
     */
    public static final boolean ENABLE_OUTPUT = true;
    public static final boolean OUTPUT_CSV = false;
    public static final double SCHEDULING_INTERVAL = 12D;
    public static final double SIMULATION_LIMIT = 87400.0D;
    /**
     * Cloudlet specs
     */
    public static final int CLOUDLET_LENGTH = 40;
    public static final int CLOUDLET_PES = 10;

    /**
     * Startup delay for VMs and the containers are mentioned here.
     */
    public static final double CONTAINER_STARTTUP_DELAY = 20;//the amount is in seconds
    public static final double VM_STARTTUP_DELAY = 0.4;//the amoun is in seconds

    /**
     * The available virtual machine types along with the specs.
     */

    public static final int VM_TYPES = 1;
    public static final double[] VM_MIPS = new double[]{1000};
    public static final int[] VM_PES = new int[]{2000};
    public static final float[] VM_RAM = new float[] {(float)10240};//**MB*
    public static final int VM_BW = 10000;
    public static final int VM_SIZE = 20000;

    /**
     * The available types of container along with the specs.
     */

    public static final int CONTAINER_TYPES = 1;
    //chris add:
    public static final int NUMBER_CONTAINERS = 4;
    public static final int[] CONTAINER_MIPS = new int[]{10};
    public static final int[] CONTAINER_PES = new int[]{20};
    public static final int[] CONTAINER_RAM = new int[]{102};
    public static final int CONTAINER_BW = 100;

    /**
     * The available types of hosts along with the specs.
     */

    public static final int HOST_TYPES = 1;
    public static final int[] HOST_MIPS = new int[]{1001};
    public static final int[] HOST_PES = new int[]{2001};
    public static final int[] HOST_RAM = new int[]{10241};
    public static final int HOST_BW = 1000001;
    public static final int HOST_STORAGE = 1000000;
    public static final PowerModel[] HOST_POWER = new PowerModel[]{new PowerModelSpecPowerHpProLiantMl110G4Xeon3040(),
            new PowerModelSpecPowerHpProLiantMl110G5Xeon3075(), new PowerModelSpecPowerIbmX3550XeonX5670()};

    /**
     * The population of hosts, containers, and VMs are specified.
     * The containers population is equal to the cloudlets population as each cloudlet is mapped to each container.
     * However, depending on the simualtion scenario the container's population can also be different from cloudlet's
     * population.
     */


    public static final int NUMBER_HOSTS = 3;
    public static final int NUMBER_VMS = 3;
    public static final int NUMBER_CLOUDLETS = 4;

//-----------------------The Addresses


//    public static String logAddress;
//
//    public String getLogAddress() {
//        return logAddress;
//    }
//
//    public void setLogAddress(String logAddress) {
//        this.logAddress = logAddress;
//    }
//
    public ConstantsExamples() {

    }
}

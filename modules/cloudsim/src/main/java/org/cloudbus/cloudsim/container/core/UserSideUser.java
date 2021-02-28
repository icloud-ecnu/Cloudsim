package org.cloudbus.cloudsim.container.core;

public class UserSideUser {
    private int brokerId;
    private double[] coordinate = new double[]{2};

    public UserSideUser(){

    }

    public UserSideUser(int brokerId, double[] coordinate){
        this.brokerId = brokerId;
        this.coordinate[0] = coordinate[0];
        this.coordinate[1] = coordinate[1];
    }

    public void setBrokerId(int brokerId) {
        this.brokerId = brokerId;
    }

    public int getBrokerId() {
        return this.brokerId;
    }

    public void setCoordinate(double[] a) {
        this.coordinate[0] = a[0];
        this.coordinate[1] = a[1];
    }

    public double[] getCoordinate(){
        return this.coordinate;
    }
}

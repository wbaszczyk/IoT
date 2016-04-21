package com.iot.device.management.iot.device;

/**
 * Created by sneez on 21.04.16.
 */
public class DeviceProperties {

    private String title;
    private String ipAddress;
    private Integer port;
    private String mac;

    public DeviceProperties(String title, String ipAddress, Integer port, String mac) {
        this.title = title;
        this.ipAddress = ipAddress;
        this.port = port;
        this.mac = mac;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    @Override
    public String toString() {
        return "DeviceProperties{" +
                "title='" + title + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", port=" + port +
                ", mac='" + mac + '\'' +
                '}';
    }
}

package com.iot.device.management.iot;

import com.iot.device.management.iot.device.DeviceProperties;

/**
 * Created by sneez on 21.04.16.
 */
public interface RegisterDeviceCallback {

    void registerDevice(String uuid, DeviceProperties deviceProperties);

    void notifyAboutProximityDetector(String uuid, Integer proximity);

}

package org.pluggables.app.api;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;

import java.util.ArrayList;
import java.util.Collection;

public interface DevicePluggables {

    /**
     * Creates a new Device pluggables
     *
     * @param deviceId the device ID
     * @return a Device pluggables instance if the operation is successful; null otherwise
     */
     DevicePluggablesData createDevicePluggables(DeviceId deviceId);

    /**
     * Provide all Device pluggables
     * @return all Device pluggables
     */
    Collection<DevicePluggablesData> allDevicePluggables();

    /**
     * Configure the Device pluggables and create a connection
     * @param devicePluggableDatas pluggables configuration that as to be configured
     * @param connetPoints identify the connect point for enstablishing the connection
     * @param connectioname identify the connection
     */
    void configureDevicePluggable(ArrayList<DevicePluggablesData> devicePluggableDatas,
                                  ArrayList<ConnectPoint> connetPoints,String connectioname) throws InterruptedException;

    /**
     * Disable the pluggables associated to a certain connection
     * @param connectioname identify the connection
     */
    void deleteConfigureDevicePluggable(String connectioname) throws InterruptedException;

}

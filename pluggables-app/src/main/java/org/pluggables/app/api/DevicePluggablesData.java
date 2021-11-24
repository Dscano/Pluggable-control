package org.pluggables.app.api;
import org.onosproject.net.DeviceId;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;
import com.google.common.base.MoreObjects;

public class DevicePluggablesData {

        protected DeviceId deviceId;
        protected ArrayList<PluggableData> pluggables;

        /**
         * Constructs a DevicePluggablesData data by given a devicePluggablesData.
         *  @param deviceId the given deviceId
         *  @param plugs a given list of pluggable
         */
        private DevicePluggablesData (DeviceId deviceId, ArrayList <PluggableData> plugs) {

            this.deviceId = deviceId;
            this.pluggables = new ArrayList<>();
            pluggables.addAll(plugs);
        }

        /**
         * Creates a  Device Pluggables Data by given devideId.
         * @param deviceId the given deviceId
         * @param pluggables the given list PluggableData
         * @return the device pluggables data
         */
        public static DevicePluggablesData of(DeviceId deviceId, ArrayList <PluggableData> pluggables) {
            requireNonNull(deviceId);
            requireNonNull(pluggables);
            return new DevicePluggablesData(deviceId, pluggables);
        }

        /**
         * Creates a copy of Device pluggables data.
         * @param devPluggablesData the devPluggable data
         * @return the copy of the DevicePluggableData
         */
        public static DevicePluggablesData of(DevicePluggablesData devPluggablesData) {
            requireNonNull(devPluggablesData);
            DevicePluggablesData devPluggablesDataCopy = new DevicePluggablesData(devPluggablesData.getDeviceId(),
                     devPluggablesData.getPluggables());
            return devPluggablesData;
        }

        /**
         * Gets device ID of the DevicePluggablesData.
         * @return the name of the Device
         */
        public DeviceId getDeviceId() { return deviceId; }

         /**
          * Get DevicePluggablesData.
          * @return the name of the Device
          */
        public ArrayList<PluggableData> getPluggables() {
            ArrayList<PluggableData> plugs = new ArrayList<>();
            plugs.addAll(pluggables);
            return plugs;
        }

        public void addPluggables ( List<PluggableData> pluggablesData) { this.pluggables.addAll(pluggablesData);}

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("Device id", deviceId)
                    .add("Pluggables ", pluggables.toString())
                    .toString();
        }
}

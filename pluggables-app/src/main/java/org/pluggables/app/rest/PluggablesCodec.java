package org.pluggables.app.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.DeviceId;
import org.pluggables.app.api.DevicePluggablesData;
import org.pluggables.app.api.PluggableData;
import org.slf4j.Logger;

import java.util.ArrayList;


import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * DevicePluggablesData JSON codec.
 */
//TODO realize a better codec

public final class PluggablesCodec extends JsonCodec<DevicePluggablesData> {
    // JSON field names
    private static final String DEVICE_ID = "device-id";
    private static final String PLUGGABLES = "pluggables";


    private final Logger log = getLogger(getClass());

    @Override
    public ObjectNode encode(DevicePluggablesData devicePluggablesData, CodecContext context){
        checkNotNull(devicePluggablesData, "Device Pluggables Data cannot be null");
        ObjectNode result = context.mapper().createObjectNode();
                //.put(DEVICE_ID,devicePluggablesData.getDeviceId().toString());
        ArrayNode pluggables = context.mapper().createArrayNode();
            devicePluggablesData.getPluggables().forEach(pluggableData -> {
                ObjectNode plugJson = context.codec(PluggableData.class).encode(pluggableData, context);
                plugJson.put(DEVICE_ID,devicePluggablesData.getDeviceId().toString());
                pluggables.add(plugJson);
        });
            result.set(PLUGGABLES, pluggables);
        return result;
    }

    @Override
    public DevicePluggablesData decode(ObjectNode json, CodecContext context){
        if (json == null || !json.isObject()) {
            return null;
        }
        String deviceId = json.findValue(DEVICE_ID).asText();
        ArrayList<PluggableData> pluggableList = new ArrayList<>();
        JsonCodec<PluggableData> pluggableDataCodec = context.codec(PluggableData.class);
        if (!json.isNull()) {
            pluggableList.add(pluggableDataCodec.decode(json,context));
        }

        DevicePluggablesData devicePluggablesData = DevicePluggablesData.of(DeviceId.deviceId(deviceId)
                ,pluggableList);

        return devicePluggablesData;
    }

}

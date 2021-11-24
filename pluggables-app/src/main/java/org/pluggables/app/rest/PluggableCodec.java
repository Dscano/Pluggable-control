package org.pluggables.app.rest;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.util.Frequency;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.pluggables.app.api.PluggableData;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * PluggableData JSON codec.
 */
//TODO realize a better codec

public final class PluggableCodec extends JsonCodec<PluggableData> {
    // JSON field names
    private static final String DEVICE_ID = "device-id";
    private static final String PLUGGABLE_ID = "pluggable-id";
    private static final String PORT = "port";
    private static final String IS_ENABLE = "isEnable";
    private static final String PLUGGABLE_TYPE = "pluggableType";
    private static final String PLUGGABLE_SPEED = "pluggableSpeed";
    private static final String PORT_TYPE = "portType";
    private static final String PORT_SPEED = "portSpeed";
    private static final String OPMODE = "opmode";
    private static final String FREQUENCY = "frequency";
    private static final String POWER = "power";

    private final Logger log = getLogger(getClass());

    @Override
    public ObjectNode encode(PluggableData pluggableData, CodecContext context){
        checkNotNull(pluggableData, "Pluggable Data cannot be null");
        //log.info("pluggabledata" + pluggableData.toString());
        ObjectNode result = context.mapper().createObjectNode()
                .put(PLUGGABLE_ID,pluggableData.getPluggableID())
                .put(PORT, pluggableData.getPort().toString())
                .put(PORT_TYPE, pluggableData.getPortType().toString())
                .put(PORT_SPEED, pluggableData.getPortSpeed().toString())
                .put(IS_ENABLE, pluggableData.getIsEnable().toString())
                .put(PLUGGABLE_TYPE, pluggableData.getPluggableType())
                .put(PLUGGABLE_SPEED, pluggableData.getPluggableSpeed().toString())
                .put(OPMODE, pluggableData.getOpMode())
                .put(FREQUENCY, pluggableData.getFrequency().toString())
                .put(POWER, pluggableData.getPower().toString());


        return result;
    }

    @Override
    public PluggableData decode(ObjectNode json, CodecContext context){
        if (json == null || !json.isObject()) {
            return null;
        }

        String pluggableName= json.findValue(PLUGGABLE_ID).asText();
        //PortNumber port = PortNumber.fromString(json.findValue(PORT).asText());
        //Boolean isEnable = json.findValue(IS_ENABLE).asBoolean();
        //Port.Type portType = Port.Type.valueOf(json.findValue(PORT_TYPE).asText());
        //Long portSpeed = Long.decode(json.findValue(PORT_SPEED).asText());
        String opMode =  json.findValue(OPMODE).asText();
        Frequency frequency = Frequency.ofGHz(Double.valueOf(json.findValue(FREQUENCY).asText()));
        Float power = Float.valueOf(json.findValue(POWER).asText());

        PluggableData pluggableData = PluggableData.of(pluggableName);
        //pluggableData.setPort(port);
        //pluggableData.setIsEnable(isEnable);
        //pluggableData.setPortType(portType);
        //pluggableData.setPortSpeed(portSpeed);
        pluggableData.setOpMode(opMode);
        pluggableData.setFrequency(frequency);
        pluggableData.setPower(power);

        return pluggableData;
    }
}

package org.pluggables.app.api;


import com.google.common.base.MoreObjects;
import org.onlab.util.Frequency;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;

import static java.util.Objects.requireNonNull;



public class PluggableData {

    protected String pluggableID;
    protected PortNumber port;
    protected Port.Type portType;
    protected Long portSpeed;
    protected Boolean isEnabled;
    protected String pluggableType;
    protected Long pluggableSpeed;
    protected String opMode;
    protected Frequency frequency;
    protected Float power;

    /**
     * Constructs a PluggableData given a pluggableId.
     * @param pluggableID the given pluggableId
     */
    private PluggableData (String pluggableID) {

        this.pluggableID = pluggableID;
        this.port = null;
        this.isEnabled = false;
        this.pluggableType = null;
        this.pluggableSpeed = null;
        this.opMode =  "0";
        this.frequency = Frequency.ofGHz(0);
        this.power = (float) 0;
    }
    /**
     * Creates a Pluggable Data data by given name.
     * @param pluggableID the given pluggable name
     * @return the device pluggables data
     */
    public static PluggableData of(String pluggableID) {
        requireNonNull(pluggableID);
        return new PluggableData(pluggableID);
    }

    public void setPort(PortNumber port) { this.port = port; }
    public void setPortType(Port.Type portType) { this.portType= portType; }
    public void setPortSpeed(Long portSpeed) { this.portSpeed= portSpeed; }
    public void setIsEnable(Boolean isEnabled) { this.isEnabled = isEnabled; }
    public void setPluggableType(String pluggableType) { this.pluggableType = pluggableType; }
    public void setPluggableSpeed(Long pluggableSpeed) { this.pluggableSpeed = pluggableSpeed; }
    public void setOpMode(String opModess) { this.opMode = opModess;}
    public void setFrequency(Frequency freq) { this.frequency = freq; }
    public void setPower(Float pow) { this.power = pow; }

    public String getPluggableID() { return this.pluggableID; }
    public Port.Type getPortType() { return this.portType; }
    public Long getPortSpeed() { return this.portSpeed; }
    public PortNumber getPort() { return this.port; }
    public Boolean getIsEnable() { return this.isEnabled; }
    public String getPluggableType() { return this.pluggableType; }
    public Long getPluggableSpeed() { return this.pluggableSpeed; }
    public String getOpMode(){ return this.opMode; }
    public Frequency getFrequency(){ return this.frequency; }
    public Float getPower() { return this.power;}


    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("Pluggable ID", pluggableID)
                .add("Port", port)
                .add("isEnable", isEnabled.toString())
                .add("pluggableType", pluggableType)
                .add("pluggableSpeed", pluggableSpeed)
                .add("opMode", opMode)
                .add("frequency", frequency)
                .add("power", power)
                .toString();
    }




}

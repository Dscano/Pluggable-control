/*
 * Copyright 2021-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pluggables.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.onlab.packet.IpAddress;
import org.onosproject.app.ApplicationAdminService;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.codec.CodecService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.*;

import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.device.*;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.link.*;
import org.onosproject.netconf.*;
import org.onosproject.drivers.utilities.XmlConfigParser;


import org.onosproject.netconf.config.NetconfDeviceConfig;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.pluggables.app.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.pluggables.app.rest.PluggableCodec;
import org.pluggables.app.rest.PluggablesCodec;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Application that manage the pluggables attached to a certain device .
 */
@Component(immediate = true,
           service = {DevicePluggables.class}
           )

public class PluggableManager implements DevicePluggables {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetconfController netconfController;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry netCfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ApplicationAdminService applicationAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CodecService codecService;

    protected DeviceProviderService deviceProviderService;
    protected LinkProviderService linkProviderService;
    public static final String PLUGGABLE_APP = "org.pluggables.app";

    private static final String OC_PLATFORM_TYPES_TRANSCEIVER =
            "oc-platform-types:TRANSCEIVER";

    private static final Integer PORT_NETCONF = 2022;
    private static final String DEVICE_MLNX_1 = "device:10.30.2.44:50001";
    private static final String DEVICE_MLNX_2 = "device:10.30.2.102:50001";
    private static final String CONTROLLER_IP= "10.30.2.73";

    private ArrayList<DevicePluggablesData> storeDevicePluggables = new ArrayList<>();

    private DeviceListener deviceListenert;
    private ApplicationId onosnetconf;
    private ApplicationId onosfaultmanagement;
    private ApplicationId appId;
    private final boolean deactivate_onos_app = true;
    protected Map< String, Map<List<String>,List<DeviceId>>> connectionDB = Maps.newHashMap();

    @Activate
    protected void activate() {
        appId = coreService.registerApplication("org.pluggables.app");
        onosnetconf = coreService.getAppId("org.onosproject.netconf");
        onosfaultmanagement = coreService.getAppId("org.onosproject.faultmanagement");
        deviceListenert = new DevListener();
        deviceService.addListener(deviceListenert);
        codecService.registerCodec(DevicePluggablesData.class, new PluggablesCodec());
        codecService.registerCodec(PluggableData.class, new PluggableCodec());

        if (deactivate_onos_app) {
            try {
                applicationAdminService.activate(onosfaultmanagement);
                applicationAdminService.activate(onosnetconf);
                log.info("### Activating Onos Netconf App ###");
            } catch (NullPointerException ne) {
                log.info(ne.getMessage());
            }
        }
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        deviceService.removeListener(deviceListenert);
        deviceProviderService = null;
        linkProviderService = null;
        log.info("Stopped");
    }

    @Override
    public DevicePluggablesData createDevicePluggables(DeviceId deviceId) {
        requireNonNull(deviceId);
        DevicePluggablesData devPluggablesData = DevicePluggablesData.of(deviceId,retreivePluggablesData(deviceId));
        storeDevicePluggables.add(devPluggablesData);
        log.info(storeDevicePluggables.toString());
        return devPluggablesData;
    }

    @Override
    public Collection<DevicePluggablesData> allDevicePluggables(){
        if(storeDevicePluggables.isEmpty()){ return null; }
        return ImmutableSet.copyOf(storeDevicePluggables);
    }


    /**
     * Configure a couple of pluggable and create a a connection.
     * @param deviceConfPluggables contains the pluggables configuration that has to be set to the pluggable
     * @param connetPoints identify the connect point for enstablishing the connection
     * @param connectioname identify the connection
     */
    @Override
    public void configureDevicePluggable(ArrayList<DevicePluggablesData> deviceConfPluggables,
                                         ArrayList<ConnectPoint> connetPoints, String connectioname)
            throws InterruptedException {
        List<DeviceId> Deviceplugabble = new ArrayList<>();
        List<ConnectPoint> connect = new ArrayList<>();
        deviceConfPluggables.forEach( devConfPlug -> {
            getStoredPluggables(devConfPlug.getDeviceId()).forEach(pluggableData -> {
                devConfPlug.getPluggables().forEach(pluggableDataConf -> {
                    log.info(pluggableDataConf.toString());
                   if(pluggableData.getPluggableID().equals(pluggableDataConf.getPluggableID())){
                       pluggableData.setFrequency(pluggableDataConf.getFrequency());
                       pluggableData.setPower(pluggableDataConf.getPower());
                       pluggableData.setOpMode(pluggableDataConf.getOpMode());
                       Deviceplugabble.add(devConfPlug.getDeviceId());
                       connect.add(ConnectPoint.fromString(devConfPlug.getDeviceId().toString()+"/4"));
                       configurePluggableData(devConfPlug.getDeviceId(),pluggableData);
                   }
                });
            });
        });

        while(linkService.getLink(ConnectPoint.fromString(deviceConfPluggables.get(0).getDeviceId().toString() +"/4"),
                ConnectPoint.fromString(deviceConfPluggables.get(1).getDeviceId().toString()+"/4")) == null
                && linkService.getLink(ConnectPoint.fromString(deviceConfPluggables.get(1).getDeviceId().toString()+"/4"),
                ConnectPoint.fromString(deviceConfPluggables.get(0).getDeviceId().toString()+"/4")) == null)
        {
            log.info("DENTRO WHILE");
            Thread.sleep(10);
        }
        PointToPointIntent.Builder builder = PointToPointIntent.builder();
        builder.appId(appId);
        builder.priority(55);
        builder.filteredIngressPoint(new FilteredConnectPoint(connetPoints.get(0)));
        builder.filteredEgressPoint(new FilteredConnectPoint(connetPoints.get(1)));
        Map<List<String>, List<DeviceId>> connection = Maps.newHashMap();
        List<String> key = new ArrayList<>();

        intentService.submit(builder.build());

        PointToPointIntent.Builder builder2 = PointToPointIntent.builder();
        builder2.appId(appId);
        builder2.priority(55);
        builder2.filteredIngressPoint(new FilteredConnectPoint(connetPoints.get(1)));
        builder2.filteredEgressPoint(new FilteredConnectPoint(connetPoints.get(0)));

        intentService.submit(builder2.build());

        Thread.sleep(100);
        intentService.getIntents().forEach(intent -> {
            key.add(intent.key().toString());
        });
        connection.put(key, Deviceplugabble);
        connectionDB.put(connectioname,connection);
        postLinkREST(CONTROLLER_IP,connectioname,connect.get(0),connect.get(1),
                        Link.Type.DIRECT.toString());
    }

    /**
     * Disable couple of pluggable and remove a connection.
     * @param connectioname identify the connection
     */
    @Override
    public void deleteConfigureDevicePluggable(String connectioname) throws InterruptedException {
        log.info("deleteConfigureDevicePluggable");
        log.info(connectionDB.toString());
        connectionDB.get(connectioname).forEach((key, deviceIds) -> {
            key.forEach(key1-> {
                Intent intent = intentService.getIntent(Key.of(Long.decode(key1), appId));
                intentService.withdraw(intent);
                deviceIds.forEach( deviceId -> {
                        deleteConfigurePluggableData(deviceId);
                        }
                );
            });

        });
        Map<List<String>,List<DeviceId>>  mp = connectionDB.get(connectioname);
        while(linkService.getLink( ConnectPoint.fromString(mp.entrySet().iterator().next().getValue().get(0).toString() +"/4"),
                ConnectPoint.fromString(mp.entrySet().iterator().next().getValue().get(1).toString()+"/4")) != null){
                log.info("while deleteConfigureDevicePluggable");
                Thread.sleep(100);
        }
        deleteLinkREST(CONTROLLER_IP,connectioname,
                    ConnectPoint.fromString(mp.entrySet().iterator().next().getValue().get(0).toString()+"/4"),
                    ConnectPoint.fromString(mp.entrySet().iterator().next().getValue().get(1).toString()+"/4"));

        connectionDB.remove(connectioname);
    }

    /**
     * A listener for Device events. Once are detected the devices with the pluggables it triggers the
     * creation of the related data structure.
     */
    class DevListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            if (event.type().equals(DeviceEvent.Type.DEVICE_ADDED)) {

                if(event.subject().id().toString().equals(DEVICE_MLNX_1) |
                        event.subject().id().toString().equals(DEVICE_MLNX_2)){
                    createDevicePluggables(DeviceId.deviceId(event.subject().id().toString()));
                }
            }

        }
    }

    /**
     * Get the configuration from the xml config.
     * @return The xml string
     */
    private String getConfiguration() {
        StringBuilder filter = new StringBuilder();
        filter.append("<components xmlns='http://openconfig.net/yang/platform'>");
        filter.append("</components>");
        return filter.toString();
    }

    /**
     * Retrieve the pluggables configuration attached to a certain device
     * @param deviceId identify the device where the pluggable is attached
     *
     * @return List of PluggableData
     */
    private ArrayList<PluggableData> retreivePluggablesData(DeviceId deviceId){

        log.info("**********************************************");
        log.info("DENTRO retreivePluggablesData");
        log.info("**********************************************");

        ArrayList<PluggableData> pluggables = new ArrayList<PluggableData>();
        DeviceId deviceId1 = DeviceId.deviceId("netconf:"+
                deviceId.toString().split("device:")[1].split(":")[0]
                +":2022");

        NetconfDeviceConfig netCfg  = netCfgService.addConfig( deviceId1, NetconfDeviceConfig.class);

        netCfg.setIp(deviceId.toString().split("device:")[1].split(":")[0]);
        netCfg.setPort(PORT_NETCONF);
        netCfg.setUsername("admin");
        netCfg.setPassword("admin");

        NetconfDeviceInfo deviceInfo = new NetconfDeviceInfo(netCfg);
        netconfController.setNetconfDevice(deviceId1,deviceInfo);

        try {

            netconfController.connectDevice(deviceId1);
        } catch (NetconfException e) {
            e.printStackTrace();
        }

        NetconfDevice conn = netconfController.getNetconfDevice( IpAddress.valueOf(
                deviceId.toString().split("device:")[1].split(":")[0]),
                PORT_NETCONF);
        try {
            String reply = conn.getSession().getConfig(DatastoreId.RUNNING, getConfiguration());
            XMLConfiguration xconf = (XMLConfiguration) XmlConfigParser.loadXmlString(reply);
            XPathExpressionEngine xpe = new XPathExpressionEngine();
            xconf.setExpressionEngine(xpe);
            HierarchicalConfiguration components = xconf.configurationAt("data/components");
            pluggables.addAll(parseDevicePluggable(deviceId, components));

        } catch (NetconfException e) {
            e.printStackTrace();
        }
        return pluggables;
    }

    /**
     * Configure a pluggable attached to a certain device
     * @param deviceId identify the device where the pluggable is attached
     * @param confPluggableData contains the pluggable configuration that has to be set.
     */
    private void configurePluggableData(DeviceId deviceId, PluggableData confPluggableData){

        log.info("**********************************************");
        log.info("DENTRO configurePluggableData");
        log.info(" conf data" + confPluggableData.toString());
        log.info("**********************************************");

        //DeviceId deviceId1 = DeviceId.deviceId("netconf:127.0.0.1:2022");
        DeviceId deviceId1 = DeviceId.deviceId("netconf:"+
                deviceId.toString().split("device:")[1].split(":")[0]+
                ":2022");
        NetconfDeviceConfig netCfg  = netCfgService.addConfig( deviceId1, NetconfDeviceConfig.class);

        //netCfg.setIp("127.0.0.1");
        netCfg.setIp(deviceId.toString().split("device:")[1].split(":")[0]);
        netCfg.setPort(PORT_NETCONF);
        netCfg.setUsername("admin");
        netCfg.setPassword("admin");

        NetconfDeviceInfo deviceInfo = new NetconfDeviceInfo(netCfg);
        netconfController.setNetconfDevice(deviceId1,deviceInfo);

        try {
            netconfController.connectDevice(deviceId1);
        } catch (NetconfException e) {
            e.printStackTrace();
        }

        NetconfDevice conn = netconfController.getNetconfDevice( IpAddress.valueOf(
                deviceId.toString().split("device:")[1].split(":")[0]),
                PORT_NETCONF);

        try {
                conn.getSession().editConfig(DatastoreId.RUNNING, null, setConfPlug(confPluggableData));
                conn.disconnect();

        } catch (NetconfException e) {
            e.printStackTrace();
        }

    }

    /**
     * Disable the pluggable attached to a certain device
     * @param deviceId identify the device where the pluggable is attached
     */
    private void deleteConfigurePluggableData(DeviceId deviceId){

        log.info("**********************************************");
        log.info("DENTRO deleteconfigurePluggableData");
        log.info("**********************************************");

        DeviceId deviceId1 = DeviceId.deviceId("netconf:"+
                deviceId.toString().split("device:")[1].split(":")[0]+
                ":2022");
        NetconfDeviceConfig netCfg  = netCfgService.addConfig( deviceId1, NetconfDeviceConfig.class);

        netCfg.setIp(deviceId.toString().split("device:")[1].split(":")[0]);
        netCfg.setPort(PORT_NETCONF);
        netCfg.setUsername("admin");
        netCfg.setPassword("admin");

        NetconfDeviceInfo deviceInfo = new NetconfDeviceInfo(netCfg);
        netconfController.setNetconfDevice(deviceId1,deviceInfo);

        try {
            netconfController.connectDevice(deviceId1);
        } catch (NetconfException e) {
            e.printStackTrace();
        }

        NetconfDevice conn = netconfController.getNetconfDevice( IpAddress.valueOf(
                deviceId.toString().split("device:")[1].split(":")[0]),
                PORT_NETCONF);

        try {   //TODO make it better
            conn.getSession().editConfig(DatastoreId.RUNNING, null, deleteConfPlug());
            //conn.getSession().editConfig(DatastoreId.RUNNING, null, setConfPower(confPluggableData));
            //conn.getSession().editConfig(DatastoreId.RUNNING, null, setConfOPmode(confPluggableData));
            conn.disconnect();

        } catch (NetconfException e) {
            log.info("MANNAGGIAS");
            e.printStackTrace();
        }

    }

    /**
     * Retrieve all the pluggables information attached to a certain device
     * @param deviceId identify the device where the pluggables are attached
     *
     * @return List of PluggableData
     */
    private ArrayList<PluggableData> getStoredPluggables(DeviceId deviceId){

        ArrayList<PluggableData> pluggableData = new ArrayList<>();
        storeDevicePluggables.forEach( devplug -> {
            if(devplug.getDeviceId().equals(deviceId)){
                pluggableData.addAll(devplug.getPluggables());
            }
        });

        return pluggableData;
    }

    /**
     * Parses port information from OpenConfig XML configuration.
     *
     * @param components the XML document with components root.
     * @return List of PluggableData
     *
     * //CHECKSTYLE:OFF
     * <pre>{@code
     *   <components xmlns="http://openconfig.net/yang/platform">
     *     <component>....
     *     </component>
     *     <component>....
     *     </component>
     *   </components>
     * }</pre>
     * //CHECKSTYLE:ON
     */
    protected List<PluggableData> parseDevicePluggable(DeviceId deviceId, HierarchicalConfiguration components) {

        return components.configurationsAt("component").stream()
                .filter(component -> {
                    return !component.getString("name", "unknown").equals("unknown")&&
                            component.getString("state/type", "unknown")
                                    .equals(OC_PLATFORM_TYPES_TRANSCEIVER);
                })
                .map(component -> {
                            try {;
                                // Pass the root document for cross-reference
                                return parsePluggabeComponent(deviceId, component);
                            } catch (Exception e) {
                                return null;
                            }
                        }
                ).filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Parses a component XML doc into PluggableData.
     *
     * @param component subtree to parse. It must be a component ot type PluggableData.
     * @param deviceId identify the device where the pluggables are attached
     *
     * @return PluggableData
     */
    private PluggableData parsePluggabeComponent(DeviceId deviceId, HierarchicalConfiguration component) {
        PluggableData pluggableData = PluggableData.of(
                component.getString("transceiver/state/vendor").concat("-")
                .concat(component.getString("transceiver/state/vendor-part")).concat("-")
                .concat(component.getString("transceiver/state/vendor-rev")));
        pluggableData.setPort(PortNumber.fromString(
                component.getString("name").split("-")[1]));
        pluggableData.setPortType(deviceService.getPort(deviceId,PortNumber.fromString(
                component.getString("name").split("-")[1])).type());
        pluggableData.setPortSpeed(deviceService.getPort(deviceId,PortNumber.fromString(
                component.getString("name").split("-")[1])).portSpeed());
        pluggableData.setPluggableSpeed(Long.decode(component.getString("transceiver/state/ethernet-pmd-preconf")
                .split(":ETH_")[1].split("GBASE_ZR")[0]));
        pluggableData.setPluggableType(component.getString("transceiver/state/module-functional-type")
                .split(":TYPE_")[1]);
        pluggableData.setIsEnable(Boolean.valueOf(component.getString("transceiver/state/enabled")));
        return pluggableData;
    }

    /**
     * Create a xml string for configuring the pluggable
     * @param confPluggableData contains the parameter that must be configured on the pluggable
     *
     * @return The xml string
     */
    private String setConfPlug(PluggableData confPluggableData) {
            StringBuilder sb = new StringBuilder();
            sb.append("<components xmlns='http://openconfig.net/yang/platform'>");
            sb.append("<component>");
            sb.append("<name>");
            sb.append("channel-".concat(confPluggableData.getPort().toString()));
            sb.append("</name>");
            sb.append("<oc-opt-term:optical-channel xmlns:oc-opt-term='http://openconfig.net/yang/terminal-device'>");
            sb.append("<oc-opt-term:config>");
            sb.append("<oc-opt-term:frequency>".concat(String.valueOf(
                    (int) confPluggableData.getFrequency().asGHz())));
            sb.append("</oc-opt-term:frequency>");
            sb.append("<oc-opt-term:target-output-power>".concat(confPluggableData.getPower().toString()));
            sb.append("</oc-opt-term:target-output-power>");
            sb.append("<oc-opt-term:operational-mode>".concat(confPluggableData.getOpMode()));
            sb.append("</oc-opt-term:operational-mode>");
            sb.append("</oc-opt-term:config>");
            sb.append("</oc-opt-term:optical-channel>");
            sb.append("</component>");
            sb.append("</components>");
            sb.append("<terminal-device xmlns='http://openconfig.net/yang/terminal-device'>");
            sb.append("<logical-channels>");
            sb.append("<channel>");
            sb.append("<index>".concat(confPluggableData.getPort().toString()));
            sb.append("</index>");
            sb.append("<config>");
            sb.append("<admin-state>"+"ENABLED");
            sb.append("</admin-state>");
            sb.append("</config>");
            sb.append("</channel>");
            sb.append("</logical-channels>");
            sb.append("</terminal-device>");

        return sb.toString();
    }

    /**
     * Create a xml string for disabling the pluggable
     * @return The xml string
     */
    private String deleteConfPlug() {
        StringBuilder sb = new StringBuilder();
        sb.append("<terminal-device xmlns='http://openconfig.net/yang/terminal-device'>");
        sb.append("<logical-channels>");
        sb.append("<channel>");
        sb.append("<index>".concat("4")); //TODO should do a better implementation
        sb.append("</index>");
        sb.append("<config>");
        sb.append("<admin-state>"+"DISABLED");
        sb.append("</admin-state>");
        sb.append("</config>");
        sb.append("</channel>");
        sb.append("</logical-channels>");
        sb.append("</terminal-device>");

        return sb.toString();
    }


    protected void postLinkREST(String controllerIP,
                                String connectioname,
                                ConnectPoint src, ConnectPoint dst,
                                String type) {
        //Build the POST json
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode srcData = mapper.createObjectNode();
        srcData.put("port", src.port().toString());
        srcData.put("device", src.deviceId().toString());

        ObjectNode dstData = mapper.createObjectNode();
        dstData.put("port", dst.port().toString());
        dstData.put("device", dst.deviceId().toString());

        ObjectNode objectNode = mapper.createObjectNode();
        objectNode.put("src", srcData);
        objectNode.put("dst", dstData);
        objectNode.put("type", type);
        objectNode.put("state", "ACTIVE");
        objectNode.put("connection-name", connectioname);

        log.info("Pluggable configuration sent to controller {} json {}",
                controllerIP,
                objectNode.toString());

        try {
            URL url = new URL("http://" + controllerIP + ":8181/onos/ecoc21opt-app/listener/links");
            try {

                String loginPassword = "karaf:karaf";
                String encoded = Base64.getEncoder().encodeToString(loginPassword.getBytes());

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty ("Authorization", "Basic " + encoded);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestMethod("POST");


                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(objectNode.toString().getBytes());
                os.flush();
                os.close();

                log.info(String.valueOf(conn.getResponseCode()));
                log.info(conn.getResponseMessage());
                log.info(conn.getRequestMethod());

                //ObjectNode root = readTreeFromStream(mapper, conn.getInputStream());
                //log.info(root.toString());

            } catch (IOException exe) {
                exe.printStackTrace();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    protected void deleteLinkREST(String controllerIP,String connectioname,
                                ConnectPoint src, ConnectPoint dst) {
        //Build the POST json
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode srcData = mapper.createObjectNode();
        srcData.put("port", src.port().toString());
        srcData.put("device", src.deviceId().toString());

        ObjectNode dstData = mapper.createObjectNode();
        dstData.put("port", dst.port().toString());
        dstData.put("device", dst.deviceId().toString());

        ObjectNode objectNode = mapper.createObjectNode();
        objectNode.put("src", srcData);
        objectNode.put("dst", dstData);
        objectNode.put("type", Link.Type.DIRECT.toString());
        objectNode.put("state", Link.State.INACTIVE.toString());
        objectNode.put("connection-name", connectioname);

        log.info("Pluggable configuration sent to controller {} json {}",
                controllerIP,
                objectNode.toString());

        try {
            URL url = new URL("http://" + controllerIP + ":8181/onos/ecoc21opt-app/listener/links");
            try {

                String loginPassword = "karaf:karaf";
                String encoded = Base64.getEncoder().encodeToString(loginPassword.getBytes());

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty ("Authorization", "Basic " + encoded);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestMethod("DELETE");

                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(objectNode.toString().getBytes());
                os.flush();
                os.close();

                log.info(String.valueOf(conn.getResponseCode()));
                log.info(conn.getResponseMessage());
                log.info(conn.getRequestMethod());

            } catch (IOException exe) {
                exe.printStackTrace();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }


}

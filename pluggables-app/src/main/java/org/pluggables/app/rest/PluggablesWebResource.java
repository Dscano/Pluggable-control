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
package org.pluggables.app.rest;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.device.DeviceService;
import org.onosproject.rest.AbstractWebResource;
import org.pluggables.app.api.DevicePluggables;
import org.pluggables.app.api.DevicePluggablesData;
import org.slf4j.Logger;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;

import java.util.*;

import static org.onlab.util.Tools.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Query Pluggables app.
 */
@Path("pluggables")
public class PluggablesWebResource extends AbstractWebResource {
    @Context
    private UriInfo uriInfo;

    private static final String PLUGGABLES = "pluggables";
    private static final String DEVICE_ID = "device-id";
    private static final String PLUGGABLE_ONE = "pluggable-one";
    private static final String PLUGGABLE_TWO = "pluggable-two";
    private static final String CONF_PLUGGABLE = "configure";
    private static final String SRC = "srcConnectPoint";
    private static final String DST = "dstConnectPoint";
    private static final String PLUGGABLE_ID = "pluggable-id";
    private static final String PORT = "port";
    private static final String PORT_TYPE = "portType";
    private static final String PORT_SPEED = "portSpeed";
    private static final String IS_ENABLE = "isEnable";
    private static final String PLUGGABLE_TYPE = "pluggableType";
    private static final String PLUGGABLE_SPEED = "pluggableSpeed";
    private static final String OPMODE = "opmode";
    private static final String FREQUENCY = "frequency";
    private static final String POWER = "power";
    private static final String CONNECTIONAME = "connection-name";

    private final ObjectNode root = mapper().createObjectNode();
    private final Logger log = getLogger(getClass());

    /**
     * Gets all Pluggables. Returns an array of all Pluggables in the network.
     * @return 200 OK
     *
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPluggables() {
        ArrayNode pluggablesNode = mapper().createArrayNode();
        DevicePluggables service = get(DevicePluggables.class);
        Collection<DevicePluggablesData> devicePluggablesDatas = service.allDevicePluggables();
        if (!devicePluggablesDatas.isEmpty()) {
            for (DevicePluggablesData entry : devicePluggablesDatas) {
                //TODO fix the codecDevicePluggables and PlauggableData for obtaining a pretty JSON
                //pluggablesNode.add(codec(DevicePluggablesData.class).encode(entry, this));
                entry.getPluggables().forEach(pluggableData -> {
                    ObjectNode objectNode = mapper().createObjectNode();
                    objectNode.put(DEVICE_ID,entry.getDeviceId().toString())
                            .put(PLUGGABLE_ID,pluggableData.getPluggableID())
                            .put(PORT, pluggableData.getPort().toString())
                            .put(PORT_TYPE, pluggableData.getPortType().toString())
                            .put(PORT_SPEED, pluggableData.getPortSpeed().toString())
                            .put(IS_ENABLE, pluggableData.getIsEnable().toString())
                            .put(PLUGGABLE_TYPE, pluggableData.getPluggableType())
                            .put(PLUGGABLE_SPEED, pluggableData.getPluggableSpeed().toString())
                            .put(OPMODE, pluggableData.getOpMode())
                            .put(FREQUENCY, pluggableData.getFrequency().asGHz())
                            .put(POWER, pluggableData.getPower().toString());
                    pluggablesNode.add(objectNode);
                    }
                );
            }
        }
        root.putPOJO(PLUGGABLES, pluggablesNode);
        return ok(root).build();
    }

    /**
     * Set a Pluggables configuration and create a connection.
     * @param stream input JSON
     * @return 200 OK
     * @onos.rsModel ConfpluggablesPost
     *
     */
    @POST
    @Path("configure")
    public Response setPluggableConnection(InputStream stream) {
        log.info("Dentro setPluggableConnection");
        DevicePluggables service = get(DevicePluggables.class);
        DeviceService deviceService = get(DeviceService.class);
        ArrayList<DevicePluggablesData> devicePluggablesDataConf = new ArrayList<>();
        ArrayList<ConnectPoint> connectPoints = new ArrayList<>();
        String connectioname;

       try{
           //TODO better Codec
           ObjectNode jsonTree = readTreeFromStream(mapper(), stream);
           devicePluggablesDataConf.add(decode(jsonTree.get(PLUGGABLE_ONE),DevicePluggablesData.class));
           devicePluggablesDataConf.add(decode(jsonTree.get(PLUGGABLE_TWO),DevicePluggablesData.class));
           connectPoints.add(ConnectPoint.fromString(jsonTree.get(SRC).asText()));
           connectPoints.add(ConnectPoint.fromString(jsonTree.get(DST).asText()));
           connectioname = jsonTree.get(CONNECTIONAME).asText();
           nullIsNotFound(deviceService.getDevice(devicePluggablesDataConf.get(0).getDeviceId()),
                   "Device Id is not found");
           nullIsNotFound(deviceService.getDevice(devicePluggablesDataConf.get(1).getDeviceId()),
                   "Device Id is not found");
           service.configureDevicePluggable(devicePluggablesDataConf,connectPoints,connectioname);

           UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                   .path(CONF_PLUGGABLE);

           return Response
                   .created(locationBuilder.build())
                   .build();

       } catch (IOException | InterruptedException e) {
           throw new IllegalArgumentException(e.getMessage());
       }

    }

    /**
     * Disable the pluggables tha belonging to a certain connection.
     * @param connectioname connection identifier
     * @return 200 OK
     *
     */
    @DELETE
    @Path("delete/{connection-name}")
    public Response removePluggableConnection(@PathParam("connection-name") String connectioname) {

        DevicePluggables servicePlug = get(DevicePluggables.class);
        try {
            servicePlug.deleteConfigureDevicePluggable(connectioname);
        } catch (InterruptedException e) {
        throw new IllegalArgumentException(e.getMessage());
        }
        return Response.noContent().build();
    }

    }

/*******************************************************************************
 * Copyright (c) 2015 Institute for Pervasive Computing, ETH Zurich and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *    Matthias Kovatsch - creator and main architect
 ******************************************************************************/
package com.iot.device.management.iot.coap.server;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.iot.device.management.iot.Management;
import com.iot.device.management.iot.device.DeviceProperties;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class ServerService extends Service {

    CoapServer server;

    @Override
    public void onCreate() {
        this.server = new CoapServer();
        server.add(new RegisterDevice());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        server.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        server.destroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    class RegisterDevice extends CoapResource {

        private static final String TAG_TITLE = "title";
        private static final String TAG_IP = "ip";
        private static final String TAG_PORT = "port";
        private static final String TAG_MAC = "mac";
        private static final String TAG_VERSION = "version";

        public RegisterDevice() {
            super("register/device");
            getAttributes().setTitle("Register device resource");
        }

        @Override
        public void handlePOST(CoapExchange exchange) {
            try {
                JSONObject register = new JSONObject(exchange.getRequestText());
                String title = register.getString(TAG_TITLE);
                String ipAddress = register.getString(TAG_IP);
                Integer port = register.getInt(TAG_PORT);
                String macAddress = register.getString(TAG_MAC);
                Integer version = register.getInt(TAG_VERSION);
                if(version != 1){
                    exchange.reject();
                    exchange.respond(CoAP.ResponseCode.NOT_IMPLEMENTED);
                    return;
                }
                exchange.accept();
                DeviceProperties deviceProperties = new DeviceProperties(title, ipAddress, port, macAddress);
                String uuid = UUID.randomUUID().toString();
                Management.registerDevice(uuid, deviceProperties);
                exchange.respond(CoAP.ResponseCode.CONTENT, uuid);
            } catch (Exception e) {
                exchange.reject();
                exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
            }
        }
    }
}

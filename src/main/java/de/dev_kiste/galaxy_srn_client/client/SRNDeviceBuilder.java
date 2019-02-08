package de.dev_kiste.galaxy_srn_client.client;

import de.dev_kiste.galaxy.driver.GalaxyDriver;
import de.dev_kiste.galaxy.messaging.MessageHandler;
import de.dev_kiste.galaxy.node.middleware.GalaxyMiddleware;

import java.util.ArrayList;
import java.util.Optional;

/**
 * Builder for
 * @author Benny Lach
 */
public class SRNDeviceBuilder {
    private DeviceType type = DeviceType.FULL_FUNCTION;
    private GalaxyDriver driver = null;
    private MessageHandler messageHandler = null;
    private ArrayList<GalaxyMiddleware> middlewares = new ArrayList();
    private String secret = "";

    private boolean isDebug = false;

    public SRNDeviceBuilder() {}

    public SRNDeviceBuilder setDeviceType(DeviceType type) {
        if(type != null) {
            this.type = type;
        }
        return this;
    }

    public SRNDeviceBuilder setDriver(GalaxyDriver driver) {
        this.driver = driver;

        return this;
    }

    public SRNDeviceBuilder use(GalaxyMiddleware middleware) {
        Optional.ofNullable(middleware).ifPresent(value -> middlewares.add(value));

        return this;
    }

    public SRNDeviceBuilder setMessageHandler(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;

        return this;
    }

    public SRNDeviceBuilder setSecret(String secret) {
        if(secret != null) {
            this.secret = secret;
        }
        return this;
    }

    public SRNDeviceBuilder isDebug() {
        this.isDebug = true;

        return this;
    }

    public SRNDevice build() {
        return new SRNDevice(this);
    }

    DeviceType getDeviceType() {
        return type;
    }

    ArrayList<GalaxyMiddleware> getMiddlewares() {
        return middlewares;
    }

    MessageHandler getMessageHandler() {
        return messageHandler;
    }

    GalaxyDriver getDriver() {
        return driver;
    }

    boolean getIsDebug() {
        return isDebug;
    }

    String getSecret() {
        return secret;
    }


}

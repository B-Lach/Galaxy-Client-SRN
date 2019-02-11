package de.dev_kiste.galaxy_srn_client;

import de.dev_kiste.galaxy.driver.GalaxyDriver;
import de.dev_kiste.galaxy.driver.HTWLoRaDriver;
import de.dev_kiste.galaxy.messaging.GalaxyMessage;
import de.dev_kiste.galaxy.node.middleware.GalaxyMiddleware;
import de.dev_kiste.galaxy.node.middleware.MiddlewareCaller;
import de.dev_kiste.galaxy.node.middleware.MiddlewareStopper;
import de.dev_kiste.galaxy_srn_client.client.DeviceType;
import de.dev_kiste.galaxy_srn_client.client.SRNDevice;
import de.dev_kiste.galaxy_srn_client.client.SRNDeviceBuilder;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Benny Lach
 */
public class App {

    public static void main(String[] args) {
        GalaxyDriver driver = new HTWLoRaDriver("cu.SLAB_USBtoUART");

        GalaxyMiddleware logger = new GalaxyMiddleware() {
            private Logger logger = Logger.getLogger("Middleware");

            @Override
            public void execute(GalaxyMessage message, MiddlewareCaller caller, MiddlewareStopper stopper) {
                logger.log(Level.INFO, message.getPayload() + " - " + message.getSource());

                caller.call(message);
            }
        };

        GalaxyMiddleware cancel = new GalaxyMiddleware() {
            @Override
            public void execute(GalaxyMessage message, MiddlewareCaller caller, MiddlewareStopper stopper) {
                System.out.println("In cancel");
                stopper.stop();
            }
        };

        SRNDevice device = new SRNDeviceBuilder()
                .setDeviceType(DeviceType.FULL_FUNCTION)
                .setDriver(driver)
                .setSecret("FooBar")
                .use(logger)
                .isDebug()
                .build();

        device.connect()
                .thenCompose((isOnline) -> {
                    if(isOnline) return device.setAddress("FFFF");
                    System.out.println("device is not online");
                    return CompletableFuture.completedFuture(false);
                })
                .thenCompose((didSetAddress) -> {
                    if(didSetAddress) return device.getAddress();
                    System.out.println("Did not set new address");

                    return CompletableFuture.completedFuture("");
                })
                .thenCompose((address) -> device.send("Hello World"))
                .thenAccept((didSend) -> System.out.println("Did send: " + didSend));


    }
}

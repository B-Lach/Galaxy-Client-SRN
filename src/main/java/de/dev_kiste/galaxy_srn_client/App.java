package de.dev_kiste.galaxy_srn_client;

import de.dev_kiste.galaxy.driver.GalaxyDriver;
import de.dev_kiste.galaxy.driver.HTWLoRaDriver;
import de.dev_kiste.galaxy.messaging.GalaxyMessage;
import de.dev_kiste.galaxy.node.GalaxyNode;
import de.dev_kiste.galaxy.node.GalaxyNodeBuilder;
import de.dev_kiste.galaxy.node.middleware.GalaxyMiddleware;
import de.dev_kiste.galaxy.node.middleware.MiddlewareCaller;
import de.dev_kiste.galaxy.node.middleware.MiddlewareStopper;

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

        GalaxyNode node = new GalaxyNodeBuilder()
                .setDriver(driver)
                .setMessageHandler(message -> System.out.println("Received payload: " + message.getPayload() + " from: " + message.getSource()))
                .use(logger)
//                .use(cancel)
                .isDebug()
                .build();

        node.bootstrap()
                .thenCompose(bootstrapped -> {
                    System.out.println("Did bootstrapped: " + bootstrapped);

                    return node.getAddress();
                })
                .thenCompose(address -> {
                    System.out.println("Node address: " + address);

                    return node.sendBroadcastMessage("Hello from macOS");
                })
                .thenAccept(didSend ->
                        System.out.println("Did send message: " + didSend)
                );
    }
}

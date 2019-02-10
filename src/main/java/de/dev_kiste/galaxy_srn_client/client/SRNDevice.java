package de.dev_kiste.galaxy_srn_client.client;

import de.dev_kiste.galaxy.messaging.MessageHandler;
import de.dev_kiste.galaxy.node.GalaxyNode;
import de.dev_kiste.galaxy.node.GalaxyNodeBuilder;
import de.dev_kiste.galaxy.node.middleware.GalaxyMiddleware;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SRNDevice class
 *
 * @author Benny Lach
 */
public class SRNDevice {

    private DeviceType deviceType;
    private GalaxyNode node;
    private MessageHelper messageHelper;

    private final Optional<Logger> logger;
    private Optional<MessageHandler> messageHandler;
    
    SRNDevice(SRNDeviceBuilder builder) {
        messageHandler = Optional.ofNullable(builder.getMessageHandler());
        deviceType = builder.getDeviceType();
        logger = Optional.ofNullable(
                builder.getIsDebug() ?
                        Logger.getLogger("Galaxy.GalaxyNode") :
                        null
        );

        GalaxyNodeBuilder nodeBuilder = new GalaxyNodeBuilder()
                .setDriver(builder.getDriver())
                // TODO: Replace with valid logic
                .setMessageHandler((message) -> System.out.println("received: " + message.getPayload() + " : " + message.getSource()));

        if(builder.getIsDebug()) {
            nodeBuilder = nodeBuilder.isDebug();
        }
        for(GalaxyMiddleware m: builder.getMiddlewares()) {
            nodeBuilder.use(m);
        }
        node = nodeBuilder.build();

        messageHelper = new MessageHelper(builder.getSecret(), node.getMaximumMessageSize());

        logIfNeeded(Level.INFO, "SRN Device initialized\n " +
                "Device Type: " + deviceType + "\n" +
                "Node: " + node + "\n"
        );
    }

    /**
     * Try to connect to module and start listening
     *
     * @return Future containing boolean indicating if device is online
     */
    public CompletableFuture<Boolean> connect() {
        return node.bootstrap();
    }

    /**
     * Method to set the used hardware address
     *
     * @param address The address to us
     * @return Future containing boolean indicating if address was set
     */
    public CompletableFuture<Boolean> setAddress(String address) {
        return node.setAddress(address);
    }

    /**
     * Method to get the current used address
     *
     * @return Future containing the currently used address
     */
    public CompletableFuture<String> getAddress() {
        return node.getAddress();
    }

    /**
     * Trying to find coordinator in range. If no coordinator response was received and the current device is
     * <code>DeviceType.FULL_Function</code> the device will automatically become the coordinator
     *
     * @param repeatCount Number of retries the device shoould perform
     * @return Future containing boolean indicating if coordinator was found
     */
    public CompletableFuture<Boolean> findCoordinator(int repeatCount ) {
        CompletableFuture<Boolean> future = new CompletableFuture();
        // TODO: Start Broadcasting Coordinator Search, store future and return if response received or repeatCount exceeded
        return future;
    }

    /**
     * Method to send the given message using FFFF (broadcast) as destination.
     *
     * @param message Message to send
     * @return Futurue containing boolean indicating if sending the message succeeded.
     */
    public CompletableFuture<Boolean> send(String message) {
        CompletableFuture<Boolean> future = new CompletableFuture();
        try {
            byte[][] payloads = messageHelper.buildMessage(Optional.empty(), message);
            sendPayloadArray(payloads, 0, future, true);

        } catch (Exception e) {
            logIfNeeded(Level.WARNING, e.getMessage());
            future.complete(false);
        }
        return future;
    }

    /**
     * Recursively sends an array of payloads and will trigger the future after each message was send
     *
     * @param payloads The payloads to send
     * @param index The current in the payload array. Should be set to 0 on calling this methodö
     * @param future The Future to trigger at the end.
     * @param lastSucceeded Boolean indicating if the last message was send. Should be set to true on calling this method
     */
    private void sendPayloadArray(byte[][] payloads, int index, CompletableFuture<Boolean> future, boolean lastSucceeded) {
        if(!lastSucceeded) {
            future.complete(lastSucceeded);
        } else if(payloads.length >= index) {
            future.complete(lastSucceeded);
        } else {
            node.sendBroadcastPayload(payloads[index])
                    .thenAccept((didSend) -> {
                        sendPayloadArray(payloads, index + 1, future, didSend);
                    });
        }
    }

    /**
     * Method to log for debugging
     *
     * @param level The used log level
     * @param message The message to log
     */
    private void logIfNeeded(Level level, String message) {
        logger.ifPresent(logger -> {
            logger.log(level, message);
        });
    }
}

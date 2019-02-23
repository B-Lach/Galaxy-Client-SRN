package de.dev_kiste.galaxy_srn_client.client;

import de.dev_kiste.galaxy.messaging.GalaxyMessage;
import de.dev_kiste.galaxy.node.GalaxyNode;
import de.dev_kiste.galaxy.node.GalaxyNodeBuilder;
import de.dev_kiste.galaxy.node.middleware.GalaxyMiddleware;
import de.dev_kiste.galaxy.node.middleware.MiddlewareCaller;
import de.dev_kiste.galaxy.node.middleware.MiddlewareStopper;
import de.dev_kiste.galaxy_srn_client.message.SRNMessage;
import de.dev_kiste.galaxy_srn_client.message.SRNMessageHandler;
import de.dev_kiste.galaxy_srn_client.message.SRNMessageHeader;
import de.dev_kiste.galaxy_srn_client.message.SRNMultiMessageHandler;

import java.util.Arrays;
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
    private SRNMultiMessageHandler multiMessageHandler;

    private final Optional<Logger> logger;
    private Optional<SRNMessageHandler> messageHandler;

    /**
     * Default initializer to build an <code>SRNDevice</code> object from the given <code>SRNDeviceBuilder</code> instance
     *
     * @param builder the builder
     */
    SRNDevice(SRNDeviceBuilder builder) {
        messageHandler = Optional.ofNullable(builder.getMessageHandler());
        multiMessageHandler = new SRNMultiMessageHandler((multiMessage -> {
            messageHandler.ifPresent(handler ->  {
                try {
                    SRNMessage message = multiMessage.buildMessage(messageHelper);
                    logIfNeeded(Level.INFO, "Will forward SRMultiNMessage to registered handler");

                    handler.received(message);
                } catch (Exception e) {
                    logIfNeeded(Level.WARNING,  "Failed to build SRNMessage object from SRNMultiMessage");
                }
            });
        }));

        deviceType = builder.getDeviceType();
        logger = Optional.ofNullable(
                builder.getIsDebug() ?
                        Logger.getLogger("Galaxy.GalaxyNode") :
                        null
        );

        GalaxyNodeBuilder nodeBuilder = new GalaxyNodeBuilder()
                .setDriver(builder.getDriver())
                .setMessageHandler(received-> {
                    SRNMessage message = new SRNMessage(received.getPayload());

                    if(message.getHeader().isMultiMessage()) {
                        if(!multiMessageHandler.addNewMessage(message)) {
                            logIfNeeded(Level.WARNING, "Failed to add SRNMultiMessage to message stack");
                        } else {
                            logIfNeeded(Level.INFO, "Added new SRNMultiMessage to message stack");
                        }
                    } else {
                        messageHandler.ifPresent(handler -> {
                            logIfNeeded(Level.INFO, "Will forward SRNMessage to registered handler");

                            handler.received(message);
                        });
                    }

                    try {
                        byte[][] data = new byte[][] {messageHelper.createEncrypedMessageCopy(received.getPayload(), message.getHeader().isMultiMessage())};
                        sendPayloadArray(data, 0, Optional.empty(), true);
                    } catch (Exception e) {
                        logIfNeeded(Level.WARNING, e.getMessage());
                    }
                });

        if(builder.getIsDebug()) {
            nodeBuilder = nodeBuilder.isDebug();
        }

        // Decrypt incoming message first if it isn't part of a multi message
        nodeBuilder.use((GalaxyMessage message, MiddlewareCaller caller, MiddlewareStopper stopper) -> {
            try {
                int offset = messageHelper.getDataOffset();
                byte[] headerBytes = Arrays.copyOfRange(message.getPayload(), 0, offset);

                SRNMessageHeader header = new SRNMessageHeader(headerBytes);

                if(header.isMultiMessage()) {
                    logIfNeeded(Level.INFO, "Incoming message is a multi message. Skipped decrypting");

                    caller.call(message);
                } else {
                    byte[] encrypted = Arrays.copyOfRange(message.getPayload(), offset, message.getPayload().length);
                    byte[] decrypted = messageHelper.decrypt(encrypted);
                    byte[] newBytes = new byte[offset + decrypted.length];

                    System.arraycopy(message.getPayload(), 0, newBytes, 0, offset);
                    System.arraycopy(decrypted, 0, newBytes, offset, decrypted.length);

                    GalaxyMessage newMessage = new GalaxyMessage(newBytes, message.getSource());
                    logIfNeeded(Level.INFO, "Incoming message has been decrypted");
                    caller.call(newMessage);
                }
            } catch (Exception e) {
                logIfNeeded(Level.WARNING, e.getMessage());
                stopper.stop();
            }
        });

        // Execute Loop Detection Middleware after decryption
        nodeBuilder.use((GalaxyMessage message, MiddlewareCaller caller, MiddlewareStopper stopper) -> {
            SRNMessage m = new SRNMessage(message.getPayload());

            if(LoopDetector.getInstance().haveMessageSeenBefore(m)) {
                logIfNeeded(Level.INFO, "Loop detection found duplicate message. Will stop pipeline execution");
                stopper.stop();
            } else {
                caller.call(message);
            }
        });
        // Execute all registered middlewares
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
     * Set the used hardware address
     *
     * @param address The address to us
     * @return Future containing boolean indicating if address was set
     */
    public CompletableFuture<Boolean> setAddress(String address) {
        return node.setAddress(address);
    }

    /**
     * Get the current used address
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
     * Sends the given message using FFFF (broadcast) as destination.
     *
     * @param message Message to send
     * @return Futurue containing boolean indicating if sending the message succeeded.
     */
    public CompletableFuture<Boolean> send(byte topic, String message ) {
        CompletableFuture<Boolean> future = new CompletableFuture();
        try {
            SRNMessage[] messages = messageHelper.buildMessage(Optional.empty(), topic, message);
            byte[][] payloads = new byte[messages.length][];

            for(int i = 0; i < messages.length; i++) {
                LoopDetector.getInstance().willSendMessage(messages[i]);
                payloads[i] = messages[i].toBytes();
            }
            sendPayloadArray(payloads, 0, Optional.of(future), true);

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
     * @param index The current in the payload array. Should be set to 0 on calling this method
     * @param future The Future to trigger at the end.
     * @param lastSucceeded Boolean indicating if the last message was send. Should be set to true on calling this method
     */
    private void sendPayloadArray(byte[][] payloads, int index, Optional<CompletableFuture<Boolean>> future, boolean lastSucceeded) {
        if(!lastSucceeded) {
            future.ifPresent(f -> f.complete(lastSucceeded));
        } else if(index >= payloads.length) {
            future.ifPresent(f -> f.complete(lastSucceeded));
        } else {
            node.sendBroadcastPayload(payloads[index])
                    .thenAccept(didSend -> sendPayloadArray(payloads, index + 1, future, didSend));
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

package de.dev_kiste.galaxy_srn_client.client;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.AeadFactory;
import com.google.crypto.tink.aead.AeadKeyTemplates;
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
    private SequenceNumber seqNumber;

    private final Optional<Logger> logger;
    private Optional<MessageHandler> messageHandler;

    private Optional<Aead> aead = Optional.empty();
    private final String secret;

    SRNDevice(SRNDeviceBuilder builder) {
        messageHandler = Optional.ofNullable(builder.getMessageHandler());
        deviceType = builder.getDeviceType();
        seqNumber = new SequenceNumber();
        secret = builder.getSecret();
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

        logIfNeeded(Level.INFO, "SRN Device initialized\n " +
                "Device Type: " + deviceType + "\n" +
                "Node: " + node + "\n" +
                "Initial Sequence Number: " + seqNumber.get()
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

    private boolean setupCrypto() {
        // https://github.com/google/tink/blob/master/docs/JAVA-HOWTO.md
        try {
            AeadConfig.register();
            // 1. Generate the key material.
            KeysetHandle keysetHandle = KeysetHandle.generateNew(AeadKeyTemplates.AES128_GCM);
            // 2. Get the primitive.
            aead = Optional.of(AeadFactory.getPrimitive(keysetHandle));

            return true;
        } catch (Exception e) {
            logIfNeeded(Level.WARNING, e.getMessage());
            return false;
        }
    }

    private byte[][] buildMessage(long refSeqNumber, String payload) {
        //TODO: Implement logic
        return null;
    }

    private String encryptPayload(String source) {
        // TODO: Implement logic
        return null;
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

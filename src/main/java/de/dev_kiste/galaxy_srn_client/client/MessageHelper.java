package de.dev_kiste.galaxy_srn_client.client;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.AeadFactory;
import com.google.crypto.tink.aead.AeadKeyTemplates;

import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Optional;

/**
 * MessageHelper class used to build the payload and decrypt/encrypt messages
 * 
 * @author Benny Lach
 */
public class MessageHelper {
    private SequenceNumber seqNumber;
    private Aead aead = null;

    private String secret;
    private int maxPayload;

    /**
     * Creates a MessageHelper instance
     *
     * @param secret Used secret to encrypt/decrypt messages
     * @param maxPayload maximum supported size for a single message
     * @throws IllegalStateException if something went wrong hooking up the underlying crypto module
     */
    public MessageHelper(String secret, int maxPayload) throws IllegalStateException {
        seqNumber = new SequenceNumber();

        this.secret = secret;
        this.maxPayload = maxPayload;

        if(!setupCrypto()) {
            throw new IllegalStateException("Failed to setup crypto properly");
        }
    }

    private boolean setupCrypto() {
        // https://github.com/google/tink/blob/master/docs/JAVA-HOWTO.md
        try {
            AeadConfig.register();
            // 1. Generate the key material.
            KeysetHandle keysetHandle = KeysetHandle.generateNew(AeadKeyTemplates.AES128_GCM);
            // 2. Get the primitive.
            aead = AeadFactory.getPrimitive(keysetHandle);

            return true;
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }
    }

    /**
     * Method to build an array of messages from the given values. The payload will be encrypted automatically and
     * multiple messages will be generated if the payload does not fit in one single message.
     *
     * @param refSeqNumber Optional containing a reference sequence number if needed
     * @param payload the payload to send
     * @return generated array of payloads
     * @throws GeneralSecurityException if something went wrong encrypting the payload
     */
    public byte[][] buildMessage(Optional<Long> refSeqNumber, String payload) throws GeneralSecurityException {

        byte[] encrypted = encrypt(payload.getBytes());
        int payloadSize = encrypted.length;
        int messageCount= payloadSize / getMaxPayloadSize();

        byte[][] result = new byte[messageCount][];
        for(int i = 0; i < messageCount; i++) {
            byte[] seqBytes = buildSequenceBytes(seqNumber.getAndUpdate());
            byte[] refBytes = buildSequenceBytes(refSeqNumber.orElse(0L));
            byte[] flags = {buildFlagByte(refSeqNumber.isPresent(), i+1 == messageCount)};

            int index = (i+1 == messageCount && messageCount > 1) ? encrypted.length - 1 : getMaxPayloadSize() * (i+1) - 1;
            byte[] data = Arrays.copyOfRange(encrypted, getMaxPayloadSize() * i, index);

            result[i] = concatenateBytes(seqBytes, refBytes, flags, data);
        }
        return result;
    }

    /**
     * Decrypts the given input and returns the plain payload
     * @param input The input to decrypt
     * @return Decrypted input
     * @throws GeneralSecurityException if somehting went wrong decrypting the input
     */
    public byte[] decrypt(byte[] input) throws GeneralSecurityException {
        return aead.decrypt(input, secret.getBytes());
    }

    /**
     * Encrypts the given input
     *
     * @param input
     * @return encrypted input
     * @throws GeneralSecurityException
     */
    private byte[] encrypt(byte[] input) throws GeneralSecurityException {
        return aead.encrypt(input, secret.getBytes());
    }

    /**
     * Casts the given sequence number into 4 byte array
     * @param input the sequence number
     * @return Casted input
     */
    private byte[] buildSequenceBytes(long input) {
        // Even though long has a size of 8 byte, our sequence numbers just use 4 bytes so it's safe to cast the last
        // 4 byte only.
        return new byte[] {
                (byte) (input >> 24),
                (byte) (input >> 16),
                (byte) (input >> 8),
                (byte) input
        };
    }

    /**
     * Builds the flag byte for the given input
     *
     * @param hasRef boolean indicating if a reference sequence number is available
     * @param isFin boolean indicating if the message, the flag byte belongs to, is the last one
     * @return Calculated flag array
     */
    private byte buildFlagByte(boolean hasRef, boolean isFin) {
        // fin: 1000 0000
        // ref: 0100 0000
        int n = 0;
        if (isFin) n += 128;
        if (hasRef) n += 64;

        return (byte) n;

    }

    /**
     * Concatenates the given byte arrays and returns the new array
     *
     * @param arrays arrays to concatenate
     * @return Concatenated array
     */
    private  byte[] concatenateBytes(byte[]... arrays) {
        // https://www.mkyong.com/java/java-how-to-join-arrays/
        int length = 0;
        for (byte[] array : arrays) {
            length += array.length;
        }
        final byte[] result = new byte[length];

        int offset = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }

        return result;
    }

    private int getMaxPayloadSize() {
        // 32 Bit Sequence number
        // 32 Bit Reference Sequence number (0x0) if no ref was defined
        // 1 Bit for Reference Flag
        // 1 Bit for Fin Flag
        // 6 Bit reserved and unused -
        return maxPayload - 9;
    }
}

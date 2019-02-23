package de.dev_kiste.galaxy_srn_client.client;

import de.dev_kiste.galaxy_srn_client.message.SRNMessage;
import de.dev_kiste.galaxy_srn_client.message.SRNMessageHeader;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

/**
 * MessageHelper class used to build the payload and decrypt/encrypt messages
 * 
 * @author Benny Lach
 */
public class MessageHelper {
    private SequenceNumber seqNumber;
    private SecretKeySpec keySpec;
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

    /**
     * Setups the underlying crypto stack
     *
     * @return <code>boolean</code> indicating if the stack was bootstrapped,
     */
    private boolean setupCrypto() {
        try {
            // https://blog.axxg.de/java-aes-verschluesselung-mit-beispiel/
            // byte-Array erzeugen
            byte[] key = secret.getBytes(StandardCharsets.UTF_8);
            // aus dem Array einen Hash-Wert erzeugen mit MD5 oder SHA
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            key = sha.digest(key);
            // nur die ersten 128 bit nutzen
            key = Arrays.copyOf(key, 16);
            // der fertige Schluessel
            keySpec = new SecretKeySpec(key, "AES");
            return true;
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }
    }

    /**
     * Builds an array of <code>SRNMessage</code> object from the given values. The payload will be encrypted automatically and
     * multiple messages will be generated if the payload does not fit in one single message.
     *
     * @param refSeqNumber Optional containing a reference sequence number if needed
     * @param payload the payload to send
     * @return generated payloads
     * @throws Exception if something went wrong encrypting the payload
     */
    public SRNMessage[] buildMessage(Optional<Long> refSeqNumber, byte topic, String payload) throws Exception {
        byte[] encrypted = encrypt(payload.getBytes());

        int payloadSize = encrypted.length;
        int messageCount = (int) Math.ceil((double) payloadSize / getMaxPayloadSize());

        byte[] initialSeqBytes = null;

        SRNMessage[] result = new SRNMessage[messageCount];

        for(int i = 0; i < messageCount; i++) {
            byte[] seqBytes = buildSequenceBytes(seqNumber.getAndUpdate());

            if(i == 0) {
                initialSeqBytes = seqBytes;
            }
            byte[] refBytes;

            if(i == 0) {
                refBytes = buildSequenceBytes(refSeqNumber.orElse(0L));
            } else {
                refBytes = initialSeqBytes;
            }
            int endIndex = (i+1 == messageCount) ? payloadSize : getMaxPayloadSize() * (i+1);

            byte[] data = Arrays.copyOfRange(encrypted, getMaxPayloadSize() * i, endIndex);
            result[i] = new SRNMessage(seqBytes, refBytes, refSeqNumber.isPresent(), messageCount > 1, i+1 == messageCount, topic, new String(data, StandardCharsets.UTF_8));
        }
        return result;
    }

    /**
     * Builds an <code>SRNMessage</code> object from the given data
     *
     * @param data the reference data
     * @return Generated message
     */
    public SRNMessage getMessageFromBytes(byte[] data) {
        return new SRNMessage(data);
    }

    /**
     * Decrypts the given input - must be base64 encoded - usind AES and returns the plain payload
     *
     * @param input The input to decrypt
     * @return Decrypted input
     * @throws Exception if something went wrong decrypting the input
     */
    public byte[] decrypt(byte[] input) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(input);

        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);

        return cipher.doFinal(decoded);
    }

    /**
     * Returns the beginning index of the payload
     *
     * @return payload begin
     */
    public int getDataOffset() {
        return SRNMessageHeader.headerSize() + 1;
    }

    /**
     * Extracts the payload and creates a encrypted copy of a given byte array.
     *
     * @param source the data to get a copy from
     * @param isEncrypted <code>boolean</code> indicating if the data is already encrypted
     * @return encrypted copy of the payload
     * @throws Exception if something went wrong creating the copy
     */
    byte[] createEncrypedMessageCopy(byte[] source, boolean isEncrypted) throws Exception {
        int offset = getDataOffset();
        byte[] header = Arrays.copyOfRange(source, 0, offset);
        byte[] copy = Arrays.copyOfRange(source, offset, source.length);

        if(!isEncrypted) {
            copy = encrypt(copy);
        }
        return concatenateBytes(header, copy);
    }

    /**
     * Concatenates the given byte arrays and returns the new array
     *
     * @param arrays arrays to concatenate
     * @return Concatenated array
     */
    public byte[] concatenateBytes(byte[]... arrays) {
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

    /**
     * Encrypts the given input using AES
     *
     * @param input
     * @return encrypted input as base64
     * @throws Exception if something went wrong encrypting the input
     */
    private byte[] encrypt(byte[] input) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encrypted = cipher.doFinal(input);

        return Base64.getEncoder().encode(encrypted);
    }

    /**
     * Casts the given sequence number into 4 byte array
     *
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
     * Returns the max allowed payload size in bytes
     *
     * @return size
     */
    private int getMaxPayloadSize() {
        return maxPayload - getDataOffset();
    }
}

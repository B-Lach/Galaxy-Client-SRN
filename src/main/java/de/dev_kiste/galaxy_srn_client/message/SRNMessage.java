package de.dev_kiste.galaxy_srn_client.message;

import java.nio.charset.StandardCharsets;

/**
 * Class that represents a Message in the network
 *
 * @author Benny Lach
 */
public class SRNMessage {
    private SRNMessageHeader header;
    private String payload;

    public SRNMessage(byte[] seqNumber, byte[] refNumber, boolean hasRefNumber, boolean isLastMessage, String payload) {
        // TODO: Check for validity
        header = new SRNMessageHeader(seqNumber, refNumber, hasRefNumber, isLastMessage);
        this.payload = payload;
    }

    public SRNMessage(byte[] data) {
        // TODO: Check for validity
        header = new SRNMessageHeader(data);

        int payloadSize = data.length - SRNMessageHeader.headerSize();
        byte[] payloadBytes = new byte[payloadSize];

        System.arraycopy(data, SRNMessageHeader.headerSize(), payloadBytes, 0, payloadSize);

        payload = new String(payloadBytes, StandardCharsets.UTF_8);
    }

    /**
     * Returns byte representation of the message object
     *
     * @return message in bytes
     */
    public byte[] toBytes() {
        byte[] headerBytes = header.toBytes();
        byte[] data = payload.getBytes(StandardCharsets.UTF_8);

        byte[] result = new byte[headerBytes.length + data.length];

        System.arraycopy(headerBytes, 0, result, 0, headerBytes.length);
        System.arraycopy(data, 0, result, headerBytes.length, data.length);

        return result;
    }

    public SRNMessageHeader getHeader() {
        return header;
    }

    public String getPayload() {
        return payload;
    }
}

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
    private byte topic;

    public SRNMessage(byte[] seqNumber, byte[] refNumber, boolean hasRefNumber, boolean isLastMessage, byte topic, String payload) {
        // TODO: Check for validity
        header = new SRNMessageHeader(seqNumber, refNumber, hasRefNumber, isLastMessage);
        this.topic = topic;
        this.payload = payload;
    }

    public SRNMessage(byte[] data) {
        // TODO: Check for validity
        header = new SRNMessageHeader(data);
        topic = data[SRNMessageHeader.headerSize()];

        int payloadSize = data.length - SRNMessageHeader.headerSize() - 1;
        byte[] payloadBytes = new byte[payloadSize];

        System.arraycopy(data, SRNMessageHeader.headerSize() + 1, payloadBytes, 0, payloadSize);

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

        byte[] result = new byte[1 + headerBytes.length + data.length];

        System.arraycopy(headerBytes, 0, result, 0, headerBytes.length);
        result[headerBytes.length] = topic;
        System.arraycopy(data, 0, result, headerBytes.length + 1, data.length);

        return result;
    }

    public SRNMessageHeader getHeader() {
        return header;
    }

    public String getPayload() {
        return payload;
    }

    public byte getTopic() {
        return topic;
    }
}

package de.dev_kiste.galaxy_srn_client.message;

import de.dev_kiste.galaxy_srn_client.client.MessageHelper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class that represents a multi packet message
 *
 * @author Benny Lach
 */
public class SRNMultiMessage {
    private ArrayList<SRNMessage> messageStack;

    private byte[] sequenceNumber;
    private byte[] referenceNumber;
    private byte topic;

    /**
     * Default initializer
     *
     * @param first The first part of the <code>SRNMultiMessage</code>
     */

    SRNMultiMessage(SRNMessage first) {
        sequenceNumber = first.getHeader().getSeqNumber();
        referenceNumber = first.getHeader().getRefNumber();
        messageStack = new ArrayList<>();
        messageStack.add(first);
        topic = first.getTopic();
    }

    /**
     * Get the seq number of the message
     *
     * @return seq number
     */
    public byte[] getSequenceNumber() {
        return sequenceNumber.clone();
    }

    /**
     * Adds a new <code>SRNMessage</code> to the stack. The message's ref number must be equal to the seq number of the
     * <code>SRNMultiMessage</code>
     *
     * @param message The message to add
     */
    public void addMessage(SRNMessage message) {
        if(Arrays.equals(sequenceNumber, message.getHeader().getRefNumber())) {
            messageStack.add(message);
        }
    }

    /**
     * Returns a <code>boolean</code> indicating the the stack of received <code>SRNMessage</code> objects is complete
     *
     * @return <code>boolean</code> indicating if the message is complete
     */
    public boolean isComplete() {
        return messageStack.get(messageStack.size() - 1).getHeader().isLastMessage();
    }

    public SRNMessage buildMessage(MessageHelper helper) throws Exception {
        String payload = "";

        for(SRNMessage m: messageStack) {
            payload += m.getPayload();
        }
        byte[] decrypted = helper.decrypt(payload.getBytes(StandardCharsets.UTF_8));

        SRNMessage message = new SRNMessage(sequenceNumber, referenceNumber, hasReference(), false, true, topic, new String(decrypted, StandardCharsets.UTF_8));
        return message;
    }

    /**
     * Returns a <code>boolean</code> indicating if the <code>SRNMultiMessage</code> has a ref number
     *
     * @return <code>boolean</code> indicating if it has a ref number;
     */
    private boolean hasReference() {
        return messageStack.get(0).getHeader().hasRefNumber();
    }
}

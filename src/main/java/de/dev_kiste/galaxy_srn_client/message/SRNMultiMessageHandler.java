package de.dev_kiste.galaxy_srn_client.message;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class that is responsible for handling and building multi packet messages
 *
 * @author Benny Lach
 */
public class SRNMultiMessageHandler {
    private ArrayList<SRNMultiMessage> messageStack;
    private SRNMultiMessageReceiver receiver;

    /**
     * Default initializer
     *
     * @param receiver <code>SRNMultiMessageReceiver</code> object which will be triggered on a new message
     */
    public SRNMultiMessageHandler(SRNMultiMessageReceiver receiver) {
        messageStack = new ArrayList<>();
        this.receiver = receiver;
    }

    /**
     * Adds a new received packet of a multi message. Will only be added if the committed <code>SRNMessage</code>
     * is part of a multi message
     *
     * @param message The message to add
     * @return <code>boolean</code> indicating if the message was added
     */
    public boolean addNewMessage(SRNMessage message) {
        if(!message.getHeader().isMultiMessage()) {
            return false;
        }

        SRNMultiMessage reference = null;

        for(SRNMultiMessage m: messageStack) {
            if(Arrays.equals(m.getSequenceNumber(), message.getHeader().getRefNumber())) {
                reference = m;

                break;
            }
        }

        if(reference != null) {
            reference.addMessage(message);
        } else {
            reference = new SRNMultiMessage(message);
            messageStack.add(reference);
        }

        if(reference.isComplete()) {
            messageStack.remove(reference);
            receiver.handleNewMessage(reference);
        }
        return true;
    }
}

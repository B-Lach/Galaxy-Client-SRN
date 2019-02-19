package de.dev_kiste.galaxy_srn_client.client;

import de.dev_kiste.galaxy_srn_client.message.SRNMessage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;


/**
 * Singleton to prevent broadcasting already seen/self created messages in the network
 *
 * @author Benny Lach
 */
public class LoopDetector {
    private static LoopDetector detector;

    public static synchronized LoopDetector getInstance() {
        if(detector == null) {
            detector = new LoopDetector();
        }
        return detector;
    }

    private class MessageRef {
        byte[] seqNumber;
        long timestamp;
        int hashCode;

        MessageRef(byte[] seqNumber, long timestamp, int hashCode) {
            this.seqNumber = seqNumber;
            this.timestamp = timestamp;
            this.hashCode = hashCode;
        }
    }

    private final static long RESET_DELTA = 6000;

    private ArrayList<MessageRef> incomingMessages;
    private ArrayList<MessageRef> outgoingMessages;

    private LoopDetector() {
        incomingMessages = new ArrayList<>();
        outgoingMessages = new ArrayList<>();
    }

    public synchronized void willSendMessage(SRNMessage message) {
        outgoingMessages.add(createMessageRef(message));
    }

    public synchronized boolean haveMessageSeenBefore(SRNMessage message) {
        boolean seenBefore = false;

        for(MessageRef ref: incomingMessages) {
           if(Arrays.equals(ref.seqNumber, message.getHeader().getSeqNumber()) && ref.hashCode == message.hashCode()) {
               seenBefore = true;
           }
        }

        if(seenBefore) {
            return true;
        }

        for(MessageRef ref: outgoingMessages) {
            if(Arrays.equals(ref.seqNumber, message.getHeader().getSeqNumber()) && ref.hashCode == message.hashCode()) {
                seenBefore = true;
            }
        }

        if(!seenBefore) {
            incomingMessages.add(createMessageRef(message));
        }

        return seenBefore;
    }

    public synchronized void reset() {
        long now = Instant.now().getEpochSecond();

        reset(incomingMessages, now);
        reset(outgoingMessages, now);
    }

    private void reset(ArrayList<MessageRef> list, long timestamp) {
        Iterator<MessageRef> iterator= list.iterator();

        while (iterator.hasNext()) {
            MessageRef ref = iterator.next();
            if(timestamp - ref.timestamp > RESET_DELTA) {
                iterator.remove();
            }
        }
    }

    private MessageRef createMessageRef(SRNMessage message) {
        MessageRef ref = new MessageRef(message.getHeader().getSeqNumber(), Instant.now().getEpochSecond(), message.hashCode());

        return ref;
    }
}

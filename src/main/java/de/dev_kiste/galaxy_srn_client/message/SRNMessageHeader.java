package de.dev_kiste.galaxy_srn_client.message;

/**
 * Class that represents the header of <code>SRNMessage</code> in the network
 * @author Benny Lach
 */
public class SRNMessageHeader {
    private final static int HEADER_SIZE = 9;

    private byte[] seqNumber;
    private byte[] refNumber;
    private boolean hasRefNumber;
    private boolean isMultiMessage;
    private boolean isLastMessage;

    SRNMessageHeader(byte[] seqNumber, byte[] refNumber, boolean hasRefNumber, boolean isMultiMessage, boolean isLastMessage) {
        // TODO: Check for validity
        this.seqNumber = seqNumber;
        this.refNumber = refNumber;
        this.hasRefNumber = hasRefNumber;
        this.isLastMessage = isLastMessage;
        this.isMultiMessage = isMultiMessage;
    }

    SRNMessageHeader(byte[] data) {
        // TODO: Check for validity
        seqNumber = new byte[]{data[0], data[1], data[2], data[3]};
        refNumber = new byte[]{data[4], data[5], data[6], data[7]};

        setFlags(data[8]);
    }

    public byte[] getSeqNumber() {
        return seqNumber.clone();
    }

    public byte[] getRefNumber() {
        return refNumber.clone();
    }

    public boolean hasRefNumber() {
        return hasRefNumber;
    }

    public boolean isLastMessage() {
        return isLastMessage;
    }

    public boolean isMultiMessage() { return isMultiMessage; }

    /**
     * Returns the size of the header in bytes
     *
     * @return header size
     */
    public static int headerSize() {
        // 32 Bit Sequence number
        // 32 Bit Reference Sequence number (0x0) if no ref was defined
        // 1 Bit for Reference Flag
        // 1 Bit for Multi Flag
        // 1 Bit for Fin Flag
        // 5 Bit reserved and unused -
        return HEADER_SIZE;
    }

    /**
     * Returns byte representation of this header object
     *
     * @return header in bytes
     */
    public byte[] toBytes() {
        byte[] bytes = new byte[9];
        // Add seq number
        System.arraycopy(seqNumber, 0, bytes, 0, seqNumber.length);
        // Add ref number
        System.arraycopy(refNumber, 0, bytes, 4, refNumber.length);
        // Add Flag byte
        System.arraycopy(buildFlagByte(), 0, bytes, 8, 1);

        return bytes;
    }

    private void setFlags(byte ref) {
        hasRefNumber = ((ref >> 7) & 1) == 1;
        isMultiMessage= ((ref >> 6) & 1) == 1;
        isLastMessage = ((ref >> 5) & 1) == 1;
    }
    /**
     * Builds the flag byte
     *
     * @return Calculated flag array
     */
    private byte[] buildFlagByte() {
        // ref:     1000 000
        // multi:   0100 0000
        // fin:     0010 0000

        int n = 0x0;

        if (hasRefNumber) n += 0x80;
        if (isMultiMessage) n += 0x40;
        if (isLastMessage) n += 0x20;

        return new byte[] {(byte) n};
    }
}

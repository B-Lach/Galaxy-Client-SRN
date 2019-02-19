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
    private boolean isLastMessage;

    SRNMessageHeader(byte[] seqNumber, byte[] refNumber, boolean hasRefNumber, boolean isLastMessage) {
        // TODO: Check for validity
        this.seqNumber = seqNumber;
        this.refNumber = refNumber;
        this.hasRefNumber = hasRefNumber;
        this.isLastMessage = isLastMessage;
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

    /**
     * Returns the size of the header in bytes
     *
     * @return header size
     */
    public static int headerSize() {
        // 32 Bit Sequence number
        // 32 Bit Reference Sequence number (0x0) if no ref was defined
        // 1 Bit for Reference Flag
        // 1 Bit for Fin Flag
        // 6 Bit reserved and unused -
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
        isLastMessage = ((ref >> 7) & 1) == 1;
        hasRefNumber = ((ref >> 6) & 1) == 1;
    }
    /**
     * Builds the flag byte
     *
     * @return Calculated flag array
     */
    private byte[] buildFlagByte() {
        // fin: 1000 0000
        // ref: 0100 0000
        int n = 0;
        if (isLastMessage) n += 128;
        if (hasRefNumber) n += 64;

        return new byte[] {(byte) n};
    }
}

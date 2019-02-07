package de.dev_kiste.galaxy_srn_client.client;

import java.util.Random;

/**
 * Sequence Number object
 *
 * @author Benny Lach
 */
public class SequenceNumber {
    private static final long MIN_VALUE = 0x00000000L;
    private static final long MAX_VALUE = 0xFFFFFFFFL;
    private long value;

    /**
     * Creates a new sequence number object with a random generated start value
     */
    public SequenceNumber() {
        this.value = getRandomUnsignedInt();
    }

    /**
     * Creates a new sequence number object with a given start value
     *
     * @param value The start value
     * @throws IllegalArgumentException if the committed value is out of range
     */
    public SequenceNumber(long value) throws IllegalArgumentException {
        if(value < MIN_VALUE || value > MAX_VALUE) {
            throw new IllegalArgumentException("Committed value is not in range of " + MIN_VALUE + " - " + MAX_VALUE);
        }
        this.value = value;
    }
    /**
     * Increments the current value and returns it. If current value equals 0xFFFFFFFFL it will fall back to 0x00000000L
     *
     * @return new sequence number
     */
    public long next() {
        if(value == MAX_VALUE) {
            value = MIN_VALUE;
        } else {
            value++;
        }
        return value;
    }

    /**
     * Get the current sequence number
     *
     * @return current value
     */
    public long get() {
        return value;
    }

    /**
     * Generates a random number between 0x00000000L - 0xFFFFFFFFL
     *
     * @return gernerated number
     */
    private long getRandomUnsignedInt() {
        byte[] bytes = new byte[4];

        new Random().nextBytes(bytes);

        // With the help of https://stackoverflow.com/a/15184268
        long unsigned = ((bytes[0] & 0x00000000000000FFL) <<  0) |
                ((bytes[1] & 0x00000000000000FFL) <<  8) |
                ((bytes[2] & 0x00000000000000FFL) << 16) |
                ((bytes[3] & 0x00000000000000FFL) << 24);

        return unsigned;
    }
}

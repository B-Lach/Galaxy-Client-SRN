package de.dev_kiste.galaxy_srn_client.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for SequenceNumber class
 *
 * @author Benny Lach
 */
public class SequenceNumberTest {

    @Test
    public void initWithRandomGeneratedValueShouldBeInRange() {
        SequenceNumber seq = new SequenceNumber();

        assertAll(
                () -> assertTrue(seq.get() >= 0x00000000L),
                () -> assertTrue(seq.get() <= 0xFFFFFFFFL)
        );
    }

    @Test
    public void incrementSequenceNumberShouldBeCorrect() {
        SequenceNumber seq = new SequenceNumber();
        long current = seq.get();
        long next = seq.next();

        assertAll(
                () -> assertEquals(next, seq.get()),
                () -> {
                    if (current == 0xFFFFFFFFL) {
                        assertEquals(0x00000000F, next);
                    } else {
                        assertEquals(current + 1, next);
                    }
                });
    }

    @Test
    public void initWithInvalidValueShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> new SequenceNumber(0x100000000L));
    }

    @Test
    public void initWithValidValusShouldNotThrow() {
        SequenceNumber seq = new SequenceNumber(0xFFL);

        assertEquals(0xFFL, seq.get());
    }

    @Test
    public void incrementMaxShouldReturnToMin() {
        SequenceNumber seq = new SequenceNumber(0xFFFFFFFFL);
        seq.next();

        assertEquals(0x0L, seq.get());
    }

}

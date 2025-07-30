package org.distributed.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Oleksandr Havrylenko
 **/
class IdGeneratorTest {

    @Test
    void idTest() {
        int id = IdGenerator.id();
        int nextIndex = IdGenerator.getNextIndex();
        int previousIndex = IdGenerator.getPreviousIndex();
        assertEquals(0, id);
        assertEquals(1, nextIndex);
        assertEquals(-1, previousIndex);

        id = IdGenerator.id();
        nextIndex = IdGenerator.getNextIndex();
        previousIndex = IdGenerator.getPreviousIndex();
        assertEquals(1, id);
        assertEquals(2, nextIndex);
        assertEquals(0, previousIndex);
    }

    @Test
    void commitedTest() {
        IdGenerator.setId(5);
        int nextIndex = IdGenerator.getNextIndex();
        int previousIndex = IdGenerator.getPreviousIndex();
        assertEquals(5, nextIndex);
        assertEquals(4, previousIndex);

        int id = IdGenerator.id();
        nextIndex = IdGenerator.getNextIndex();
        previousIndex = IdGenerator.getPreviousIndex();
        assertEquals(5, id);
        assertEquals(6, nextIndex);
        assertEquals(4, previousIndex);
    }
}
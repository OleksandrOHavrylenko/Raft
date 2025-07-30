package org.distributed.repository;

import org.distributed.model.dto.LogItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Oleksandr Havrylenko
 **/
class LogRepositoryTest {

    @Test
    void testAdd() {
        LogRepository repo = new LogRepository();
        repo.add(new LogItem(0, "msg0", 1L));
        repo.add(new LogItem(1, "msg1", 1L));
        repo.add(new LogItem(2, "msg2", 1L));
        repo.add(new LogItem(3, "msg3", 1L));
        repo.add(new LogItem(4, "msg4", 1L));
        List<String> all = repo.getAll(4);
        assertEquals(4, all.size());
        assertEquals("msg0", all.get(0));
        assertEquals("msg1", all.get(1));
        assertEquals("msg2", all.get(2));
        assertEquals("msg3", all.get(3));
        repo.add(new LogItem(5, "msg6", 2L));
        all = repo.getAll(6);
        assertEquals(6, all.size());
        assertEquals(List.of("msg0", "msg1", "msg2", "msg3", "msg4","msg6"), all);
    }

    @Test
    void testAdd2() {
        LogRepository repo = new LogRepository();
        repo.add(new LogItem(0, "msg0", 1L));
        repo.add(new LogItem(1, "msg1", 1L));

        List<String> all = repo.getAll(2);
        assertEquals(2, all.size());
        assertEquals("msg0", all.get(0));
        assertEquals("msg1", all.get(1));
    }
}
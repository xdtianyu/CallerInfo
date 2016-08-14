package org.xdty.callerinfo;

import org.junit.Test;
import org.xdty.callerinfo.data.CallerRepository;

import static org.junit.Assert.assertEquals;

public class CallerRepositoryTest {

    @Test
    public void testFixNumber() {
        assertEquals(CallerRepository.fixNumber("+400123456"), "400123456");
        assertEquals(CallerRepository.fixNumber("+8612345678"), "12345678");
        assertEquals(CallerRepository.fixNumber("8612345678"), "12345678");
        assertEquals(CallerRepository.fixNumber("861258312345678"), "12345678");
    }

}

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
        assertEquals(CallerRepository.fixNumber("8612583112345678"), "12345678");
        assertEquals(CallerRepository.fixNumber("8612583212345678"), "12345678");
        assertEquals(CallerRepository.fixNumber("8612583312345678"), "12345678");
        assertEquals(CallerRepository.fixNumber("12583312345678"), "12345678");
        assertEquals(CallerRepository.fixNumber("125902312345678"), "12345678");
        assertEquals(CallerRepository.fixNumber("118334812345678"), "12345678");
    }

}

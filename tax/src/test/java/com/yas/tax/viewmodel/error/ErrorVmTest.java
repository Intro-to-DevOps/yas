package com.yas.tax.viewmodel.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ErrorVmTest {

    @Test
    void testErrorVm_primaryConstructor() {
        ErrorVm errorVm = new ErrorVm("400", "Bad Request", "Detail error message", List.of("Field error 1"));

        assertEquals("400", errorVm.statusCode());
        assertEquals("Bad Request", errorVm.title());
        assertEquals("Detail error message", errorVm.detail());
        assertEquals(1, errorVm.fieldErrors().size());
        assertEquals("Field error 1", errorVm.fieldErrors().getFirst());
    }

    @Test
    void testErrorVm_secondaryConstructor() {
        ErrorVm errorVm = new ErrorVm("500", "Internal Server Error", "Detail message");

        assertEquals("500", errorVm.statusCode());
        assertEquals("Internal Server Error", errorVm.title());
        assertEquals("Detail message", errorVm.detail());
        assertTrue(errorVm.fieldErrors().isEmpty());
    }
}

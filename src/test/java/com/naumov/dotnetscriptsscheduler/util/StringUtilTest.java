package com.naumov.dotnetscriptsscheduler.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilTest {

    @Test
    void omitLongString() {
        assertNull(StringUtil.omitLongString(null));
        assertEquals("<CONTENT OMITTED>", StringUtil.omitLongString(""));
        assertEquals("<CONTENT OMITTED>", StringUtil.omitLongString("null"));
        assertEquals("<CONTENT OMITTED>", StringUtil.omitLongString("string"));
    }
}
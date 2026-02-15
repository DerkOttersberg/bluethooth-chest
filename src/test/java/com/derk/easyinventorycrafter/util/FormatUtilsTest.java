package com.derk.easyinventorycrafter.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FormatUtilsTest {

	@Test
	void formatCount_zero() {
		assertEquals("0", FormatUtils.formatCount(0));
	}

	@Test
	void formatCount_belowThousand() {
		assertEquals("1", FormatUtils.formatCount(1));
		assertEquals("999", FormatUtils.formatCount(999));
		assertEquals("42", FormatUtils.formatCount(42));
	}

	@Test
	void formatCount_thousands() {
		assertEquals("1k", FormatUtils.formatCount(1000));
		assertEquals("1k", FormatUtils.formatCount(1500));
		assertEquals("1k", FormatUtils.formatCount(1999));
		assertEquals("10k", FormatUtils.formatCount(10000));
		assertEquals("999k", FormatUtils.formatCount(999999));
	}

	@Test
	void formatCount_millions() {
		assertEquals("1M", FormatUtils.formatCount(1000000));
		assertEquals("1M", FormatUtils.formatCount(1500000));
		assertEquals("999M", FormatUtils.formatCount(999999999));
	}

	@Test
	void formatCount_billions() {
		assertEquals("1B", FormatUtils.formatCount(1000000000));
		assertEquals("2B", FormatUtils.formatCount(2000000000));
		assertEquals("2B", FormatUtils.formatCount(Integer.MAX_VALUE));
	}

	@Test
	void formatCount_negative() {
		// Negative counts shouldn't normally occur, but verify no crash
		assertEquals("-1", FormatUtils.formatCount(-1));
		assertEquals("-999", FormatUtils.formatCount(-999));
	}
}

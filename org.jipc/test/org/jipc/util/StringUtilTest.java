package org.jipc.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class StringUtilTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testSplitLines() throws Exception {
		assertEquals(Arrays.asList("herbert", "karl", "franz"),
				StringUtil.splitLines("herbert\nkarl\nfranz\n"));

		assertEquals(Arrays.asList("herbert", "karl", "franz"),
				StringUtil.splitLines("herbert\nkarl\nfranz"));

		assertEquals(Arrays.asList("herbert", "karl heinz", "franz"),
				StringUtil.splitLines("herbert\nkarl heinz\nfranz\n"));

		assertEquals(Arrays.asList("herbert", "", "franz"),
				StringUtil.splitLines("herbert\n\nfranz\n"));

		assertEquals(Arrays.asList("", "", ""),
				StringUtil.splitLines("\n\n\n"));

		assertEquals(Arrays.asList(), StringUtil.splitLines(""));
	}


	@Test
	public void testSplit() {
		assertEquals(Arrays.asList("herbert", "karl", "franz"),
				StringUtil.split("herbert,karl,franz", ','));

		assertEquals(Arrays.asList("herbert", "karl", "franz"),
				StringUtil.split("herbert,karl,franz", ','));

		assertEquals(Arrays.asList("herbert", "karl heinz", "franz"),
				StringUtil.split("herbert,karl heinz,franz", ','));

		assertEquals(Arrays.asList("herbert", "", "franz"),
				StringUtil.split("herbert,,franz", ','));

		assertEquals(Arrays.asList("", "", ""),
				StringUtil.split(",,,", ','));

		assertEquals(Arrays.asList(), StringUtil.splitLines(""));
	}

	@Test
	public void testJoin() {
		assertEquals("karl,heinz,fritz", StringUtil.join("karl", "heinz", "fritz"));
		assertEquals("karl", StringUtil.join("karl"));
		assertEquals("", StringUtil.join());
	}

	@Test
	public void testJoinDelimiter() {
		assertEquals("karl&heinz&fritz", StringUtil.join('&',"karl", "heinz", "fritz"));
		assertEquals("karl", StringUtil.join('&',"karl"));
		assertEquals("", StringUtil.join('&'));
	}

}

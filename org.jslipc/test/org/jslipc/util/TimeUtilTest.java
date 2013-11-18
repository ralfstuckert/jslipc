package org.jslipc.util;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.InterruptedIOException;
import java.nio.channels.InterruptedByTimeoutException;

import org.jslipc.util.TimeUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link TimeUtil} class.
 */
public class TimeUtilTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testSleep() throws Exception {
		// enough time left
		long now = System.currentTimeMillis();
		expectSlept(300, 300, 2000, now - 1000); 
		now = System.currentTimeMillis();
		expectSlept(200, 200, 2000, now - 1500);

		// expect timeout
		now = System.currentTimeMillis();
		expectSleepTimeout(0, 500, 2000, now - 2000);
		now = System.currentTimeMillis();
		expectSleepTimeout(500, 1000, 2000, now - 1500);

		// timeout 0 means infinitely, so no exception
		now = System.currentTimeMillis();
		expectSlept(200, 200, 0, now);

		now = System.currentTimeMillis();
		expectSlept(200, 200, 0, now+1000);
	}

	private void expectSlept(long expectedTimeSlept, final long toSleep,
			final int timeout, final long waitingSince)
			throws InterruptedIOException, InterruptedByTimeoutException {
		long start = System.currentTimeMillis();
		TimeUtil.sleep(toSleep, timeout, waitingSince);
		long slept = System.currentTimeMillis() - start;
		assertThat(slept, not(lessThan(expectedTimeSlept)));
	}

	private void expectSleepTimeout(long expectedTimeSlept, final long toSleep,
			final int timeout, final long waitingSince) throws InterruptedIOException {
		long start = System.currentTimeMillis();
		try {
			TimeUtil.sleep(toSleep, timeout, waitingSince);
			fail("expected timeout");
		} catch (InterruptedByTimeoutException e) {
			// expected
		}
		long slept = System.currentTimeMillis() - start;
		assertThat(slept, not(lessThan(expectedTimeSlept)));
	}

	@Test
	public void testGetTimeToSleep() {
		long now = System.currentTimeMillis();
		assertThat(TimeUtil.getTimeToSleep(1000, 10000, now - 5000),
				equalTo(1000l));
		now = System.currentTimeMillis();
		assertThat(TimeUtil.getTimeToSleep(500, 10000, now - 5000),
				equalTo(500l));

		now = System.currentTimeMillis();
		assertThat(TimeUtil.getTimeToSleep(1000, 10000, now - 9500),
				lessThanOrEqualTo(500l));

		now = System.currentTimeMillis();
		assertThat(TimeUtil.getTimeToSleep(1000, 10000, now - 9500),
				lessThanOrEqualTo(500l));

		// timeout 0 means infinitely, so give full sleep time
		now = System.currentTimeMillis();
		assertThat(TimeUtil.getTimeToSleep(1000, 0, now),
				not(lessThan(1000l)));
		now = System.currentTimeMillis();
		assertThat(TimeUtil.getTimeToSleep(2000, 0, now+1000),
				not(lessThan(2000l)));
	}

	@Test
	public void testCheckForTimeout() throws Exception {
		long now = System.currentTimeMillis();
		TimeUtil.checkForTimeout(2000, now - 1000);
		TimeUtil.checkForTimeout(5000, now - 1000);

		expectCheckForTimeoutFails(999, now - 1000);
		expectCheckForTimeoutFails(1000, now - 1000);
		
		// timeout 0 means infinitely, so no exception
		TimeUtil.checkForTimeout(0, now);
		TimeUtil.checkForTimeout(0, now+1000);
	}

	private void expectCheckForTimeoutFails(final int timeout,
			final long waitingSince) {
		try {
			TimeUtil.checkForTimeout(timeout, waitingSince);
			fail("expected timeout");
		} catch (InterruptedByTimeoutException e) {
			// expected
		}
	}

}

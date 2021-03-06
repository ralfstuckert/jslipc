package org.jslipc.util;


/**
 * A bunch of utility methods on dealing with time.
 */
public class TimeUtil {

	/**
	 * The default timeout used in {@link #sleep(int, long)}.
	 */
	public static final int DEFAULT_SLEEP_TIME = 100;

	/**
	 * Sleeps the {@link #DEFAULT_SLEEP_TIME default} time or either the thread
	 * was interrupted or a timeout occurred.
	 * 
	 * @param timeout
	 *            the ms to wait at all before a timeout occurs, where
	 *            <code>0</code> means infinity. Must be >= 0.
	 * @param waitingSince
	 *            the timestamp since when the blocking method is waiting in ms.
	 * @throws InterruptedException
	 *             if the sleep() has been interrupted or a timeout occurred.
	 */
	public static void sleep(final int timeout, final long waitingSince)
			throws InterruptedException {
		sleep(DEFAULT_SLEEP_TIME, timeout, waitingSince);
	}

	/**
	 * Sleeps the given amount of time or either the thread was interrupted or a
	 * timeout occurred.
	 * 
	 * @param toSleep
	 *            the time to sleep in ms.
	 * @param timeout
	 *            the ms to wait at all before a timeout occurs, where
	 *            <code>0</code> means infinity. Must be >= 0.
	 * @param waitingSince
	 *            the timestamp since when the blocking method is waiting in ms.
	 * @throws InterruptedException
	 *             if the sleep() has been interrupted or a timeout occurred.
	 */
	public static void sleep(final long toSleep, final int timeout,
			final long waitingSince) throws InterruptedException {
			checkForTimeout(timeout, waitingSince);

			long timeToSleep = getTimeToSleep(toSleep, timeout, waitingSince);
			if (timeToSleep > 0) {
				Thread.sleep(timeToSleep);
			}
			checkForTimeout(timeout, waitingSince);
	}

	protected static long getTimeToSleep(final long toSleep, final int timeout,
			final long waitingSince) {
		if (timeout <= 0) {
			// timeout 0 means infinity
			return toSleep;
		}
		return Math.min(toSleep, waitingSince - System.currentTimeMillis()
				+ timeout);
	}

	/**
	 * Throws a InterruptedException if a timeout occurred.
	 * 
	 * @param timeout
	 *            the ms to wait at all before a timeout occurs.
	 * @param waitingSince
	 *            the timestamp since when the blocking method is waiting in ms.
	 * @throws InterruptedException
	 *             if a timeout occurred.
	 */
	public static void checkForTimeout(final int timeout,
			final long waitingSince) throws InterruptedException {
		if (timeout > 0 && System.currentTimeMillis() >= waitingSince + timeout) {
			throw new InterruptedException("interrupted due to timeout");
		}
	}

}

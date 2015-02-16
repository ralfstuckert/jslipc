package org.jslipc.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

import org.jslipc.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** 
 * Tests for class {@link HostDir}.
 */
public class HostDirTest {

	private File dir;
	private RandomAccessFile raf;
	private FileLock lock;
	private HostDir hostDir;

	@Before
	public void setUp() {
		dir = TestUtil.createDirectory();
	}

	@After
	public void tearDown() throws Exception {
		if (lock != null && lock.isValid()) {
			lock.release();
		}
		FileUtil.closeSilent(raf);
		FileUtil.closeSilent(hostDir);
		FileUtil.delete(dir);
	}
	
	private void setupLock() throws Exception {
		File lockFile = new File(dir, HostDir.HOST_LOCK_FILE);
		raf = new RandomAccessFile(lockFile, "rw");
		lock = HostDir.lock(raf);
	}

	@Test
	public void testHostDir() throws Exception {
		setupLock();
		hostDir = new HostDir(dir, raf, lock);

		try {
			new HostDir(null, raf, lock);
			fail("expected IllegalArgumentExcception since directory is null");
		} catch (IllegalArgumentException e) {
			// expected
		}
		try {
			new HostDir(dir, null, lock);
			fail("expected IllegalArgumentExcception since lock file is null");
		} catch (IllegalArgumentException e) {
			// expected
		}
		try {
			new HostDir(dir, raf, null);
			fail("expected IllegalArgumentExcception since lock is null");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

	@Test
	public void testGetDirectory() throws Exception {
		setupLock();
		hostDir = new HostDir(dir, raf, lock);
		assertSame(dir, hostDir.getDirectory());
	}

	@Test
	public void testIsActive() throws Exception {
		setupLock();
		hostDir = new HostDir(dir, raf, lock);
		assertTrue(hostDir.isActive());

		lock.release();
		assertFalse(hostDir.isActive());
	}

	@Test
	public void testClose() throws Exception {
		setupLock();
		hostDir = new HostDir(dir, raf, lock);
		assertTrue(lock.isValid());

		hostDir.close();
		assertFalse(lock.isValid());
	}


	
	@Test
	public void testCreate() throws IOException {
		hostDir = HostDir.create(dir);
		assertNotNull(hostDir);

		File hostDirectory = hostDir.getDirectory();
		assertNotNull(hostDirectory);
		assertTrue(hostDirectory.exists());

		File lockFile = new File(dir, HostDir.HOST_LOCK_FILE);
		assertTrue(lockFile.exists());
		raf = new RandomAccessFile(lockFile, "rw");
		String dirName = raf.readUTF();
		assertEquals(dirName, hostDirectory.getName());
	}

	@Test
	public void testCreateTwice() throws IOException {
		hostDir = HostDir.create(dir);
		assertNotNull(hostDir);

		try {
			HostDir secondHostDir = HostDir.create(dir);
			FileUtil.closeSilent(secondHostDir);
			fail("expected exception since host is still active");
		} catch (ActiveHostException e) {
			assertEquals(hostDir.getDirectory(), e.getActiveHostDirectory());
		}
	}

	@Test
	public void testCreateWithExistingLockFile()
			throws IOException {
		File oldHostDirectory = FileUtil.createDirectory(dir);
		File lockFile = new File(dir, HostDir.HOST_LOCK_FILE);
		raf = new RandomAccessFile(lockFile, "rwd");
		raf.writeUTF(oldHostDirectory.getName());
		raf.close();

		hostDir = HostDir.create(dir);
		assertNotNull(hostDir);

		File hostDirectory = hostDir.getDirectory();
		assertNotNull(hostDirectory);
		assertTrue(hostDirectory.exists());

		assertTrue(lockFile.exists());
		raf = new RandomAccessFile(lockFile, "rw");
		String dirName = raf.readUTF();
		assertEquals(dirName, hostDirectory.getName());
	}

	@Test
	public void testCreateRemovesOldDirs() throws IOException {
		File oldDir1 = createDirWithFiles(dir);
		File oldDir2 = createDirWithFiles(dir);
		File oldDir3 = createDirWithFiles(dir);

		File lockFile = new File(dir, HostDir.HOST_LOCK_FILE);
		raf = new RandomAccessFile(lockFile, "rw");
		raf.writeUTF(oldDir3.getName());

		hostDir = HostDir.create(dir);
		assertNotNull(hostDir);

		File hostDirectory = hostDir.getDirectory();
		assertNotNull(hostDirectory);
		assertTrue(hostDirectory.exists());

		assertFalse(oldDir1.exists());
		assertFalse(oldDir2.exists());
		assertFalse(oldDir3.exists());
	}

	@Test
	public void testCreateFails() throws IOException {
		File hostDirectory = FileUtil.createDirectory(dir);
		File lockFile = new File(dir, HostDir.HOST_LOCK_FILE);
		raf = new RandomAccessFile(lockFile, "rw");
		raf.writeUTF(hostDirectory.getName());
		lock = HostDir.lock(raf);

		try {
			hostDir = HostDir.create(dir);
			fail("expected ActiveHostException");
		} catch (ActiveHostException e) {
			assertEquals(hostDirectory, e.getActiveHostDirectory());
		}
	}

	@Test(timeout = 10000)
	public void testCreateFailsSecondJVM() throws Exception {
		Process server = Runtime.getRuntime().exec(
				new String[] { TestUtil.getJvm(), "-cp",
						TestUtil.getTestClassPath(),
						TestServer.class.getName(), dir.getAbsolutePath() });
		try {
			while (HostDir.getActive(dir) == null) {
				Thread.sleep(500);
			}

			hostDir = HostDir.create(dir);
			fail("expected ActiveHostException");
		} catch (ActiveHostException e) {
			assertEquals(dir, e.getActiveHostDirectory().getParentFile());
		} finally {
			server.destroy();
		}
	}

	@Test
	public void testGetActiveHostDirectory() throws IOException {
		File hostDirectory = FileUtil.createDirectory(dir);
		File lockFile = new File(dir, HostDir.HOST_LOCK_FILE);

		assertNull(HostDir.getActive(dir));

		assertTrue(lockFile.createNewFile());
		assertNull(HostDir.getActive(dir));

		raf = new RandomAccessFile(lockFile, "rw");
		raf.writeUTF(hostDirectory.getName());
		assertNull(HostDir.getActive(dir));

		lock = HostDir.lock(raf);
		assertNotNull(lock);
		assertEquals(hostDirectory, HostDir.getActive(dir));

		lock.release();
		assertNull(HostDir.getActive(dir));
	}

	@Test(timeout = 10000)
	public void testGetActiveHostDirectorySecondJVM() throws Exception {
		Process server = Runtime.getRuntime().exec(
				new String[] { TestUtil.getJvm(), "-cp",
						TestUtil.getTestClassPath(),
						TestServer.class.getName(), dir.getAbsolutePath() });
		try {
			while (HostDir.getActive(dir) == null) {
				Thread.sleep(500);
			}
		} finally {
			server.destroy();
		}
	}

	@Test
	public void testCreateAndGetActiveHostDirectory() throws IOException {
		assertNull(HostDir.getActive(dir));

		hostDir = HostDir.create(dir);
		assertEquals(hostDir.getDirectory(),
				HostDir.getActive(dir));

		hostDir.close();
		assertNull(HostDir.getActive(dir));
	}

	private File createDirWithFiles(final File parentDir) throws IOException {
		File newDir = FileUtil.createDirectory(parentDir);
		FileUtil.createFile(newDir);
		FileUtil.createFile(newDir);
		return newDir;
	}

}

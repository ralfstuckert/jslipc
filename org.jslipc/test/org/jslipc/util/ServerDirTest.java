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

public class ServerDirTest {

	private File dir;
	private RandomAccessFile raf;
	private FileLock lock;
	private ServerDir serverDir;

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
		FileUtil.closeSilent(serverDir);
		FileUtil.delete(dir);
	}
	
	private void setupLock() throws Exception {
		File lockFile = new File(dir, ServerDir.SERVER_LOCK_FILE);
		raf = new RandomAccessFile(lockFile, "rw");
		lock = ServerDir.lock(raf);
	}

	@Test
	public void testServerDir() throws Exception {
		setupLock();
		serverDir = new ServerDir(dir, raf, lock);

		try {
			new ServerDir(null, raf, lock);
			fail("expected IllegalArgumentExcception since directory is null");
		} catch (IllegalArgumentException e) {
			// expected
		}
		try {
			new ServerDir(dir, null, lock);
			fail("expected IllegalArgumentExcception since lock file is null");
		} catch (IllegalArgumentException e) {
			// expected
		}
		try {
			new ServerDir(dir, raf, null);
			fail("expected IllegalArgumentExcception since lock is null");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

	@Test
	public void testGetDirectory() throws Exception {
		setupLock();
		serverDir = new ServerDir(dir, raf, lock);
		assertSame(dir, serverDir.getDirectory());
	}

	@Test
	public void testIsActive() throws Exception {
		setupLock();
		serverDir = new ServerDir(dir, raf, lock);
		assertTrue(serverDir.isActive());

		lock.release();
		assertFalse(serverDir.isActive());
	}

	@Test
	public void testClose() throws Exception {
		setupLock();
		serverDir = new ServerDir(dir, raf, lock);
		assertTrue(lock.isValid());

		serverDir.close();
		assertFalse(lock.isValid());
	}


	
	@Test
	public void testCreate() throws IOException {
		serverDir = ServerDir.create(dir);
		assertNotNull(serverDir);

		File serverDirectory = serverDir.getDirectory();
		assertNotNull(serverDirectory);
		assertTrue(serverDirectory.exists());

		File lockFile = new File(dir, ServerDir.SERVER_LOCK_FILE);
		assertTrue(lockFile.exists());
		raf = new RandomAccessFile(lockFile, "rw");
		String dirName = raf.readUTF();
		assertEquals(dirName, serverDirectory.getName());
	}

	@Test
	public void testCreateTwice() throws IOException {
		serverDir = ServerDir.create(dir);
		assertNotNull(serverDir);

		try {
			ServerDir secondServerDir = ServerDir.create(dir);
			FileUtil.closeSilent(secondServerDir);
			fail("expected exception since server is still active");
		} catch (ServerActiveException e) {
			assertEquals(serverDir.getDirectory(), e.getActiveServerDirectory());
		}
	}

	@Test
	public void testCreateWithExistingLockFile()
			throws IOException {
		File oldServerDirectory = FileUtil.createDirectory(dir);
		File lockFile = new File(dir, ServerDir.SERVER_LOCK_FILE);
		raf = new RandomAccessFile(lockFile, "rwd");
		raf.writeUTF(oldServerDirectory.getName());
		raf.close();

		serverDir = ServerDir.create(dir);
		assertNotNull(serverDir);

		File serverDirectory = serverDir.getDirectory();
		assertNotNull(serverDirectory);
		assertTrue(serverDirectory.exists());

		assertTrue(lockFile.exists());
		raf = new RandomAccessFile(lockFile, "rw");
		String dirName = raf.readUTF();
		assertEquals(dirName, serverDirectory.getName());
	}

	@Test
	public void testCreateRemovesOldDirs() throws IOException {
		File oldDir1 = createDirWithFiles(dir);
		File oldDir2 = createDirWithFiles(dir);
		File oldDir3 = createDirWithFiles(dir);

		File lockFile = new File(dir, ServerDir.SERVER_LOCK_FILE);
		raf = new RandomAccessFile(lockFile, "rw");
		raf.writeUTF(oldDir3.getName());

		serverDir = ServerDir.create(dir);
		assertNotNull(serverDir);

		File serverDirectory = serverDir.getDirectory();
		assertNotNull(serverDirectory);
		assertTrue(serverDirectory.exists());

		assertFalse(oldDir1.exists());
		assertFalse(oldDir2.exists());
		assertFalse(oldDir3.exists());
	}

	@Test
	public void testCreateFails() throws IOException {
		File serverDirectory = FileUtil.createDirectory(dir);
		File lockFile = new File(dir, ServerDir.SERVER_LOCK_FILE);
		raf = new RandomAccessFile(lockFile, "rw");
		raf.writeUTF(serverDirectory.getName());
		lock = ServerDir.lock(raf);

		try {
			serverDir = ServerDir.create(dir);
			fail("expected ServerActiveException");
		} catch (ServerActiveException e) {
			assertEquals(serverDirectory, e.getActiveServerDirectory());
		}
	}

	@Test(timeout = 10000)
	public void testCreateFailsSecondJVM() throws Exception {
		Process server = Runtime.getRuntime().exec(
				new String[] { TestUtil.getJvm(), "-cp",
						TestUtil.getTestClassPath(),
						TestServer.class.getName(), dir.getAbsolutePath() });
		try {
			while (ServerDir.getActive(dir) == null) {
				Thread.sleep(500);
			}

			serverDir = ServerDir.create(dir);
			fail("expected ServerActiveException");
		} catch (ServerActiveException e) {
			assertEquals(dir, e.getActiveServerDirectory().getParentFile());
		} finally {
			server.destroy();
		}
	}

	@Test
	public void testGetActiveServerDirectory() throws IOException {
		File serverDirectory = FileUtil.createDirectory(dir);
		File lockFile = new File(dir, ServerDir.SERVER_LOCK_FILE);

		assertNull(ServerDir.getActive(dir));

		assertTrue(lockFile.createNewFile());
		assertNull(ServerDir.getActive(dir));

		raf = new RandomAccessFile(lockFile, "rw");
		raf.writeUTF(serverDirectory.getName());
		assertNull(ServerDir.getActive(dir));

		lock = ServerDir.lock(raf);
		assertNotNull(lock);
		assertEquals(serverDirectory, ServerDir.getActive(dir));

		lock.release();
		assertNull(ServerDir.getActive(dir));
	}

	@Test(timeout = 10000)
	public void testGetActiveServerDirectorySecondJVM() throws Exception {
		Process server = Runtime.getRuntime().exec(
				new String[] { TestUtil.getJvm(), "-cp",
						TestUtil.getTestClassPath(),
						TestServer.class.getName(), dir.getAbsolutePath() });
		try {
			while (ServerDir.getActive(dir) == null) {
				Thread.sleep(500);
			}
		} finally {
			server.destroy();
		}
	}

	@Test
	public void testCreateAndGetActiveServerDirectory() throws IOException {
		assertNull(ServerDir.getActive(dir));

		serverDir = ServerDir.create(dir);
		assertEquals(serverDir.getDirectory(),
				ServerDir.getActive(dir));

		serverDir.close();
		assertNull(ServerDir.getActive(dir));
	}

	private File createDirWithFiles(final File parentDir) throws IOException {
		File newDir = FileUtil.createDirectory(parentDir);
		FileUtil.createFile(newDir);
		FileUtil.createFile(newDir);
		return newDir;
	}

}

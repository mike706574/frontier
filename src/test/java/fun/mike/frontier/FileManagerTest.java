package fun.mike.frontier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.*;
import java.util.List;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.WindowsFakeFileSystem;

public class FileManagerTest {
    private static final String USER = "bob";
    private static final String PASSWORD = "password";
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private FakeFtpServer fakeFtpServer;
    private FileManager manager;

    @Before
    public void setUp() {
        fakeFtpServer = new FakeFtpServer();
        fakeFtpServer.setServerControlPort(0);
        fakeFtpServer.addUserAccount(new UserAccount(USER, PASSWORD, "c:\\home"));

        FileSystem fileSystem = new WindowsFakeFileSystem();
        fileSystem.add(new DirectoryEntry("c:\\home"));
        fileSystem.add(new FileEntry("c:\\home\\test\\foo.txt", "foo."));
        fileSystem.add(new FileEntry("c:\\home\\test\\bar.txt", "bar!!"));
        fakeFtpServer.setFileSystem(fileSystem);

        fakeFtpServer.start();

        manager = new FileManager("localhost",
                fakeFtpServer.getServerControlPort(),
                USER,
                PASSWORD);
    }

    @After
    public void tearDown() {
        fakeFtpServer.stop();
    }

    @Test
    public void stream() {
        OutputStream os = new ByteArrayOutputStream();
        assertEquals("foo.", IO.slurp(manager.stream("test/foo.txt").get()));
    }

    @Test
    public void streamFailure() {
        assertFalse(manager.stream("elkawrjwa").isPresent());
    }

    @Test
    public void list() throws Exception {
        List<FileInfo> files = manager.list("test");

        assertEquals(2, files.size());

        FileInfo foo = files.get(0);
        assertEquals("foo.txt", foo.getName());
        assertEquals(new Long(4), foo.getSize());

        FileInfo bar = files.get(1);
        assertEquals("bar.txt", bar.getName());
        assertEquals(new Long(5), bar.getSize());
    }

    @Test
    public void download() {
        OutputStream out = new ByteArrayOutputStream();
        manager.download("test/foo.txt",
                out);
        assertEquals("foo.", out.toString());
    }

    @Test
    public void downloadNonexistentFile() {
        OutputStream os = new ByteArrayOutputStream();
        assertEquals(Optional.empty(), manager.download("foo", os));
    }

    @Test
    public void dirExists() {
        assertTrue(manager.dirExists("test"));
        assertFalse(manager.dirExists("fake"));
    }

    @Test
    public void downloadFailure() {
        thrown.expect(FileManagerException.class);
        OutputStream os = new ByteArrayOutputStream();
        new FileManager("eakw", "foo", "bar").download("foo", os);
    }

    @Test
    public void downloadToFile() {
        final String FTP_PATH = "test/foo.txt";
        final String LOCAL_PATH = "local/foo.txt";
        try {
            OutputStream out = new ByteArrayOutputStream();
            assertTrue(manager.download(FTP_PATH, LOCAL_PATH));
            assertEquals("foo.", IO.slurp(LOCAL_PATH));
        } finally {
            IO.deleteQuietly(LOCAL_PATH);
        }
    }

    @Test
    public void upload() {
        final String PATH = "test/baz.txt";
        final String CONTENT = "baz.";

        InputStream in = new ByteArrayInputStream(CONTENT.getBytes());
        assertEquals(Optional.of(PATH), manager.upload(PATH, in));
        assertEquals(CONTENT, manager.slurp(PATH));
    }
}

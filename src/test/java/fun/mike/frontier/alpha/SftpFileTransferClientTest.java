package fun.mike.frontier.alpha;

import java.io.IOException;

import com.github.stefanbirkner.fakesftpserver.rule.FakeSftpServerRule;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class SftpFileTransferClientTest {
    private int PORT = 8080;

    @Rule
    public final FakeSftpServerRule server = new FakeSftpServerRule().setPort(PORT);
    private final String LOCAL_FILE = "local/foo.txt";
    private final SftpFileTransferClient client = new SftpFileTransferClient("localhost",
                                                                             PORT,
                                                                             "foo",
                                                                             "baz");

    @After
    public void tearDown() {
        IO.deleteQuietly(LOCAL_FILE);
    }

    @Test
    public void uploadLocalFile() throws IOException {
        final String PATH = "test/baz.txt";
        final String CONTENT = "baz.";

        String content = "this is a test";
        IO.spit(LOCAL_FILE, content);
        client.upload(LOCAL_FILE, "newTest", "/");

        String serverContent = server.getFileContent("/newTest", UTF_8);

        assertTrue("File wasn't found at the root.", server.existsFile("/newTest"));
        assertEquals(content, serverContent);
    }

    @Test
    public void downloadRemoteFile() throws IOException {
        String content = "this is a test";
        server.putFile("/bar", content, UTF_8);

        client.download("/bar", "local");

        assertEquals(content, IO.slurp("local/bar"));
        IO.nuke("local/bar");
    }

}

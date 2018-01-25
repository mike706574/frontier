package fun.mike.frontier.alpha;

import com.github.stefanbirkner.fakesftpserver.rule.FakeSftpServerRule;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.UncheckedIOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SftpFileTransferClientTest  {
    private int PORT = 8080;

    @Rule
    public final FakeSftpServerRule server = new FakeSftpServerRule().setPort(PORT);
    private final String LOCAL_FILE = "local/foo.txt";

    public FileTransferClient client() {
        return new SftpFileTransferClient("localhost",
                                          PORT,
                                          "foo",
                                          "baz");
    }
    
   @Test
    public void uploadLocalFile() {
        final String PATH = "test/baz.txt";
        final String CONTENT = "baz.";

        String content = "this is a test";
        IO.spit(LOCAL_FILE, content);
        client().upload(LOCAL_FILE, "/newTest");

        String serverContent = getFileContent("/newTest");

        assertTrue("File wasn't found at the root.", server.existsFile("/newTest"));
        assertEquals(content, serverContent);
    }

    @Test
    public void downloadRemoteFile() throws IOException {
        String content = "this is a test";
        server.putFile("/bar", content, UTF_8);

        client().download("/bar", "local/bar");

        assertEquals(content, IO.slurp("local/bar"));
        IO.nuke("local/bar");
    }
    
    @Test
    public void assertFileExists() throws IOException {
        String content = "this is a test";
        server.putFile("/bar", content, UTF_8);
        
        boolean shouldBeTrue = client().fileExists("/bar");
        assertEquals(true,shouldBeTrue);
        
        boolean shouldBeFalse = client().fileExists("baboo");
        assertEquals(false,shouldBeFalse);
        
    }

    private String getFileContent(String path) {
        try {
            return server.getFileContent(path, UTF_8);
        }
        catch(IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}

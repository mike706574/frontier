package fun.mike.frontier.alpha;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface FileTransferClient {
    Optional<InputStream> optionalStream(String path);

    InputStream stream(String path);

    Boolean dirExists(String path);

    Boolean fileExists(String path);

    Optional<String> optionalSlurp(String path);

    String slurp(String path);

    List<FileInfo> list(String path);

    Boolean optionalDownload(String path, String localPath);

    Optional<OutputStream> optionalDownload(String path, OutputStream stream);

    void download(String path, String localPath);

    OutputStream download(String path, OutputStream stream);

    Map<String, Boolean> downloadAll(Map<String, OutputStream> targets);

    String upload(String source, String dest);

    String upload(InputStream is, String path);

    void delete(String path);
}
package fun.mike.frontier.alpha;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface FileTransferClient {
    Optional<InputStream> optionalStream(String path) throws FileTransferException;

    InputStream stream(String path) throws FileTransferException, MissingFileException;

    Boolean dirExists(String path) throws FileTransferException;

    Boolean fileExists(String path) throws FileTransferException;

    Optional<String> optionalSlurp(String path) throws FileTransferException, MissingFileException;

    String slurp(String path) throws FileTransferException, MissingFileException;

    List<FileInfo> list(String path) throws FileTransferException;

    Boolean optionalDownload(String path, String localPath) throws FileTransferException;

    Optional<OutputStream> optionalDownload(String path, OutputStream stream) throws FileTransferException;

    void download(String path, String localPath) throws FileTransferException, MissingFileException;

    OutputStream download(String path, OutputStream stream) throws FileTransferException, MissingFileException;

    Map<String, Boolean> downloadAll(Map<String, OutputStream> targets) throws FileTransferException;

    String upload(String source, String dest) throws FileTransferException;

    String upload(InputStream is, String path) throws FileTransferException;
}
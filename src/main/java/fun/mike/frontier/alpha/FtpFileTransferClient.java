package fun.mike.frontier.alpha;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import fun.mike.frontier.impl.alpha.ApacheFtp;
import fun.mike.frontier.impl.alpha.FtpConnector;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FtpFileTransferClient implements FileTransferClient {
    private static final Logger log = LoggerFactory.getLogger(FtpFileTransferClient.class);

    private final String host;
    private final Integer port;
    private final String username;
    private final String password;

    /**
     * Creates a FtpFileTransferClient using port 21.
     *
     * @param host     a host
     * @param username a username
     * @param password a password
     */
    public FtpFileTransferClient(String host,
            String username,
            String password) {
        this(host, 21, username, password);
    }

    /**
     * Creates a FtpFileTransferClient.
     *
     * @param host     a host
     * @param port     a port
     * @param username a username
     * @param password a password
     */
    public FtpFileTransferClient(String host,
            Integer port,
            String username,
            String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    /**
     * Streams a file over FTP.
     *
     * @param path the path to a file on the host.
     * @return an Optional containing an InputStream to the file at path if it exists; otherwise, an empty Optional.
     */
    public Optional<InputStream> optionalStream(String path) {
        return withConnector(conn -> ApacheFtp.optionalStream(conn, path));
    }

    /**
     * Streams a file over FTP.
     *
     * @param path the path to a file on the host.
     * @return an InputStream to the file at path
     */
    public InputStream stream(String path) {
        return withConnector(conn -> ApacheFtp.stream(conn, path));
    }

    /**
     * Checks if a directory exists on the host.
     *
     * @param path a path to the directory on the host.
     * @return true if the directory at path exists; otherwise, false.
     */
    public Boolean dirExists(String path) {
        return withConnector(conn -> ApacheFtp.dirExists(conn, path));
    }

    /**
     * Checks if a file exists on the host.
     *
     * @param path a path to the file on the host.
     * @return true if the file at path exists; otherwise, false.
     */
    @Override
    public Boolean fileExists(String path) {
        return withConnector(conn -> ApacheFtp.fileExists(conn, path));
    }

    /**
     * Reads the contents of a file on the host to a string.
     *
     * @param path a path to a file on the host.
     * @return an Optional containing the contents of the file if it exists; otherwise, an empty Optional.
     */
    public Optional<String> optionalSlurp(String path) {
        return withConnector(conn -> ApacheFtp.optionalSlurp(conn, path));
    }

    /**
     * Reads the contents of a file on the host to a string.
     *
     * @param path a path to a file on the host.
     * @return the contents of the file at path as a string.
     */
    public String slurp(String path) {
        return withConnector(conn -> ApacheFtp.slurp(conn, path));
    }

    /**
     * Lists the files in a directory on the host.
     *
     * @param path a path to a directory on the host.
     * @return a list of files.
     */
    public List<FileInfo> list(String path) {
        return withConnector(conn -> ApacheFtp.list(conn, path));
    }

    /**
     * Downloads a file from the host to the local machine.
     *
     * @param path      a path to a file on the host.
     * @param localPath a path on the local machine.
     * @return true if the file exists and was downloaded; otherwise, false.
     */
    public Boolean optionalDownload(String path, String localPath) {
        return withConnector(conn -> ApacheFtp.optionalDownload(conn, path, localPath));
    }

    /**
     * Writes the contents of a file on the host to an output stream.
     *
     * @param path   a path to a file on the host.
     * @param stream An OutputStream to write to.
     * @return An Optional containing the OutputStream if the file exists; otherwise, an empty Optional.
     */
    public Optional<OutputStream> optionalDownload(String path, OutputStream stream) {
        return withConnector(conn -> ApacheFtp.optionalDownload(conn, path, stream));
    }

    /**
     * Writes the contents of a file on the host to a local file.
     *
     * @param path      a path to a file on the host.
     * @param localPath a local path to a file to be written to.
     */
    public void download(String path, String localPath) {
        useConnector(conn -> ApacheFtp.download(conn, path, localPath));
    }

    /**
     * Writes the contents of a file on the host to an output stream.
     *
     * @param path   a path to a file on the host.
     * @param stream An OutputStream to write to.
     * @return An Optional containing the OutputStream if the file exists; otherwise, an empty Optional.
     */
    public OutputStream download(String path, OutputStream stream) {
        return withConnector(conn -> ApacheFtp.download(conn, path, stream));
    }

    /**
     * Writes the contents of a group of files to their respective output streams.
     *
     * @param targets a Map of paths to their respective OutputStream.
     * @return An Map of paths to a Boolean indicating if the respective file was found and written to their respective
     * OutputStream.
     */
    public Map<String, Boolean> downloadAll(Map<String, OutputStream> targets) {
        return withConnector(conn -> ApacheFtp.downloadAll(conn, targets));
    }

    /**
     * Uploads the contents of the file at path to the given path on the host.
     *
     * @param source a path of a file
     * @param dest   a path to write to on the host
     * @return the path written to
     */
    public String upload(String source, String dest) {
        return withConnector(conn -> ApacheFtp.upload(conn, source, dest));
    }

    /**
     * Uploads the contents from an input stream to a path on the host.
     *
     * @param path a path to write to on the host
     * @param is   an InputStream containing the content to be written.
     * @return the path written to
     */
    public String upload(InputStream is, String path) {
        return withConnector(conn -> ApacheFtp.upload(conn, is, path));
    }

    /**
     * Deletes the file at path on the host.
     *
     * @param path a path on the host
     */
    @Override
    public void delete(String path) {
        useConnector(conn -> ApacheFtp.delete(conn, path));
    }

    /**
     * Logs out and disconnects the given client from the host.
     *
     * @param client An FTPClient instance.
     */
    private void disconnect(FTPClient client) {
        if (client == null) {
            return;
        }

        String hostLabel = getHostLabel();

        log.debug(String.format("Disconnecting from %s.",
                                hostLabel));

        try {
            client.logout();
        } catch (IOException ex) {
            String message = String.format("Failed to logout of host %s.", hostLabel);
            log.warn(message);
            throw new FileTransferException(message, ex);
        }

        if (client.isConnected()) {
            try {
                client.disconnect();
            } catch (IOException ex) {
                String message = String.format("Failed to disconnect from host %s.",
                                               hostLabel);
                log.warn(message);
                throw new FileTransferException(message, ex);
            }
        }
    }

    /**
     * Logs in and connects to the host, returning the created FTPClient instance.
     *
     * @return An FTPClient instance.
     */
    private FTPClient connect() {
        String hostLabel = getHostLabel();
        try {
            log.debug(String.format("Connecting to %s as %s.", hostLabel, username));
            FTPClient client = new FTPClient();
            client.connect(host, port);
            client.login(username, password);
            if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
                client.disconnect();
                String message = String.format("%s refused connection.", host);
                log.warn(message);
                throw new FileTransferException(message);
            }
            return client;
        } catch (IOException ex) {
            String message = String.format("Failed to connect to %s.",
                                           hostLabel);
            log.warn(message);
            throw new FileTransferException(message, ex);
        }
    }

    private <T> T withConnector(Function<FtpConnector, T> function) {
        FTPClient client = null;
        try {
            client = connect();
            FtpConnector connector = new FtpConnector(client, host, port);
            return function.apply(connector);
        } finally {
            disconnect(client);
        }
    }

    private void useConnector(Consumer<FtpConnector> consumer) {
        FTPClient client = null;
        try {
            client = connect();
            FtpConnector connector = new FtpConnector(client, host, port);
            consumer.accept(connector);
        } finally {
            disconnect(client);
        }
    }

    private String getHostLabel() {
        if (port == 21) {
            return host;
        }
        return String.format("%s:%d", host, port);
    }

}

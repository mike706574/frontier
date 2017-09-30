package fun.mike.frontier;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileTransferClient {
    private static final Logger log = LoggerFactory.getLogger(FileTransferClient.class);

    private final String host;
    private final Integer port;
    private final String username;
    private final String password;

    /**
     * Creates a FileTransferClient using port 21.
     *
     * @param host     a host
     * @param username a username
     * @param password a password
     */

    public FileTransferClient(String host,
                              String username,
                              String password) {
        this(host, 21, username, password);
    }

    /**
     * Creates a FileTransferClient.
     *
     * @param host     a host
     * @param port     a port
     * @param username a username
     * @param password a password
     */
    public FileTransferClient(String host,
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
    public Optional<InputStream> stream(String path) {
        FTPClient client = null;
        try {
            client = connect();
            return stream(client, path);
        } finally {
            disconnect(client);
        }
    }

    /**
     * Streams a file over FTP using the given client.
     *
     * @param client an FTPClient instance.
     * @param path   the path to a file on the host.
     * @return an Optional containing an InputStream to the file at path if it exists; otherwise, an empty Optional.
     */
    public Optional<InputStream> stream(FTPClient client, String path) {
        String locationLabel = getLocationLabel(path);
        try {
            log.debug(String.format("Streaming file %s.",
                    locationLabel));
            InputStream is = client.retrieveFileStream(path);
            if (is == null) {
                if (client.getReplyCode() == 550) {
                    log.debug(String.format("Failed to find file %s.", locationLabel));
                    return Optional.empty();
                }
                String message = String.format("Unexpected reply: %s.",
                        client.getReplyString());
                log.warn(message);
                throw new FileTransferException(message);
            }
            return Optional.of(is);
        } catch (IOException ex) {
            String message = String.format("I/O error streaming file %s.",
                    locationLabel);
            log.warn(message);
            throw new FileTransferException(message, ex);
        }
    }

    /**
     * Checks if a file exists on the host.
     *
     * @param path a path to the file on the host.
     * @return true if the directory at path exists; otherwise, false.
     */
    public Boolean dirExists(String path) {
        FTPClient client = null;
        try {
            client = connect();
            return dirExists(client, path);
        } finally {
            disconnect(client);
        }
    }

    /**
     * Checks if a file exists on the host using the given client.
     *
     * @param client an FTPClient instance.
     * @param path   a path to the file on the host.
     * @return true if the directory at path exists; otherwise, false.
     */
    public Boolean dirExists(FTPClient client, String path) {
        String locationLabel = getLocationLabel(path);
        try {
            log.debug(String.format("Checking if directory %s exists.",
                    locationLabel));
            String workingDir = client.printWorkingDirectory();
            client.changeWorkingDirectory(path);

            boolean found = false;
            int replyCode = client.getReplyCode();
            if (replyCode == 250) {
                log.debug(String.format("Found directory %s.", locationLabel));
                found = true;
            } else if (replyCode == 550) {
                log.debug(String.format("Failed to find directory %s.",
                        locationLabel));
                found = false;
            } else {
                String message = String.format("Unexpected reply from changing working directory while checking for existence of %s: %s",
                        locationLabel,
                        client.getReplyString());
                log.warn(message);
                throw new FileTransferException(message);
            }

            client.changeWorkingDirectory(workingDir);
            replyCode = client.getReplyCode();
            if (replyCode != 250) {
                String message = String.format("Unexpected reply from changing working directory back to %s while checking for existence of %s: %s",
                        locationLabel,
                        workingDir,
                        client.getReplyString());
                log.warn(message);
                throw new FileTransferException(message);
            }

            return found;
        } catch (IOException ex) {
            String message = String.format("I/O error checking if directory %s exists.",
                    locationLabel);
            log.warn(message);
            throw new FileTransferException(message, ex);
        }
    }

    /**
     * Reads the contents of a file on the host to a string.
     *
     * @param path a path to a file on the host.
     * @return the contents of the file at path as a string.
     */
    public String slurp(String path) {
        return IO.slurp(stream(path).orElseThrow(() -> new FileNotFoundException(path)));
    }

    /**
     * Reads the contents of a file on the host to a string using the given client.
     *
     * @param client an FTPClient instance.
     * @param path   a path to a file on the host.
     * @return the contents of the file at path as a string.
     */
    public String slurp(FTPClient client, String path) {
        return IO.slurp(stream(client, path).orElseThrow(() -> new FileNotFoundException(path)));
    }

    /**
     * Lists the files in a directory on the host.
     *
     * @param path a path to a directory on the host.
     * @return a list of files.
     */
    public List<FileInfo> list(String path) {
        FTPClient client = null;
        try {
            client = connect();
            return list(client, path);
        } finally {
            disconnect(client);
        }
    }

    /**
     * Lists the files in a directory on the host.
     *
     * @param client an FTPClient instance.
     * @param path   a path to a directory on the host.
     * @return a list of files.
     */
    public List<FileInfo> list(FTPClient client, String path) {
        String locationLabel = getLocationLabel(path);
        try {
            log.debug(String.format("Listing files in %s.",
                    locationLabel));
            if (!dirExists(client, path)) {
                throw new FileTransferException(String.format("Directory %s does not exist.",
                        locationLabel));
            }

            List<FileInfo> files = Arrays.stream(client.listFiles(path))
                    .map(file -> {
                        Calendar timestamp = file.getTimestamp();
                        LocalDateTime time = LocalDateTime.ofInstant(timestamp.toInstant(),
                                ZoneId.systemDefault());
                        return new FileInfo(file.getName(),
                                file.getSize(),
                                time);
                    })
                    .collect(Collectors.toList());
            log.debug(String.format("Found %d files.", files.size()));
            return files;
        } catch (IOException ex) {
            String message = String.format("I/O error listing directory.");
            log.warn(message);
            throw new FileTransferException(message, ex);
        }
    }

    /**
     * Downloads a file from the host to the local machine.
     *
     * @param path      a path to a file on the host.
     * @param localPath a path on the local machine.
     * @return true if the file exists and was downloaded; otherwise, false.
     */
    public Boolean download(String path, String localPath) {
        String locationLabel = getLocationLabel(path);
        log.debug(String.format("Downloading file %s locally to %s.",
                path,
                localPath));
        try (OutputStream os = new FileOutputStream(localPath)) {
            return download(path, os).isPresent();
        } catch (IOException ex) {
            String message = String.format("I/O error downloading %s.",
                    path);
            log.warn(message);
            throw new FileTransferException(message, ex);
        }
    }

    /**
     * Writes the contents of a file on the host to an output stream.
     *
     * @param path   a path to a file on the host.
     * @param stream An OutputStream to write to.
     * @return An Optional containing the OutputStream if the file exists; otherwise, an empty Optional.
     */
    public Optional<OutputStream> download(String path, OutputStream stream) {
        FTPClient client = null;
        try {
            client = connect();
            return download(client, path, stream);
        } finally {
            disconnect(client);
        }
    }

    /**
     * Writes the contents of a file on the host to an output stream using the given client.
     *
     * @param client an FTPClient instance.
     * @param path   a path to a file on the host.
     * @param stream An OutputStream to write to.
     * @return An Optional containing the OutputStream if the file exists; otherwise, an empty Optional.
     */
    public Optional<OutputStream> download(FTPClient client, String path, OutputStream stream) {
        return retrieveFile(client, path, stream)
                .map(replyString -> stream);
    }

    /**
     * Writes the contents of a group of files to their respective output streams.
     *
     * @param targets a Map of paths to their respective OutputStream.
     * @return An Map of paths to a Boolean indicating if the respective file was found and written to their respective
     * OutputStream.
     */
    public Map<String, Boolean> downloadAll(Map<String, OutputStream> targets) {
        FTPClient client = null;
        try {
            client = connect();
            return downloadAll(client, targets);
        } finally {
            disconnect(client);
        }
    }

    /**
     * Writes the contents of a group of files to their respective output streams.
     *
     * @param client  An FTPClient instance.
     * @param targets a Map of paths to their respective OutputStream.
     * @return An Map of paths to a Boolean indicating if the respective file was found and written to their respective
     * OutputStream.
     */
    public Map<String, Boolean> downloadAll(FTPClient client, Map<String, OutputStream> targets) {
        Map<String, Boolean> results = new HashMap<>();

        for (Map.Entry<String, OutputStream> target : targets.entrySet()) {
            String path = target.getKey();
            OutputStream stream = target.getValue();
            Boolean successful = retrieveFile(client, path, stream)
                    .map(replyString -> true)
                    .orElse(false);
            results.put(path, successful);
        }
        return results;
    }

    /**
     * Uploads the contents from an input stream to a path on the host.
     *
     * @param path a path to write to on the host
     * @param is   an InputStream containing the content to be written.
     * @return an Optional containing the path written to if the directory of the path exists; otherwise, an empty
     * Optional.
     */
    public Optional<String> upload(String path, InputStream is) {
        FTPClient client = null;
        try {
            client = connect();
            return upload(client, path, is);
        } finally {
            disconnect(client);
        }
    }

    /**
     * Uploads the contents from an input stream to a path on the host.
     *
     * @param client an FTPClient instance.
     * @param path   a path to write to on the host
     * @param is     an InputStream containing the content to be written.
     * @return an Optional containing the path written to if the directory of the path exists; otherwise, an empty
     * Optional.
     */
    public Optional<String> upload(FTPClient client, String path, InputStream is) {
        try {
            boolean successful = client.storeFile(path, is);
            if (successful) {
                return Optional.of(path);
            }

            if (client.getReplyCode() == 550) {
                return Optional.empty();
            }

            String message = String.format("Unexpected reply: %s.",
                    client.getReplyString());
            log.warn(message);
            throw new FileTransferException(message);
        } catch (IOException ex) {
            throw new FileTransferException(ex);
        }
    }

    private Optional<String> retrieveFile(FTPClient client,
                                          String path,
                                          OutputStream stream) {
        String locationLabel = getLocationLabel(path);
        log.debug(String.format("Retrieving file %s.", locationLabel));
        boolean successful;
        try {
            successful = client.retrieveFile(path, stream);

            if (!successful) {
                log.debug("Retrieving file %s.", locationLabel);
                if (client.getReplyCode() == 550) {
                    return Optional.empty();
                }

                throw new FileTransferException(client.getReplyString());
            }
            return Optional.of(client.getReplyString());
        } catch (IOException ex) {
            throw new FileTransferException(ex);
        }
    }

    /**
     * Logs out and disconnects the given client from the host.
     *
     * @param client An FTPClient instance.
     */
    public void disconnect(FTPClient client) {
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
    public FTPClient connect() {
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

    private String getLocationLabel(String path) {
        return String.format("%s:%s", getHostLabel(), path);
    }

    private String getHostLabel() {
        if (port == 21) {
            return host;
        }
        return String.format("%s:%d", host, port);
    }
}

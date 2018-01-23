package fun.mike.frontier.impl.alpha;

import java.io.FileInputStream;
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

import fun.mike.frontier.alpha.FileInfo;
import fun.mike.frontier.alpha.MissingFileException;
import fun.mike.frontier.alpha.FileTransferException;
import fun.mike.frontier.alpha.IO;
import org.apache.commons.net.ftp.FTPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApacheFtp {
    private static final Logger log = LoggerFactory.getLogger(ApacheFtp.class);

    /**
     * Streams a file over FTP using the given client.
     *
     * @param conn an FtpConnector instance.
     * @param path the path to a file on the host.
     * @return an Optional containing an InputStream to the file at path if it exists; otherwise, an empty Optional.
     */
    public static Optional<InputStream> optionalStream(FtpConnector conn, String path) throws FileTransferException {
        FTPClient client = conn.getClient();
        String locationLabel = getLocationLabel(conn, path);
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
     * Streams a file over FTP using the given client.
     *
     * @param conn an FtpConnector instance.
     * @param path the path to a file on the host.
     * @return an InputStream to the file at path
     */
    public static InputStream stream(FtpConnector conn, String path) throws FileTransferException, MissingFileException {
        return optionalStream(conn, path).orElseThrow(() -> fileNotFound(conn, path));
    }

    /**
     * Checks if a file exists on the host using the given client.
     *
     * @param conn a FtpConnector instance.
     * @param path a path to the file on the host.
     * @return true if the directory at path exists; otherwise, false.
     */
    public static Boolean dirExists(FtpConnector conn, String path) throws FileTransferException {
        String locationLabel = getLocationLabel(conn, path);
        FTPClient client = conn.getClient();
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
     * Reads the contents of a file on the host to a string using the given client.
     *
     * @param conn an FtpConnector instance.
     * @param path a path to a file on the host.
     * @return an Optional containing the contents of the file at path as a string; otherwise an empty Optional.
     */
    public static Optional<String> optionalSlurp(FtpConnector conn, String path) throws FileTransferException, MissingFileException {
        Optional<InputStream> is = optionalStream(conn, path);
        if (is.isPresent()) {
            return Optional.of(IO.slurp(is.get()));

        }
        return Optional.empty();
    }


    /**
     * Reads the contents of a file on the host to a string using the given client.
     *
     * @param conn an FtpConnector instance.
     * @param path a path to a file on the host.
     * @return the contents of the file at path as a string.
     */
    public static String slurp(FtpConnector conn, String path) throws FileTransferException, MissingFileException {
        return optionalSlurp(conn, path).orElseThrow(() -> fileNotFound(conn, path));
    }

    /**
     * Lists the files in a directory on the host.
     *
     * @param conn an FTPClient instance.
     * @param path a path to a directory on the host.
     * @return a list of files.
     */
    public static List<FileInfo> list(FtpConnector conn, String path) throws FileTransferException {
        String locationLabel = getLocationLabel(conn, path);
        try {
            FTPClient client = conn.getClient();
            log.debug(String.format("Listing files in %s.",
                                    locationLabel));
            if (!dirExists(conn, path)) {
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
     * @param conn      an FtpConnector instance.
     * @param path      a path to a file on the host.
     * @param localPath a path on the local machine.
     * @return true if the file exists and was downloaded; otherwise, false.
     */
    public static Boolean optionalDownload(FtpConnector conn, String path, String localPath) throws FileTransferException {
        String locationLabel = getLocationLabel(conn, path);
        log.debug(String.format("Downloading file %s locally to %s.",
                                path,
                                localPath));
        try (OutputStream os = new FileOutputStream(localPath)) {
            return optionalDownload(conn, path, os).isPresent();
        } catch (IOException ex) {
            String message = String.format("I/O error downloading %s.",
                                           path);
            log.warn(message);
            throw new FileTransferException(message, ex);
        }
    }

    /**
     * Writes the contents of a file on the host to an output stream using the given client.
     *
     * @param conn   an FtpConnector instance.
     * @param path   a path to a file on the host.
     * @param stream An OutputStream to write to.
     * @return An Optional containing the OutputStream if the file exists; otherwise, an empty Optional.
     */
    public static Optional<OutputStream> optionalDownload(FtpConnector conn, String path, OutputStream stream) throws FileTransferException {
        log.debug(String.format("Downloading file %s to stream.", path));

        return retrieveFile(conn, path, stream)
                .map(replyString -> stream);
    }

    /**
     * Writes the contents of a file on the host to a local file.
     *
     * @param conn      an FtpConnector instance.
     * @param path      a path to a file on the host.
     * @param localPath a local path to a file to be written to.
     */
    public static void download(FtpConnector conn, String path, String localPath) throws FileTransferException, MissingFileException {
        String locationLabel = getLocationLabel(conn, path);
        try (OutputStream stream = new FileOutputStream(localPath)) {
            download(conn, path, stream);
        } catch (IOException ex) {
            String message = String.format("Failed to stream local file %s when trying to download %s.",
                                           localPath,
                                           locationLabel);
            log.warn(message);
            throw new FileTransferException(ex);
        }
    }

    /**
     * Writes the contents of a group of files to their respective output streams.
     *
     * @param conn    an Ftpconnector instance.
     * @param targets a Map of paths to their respective OutputStream.
     * @return An Map of paths to a Boolean indicating if the respective file was found and written to their respective
     * OutputStream.
     */
    public static Map<String, Boolean> downloadAll(FtpConnector conn, Map<String, OutputStream> targets) throws FileTransferException {
        Map<String, Boolean> results = new HashMap<>();

        for (Map.Entry<String, OutputStream> target : targets.entrySet()) {
            String path = target.getKey();
            OutputStream stream = target.getValue();
            Boolean successful = retrieveFile(conn, path, stream)
                    .map(replyString -> true)
                    .orElse(false);
            results.put(path, successful);
        }
        return results;
    }

    /**
     * Writes the contents of a file on the host to an output stream.
     *
     * @param conn   an FtpConnector instance.
     * @param path   a path to a file on the host.
     * @param stream An OutputStream to write to.
     * @return An Optional containing the OutputStream if the file exists; otherwise, an empty Optional.
     */
    public static OutputStream download(FtpConnector conn, String path, OutputStream stream) throws FileTransferException, MissingFileException {
        return optionalDownload(conn, path, stream).orElseThrow(() -> fileNotFound(conn, path));
    }

    /**
     * Uploads the contents of the file at path to the given path on the host.
     *
     * @param conn   an FtpConnector instance.
     * @param dest   a path to write to on the host
     * @param source a path of a file
     * @return the path written to
     */
    public static String upload(FtpConnector conn, String source, String dest) throws FileTransferException {
        String locationLabel = getLocationLabel(conn, dest);
        log.debug(String.format("Uploading local file %s to %s.", source, locationLabel));

        try (InputStream is = new FileInputStream(source)) {
            return upload(conn, is, dest);
        } catch (IOException ex) {
            throw new FileTransferException(ex);
        }
    }

    /**
     * Uploads the contents from an input stream to a path on the host.
     *
     * @param conn   an FtpConnector instance.
     * @param source an InputStream containing the content to be written.
     * @param dest   a path to write to on the host
     * @return the path written to
     */
    public static String upload(FtpConnector conn, InputStream source, String dest) throws FileTransferException {
        try {
            FTPClient client = conn.getClient();
            String locationLabel = getLocationLabel(conn, dest);
            log.debug(String.format("Uploading content to %s.", locationLabel));
            boolean successful = client.storeFile(dest, source);
            if (successful) {
                return dest;
            }

            // TODO: Handle specific FTP codes
            String message = String.format("Unexpected reply: %s.",
                                           client.getReplyString());
            log.warn(message);
            throw new FileTransferException(message);
        } catch (IOException ex) {
            throw new FileTransferException(ex);
        }
    }


    private static Optional<String> retrieveFile(FtpConnector conn,
            String path,
            OutputStream stream) throws FileTransferException {
        FTPClient client = conn.getClient();
        String locationLabel = getLocationLabel(conn, path);
        log.debug(String.format("Retrieving file %s.", locationLabel));
        boolean successful;
        try {
            successful = client.retrieveFile(path, stream);

            if (!successful) {
                if (client.getReplyCode() == 550) {
                    log.debug(String.format("File %s not found.", locationLabel));
                    return Optional.empty();
                }

                throw new FileTransferException(client.getReplyString());
            }
            log.debug(String.format("Found file %s.", locationLabel));
            return Optional.of(client.getReplyString());
        } catch (IOException ex) {
            throw new FileTransferException(ex);
        }
    }

    private static String getLocationLabel(FtpConnector conn, String path) {
        return String.format("%s:%s", getHostLabel(conn), path);
    }

    private static String getHostLabel(FtpConnector conn) {
        if (conn.getPort() == 21) {
            return conn.getHost();
        }
        return String.format("%s:%d", conn.getHost(), conn.getPort());
    }

    private static FileTransferException pathDoesNotExist(FtpConnector conn, String path) {
        String locationLabel = getLocationLabel(conn, path);
        String message = String.format("Path to %s does not exist.", path);
        return new FileTransferException(path);
    }

    private static MissingFileException fileNotFound(FtpConnector conn, String path) {
        String locationLabel = getLocationLabel(conn, path);
        String message = String.format("File %s not found.", path);
        return new MissingFileException(message);
    }
}

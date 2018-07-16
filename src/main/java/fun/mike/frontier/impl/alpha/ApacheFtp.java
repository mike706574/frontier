package fun.mike.frontier.impl.alpha;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import fun.mike.frontier.alpha.FileInfo;
import fun.mike.frontier.alpha.FileTransferException;
import fun.mike.frontier.alpha.IO;
import fun.mike.frontier.alpha.MissingLocalFileException;
import fun.mike.frontier.alpha.MissingRemoteFileException;
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
    public static Optional<InputStream> optionalStream(FtpConnector conn, String path) {
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
    public static InputStream stream(FtpConnector conn, String path) {
        return optionalStream(conn, path).orElseThrow(() -> remoteFileNotFound(conn, path));
    }

    /**
     * Checks if a file exists on the host using the given client.
     *
     * @param conn a FtpConnector instance.
     * @param path a path to the directory on the host.
     * @return true if the directory at path exists; otherwise, false.
     */
    public static Boolean dirExists(FtpConnector conn, String path) {
        String locationLabel = getLocationLabel(conn, path);
        log.debug(String.format("Checking if directory %s exists.",
                                locationLabel));
        FTPClient client = conn.getClient();
        try {
            String workingDir = client.printWorkingDirectory();
            client.changeWorkingDirectory(path);

            boolean found = false;
            int replyCode = client.getReplyCode();

            switch (replyCode) {
                case 250:
                    log.debug(String.format("Found directory %s.", locationLabel));
                    found = true;
                    break;
                case 257:
                    log.debug(String.format("Directory %s is current directory.", locationLabel));
                    return true;
                case 550:
                    log.debug(String.format("Failed to find directory %s.",
                                            locationLabel));
                    found = false;
                    break;
                default:
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
     * Deletes the file at path on the host.
     *
     * @param conn a FtpConnector instance.
     * @param path a path on the host
     */
    public static void delete(FtpConnector conn, String path) {
        String locationLabel = getLocationLabel(conn, path);
        log.debug(String.format("Deleting file %s.", locationLabel));
        FTPClient client = conn.getClient();

        try {
            if (!fileExists(conn, path)) {
                throw remoteFileNotFound(conn, path);
            }

            boolean deleted = client.deleteFile(path);

            if (!deleted) {
                String message = String.format("Failed to delete %s.", locationLabel);
                log.warn(message);
                throw new FileTransferException(message);
            }
        } catch (IOException ex) {
            String message = String.format("I/O error deleting %s.", locationLabel);
            log.warn(message);
            throw new FileTransferException(message, ex);
        }
    }

    /**
     * Checks if a file exists on the host using the given client.
     *
     * @param conn a FtpConnector instance.
     * @param path a path to the file on the host.
     * @return true if the file at path exists; otherwise, false.
     */
    public static Boolean fileExists(FtpConnector conn, String path) {
        String locationLabel = getLocationLabel(conn, path);
        log.debug(String.format("Checking if file %s exists.",
                                locationLabel));

        try {
            FTPClient client = conn.getClient();
            List<FileInfo> files = Arrays.stream(client.listFiles(path))
                    .map(file -> {
                        Calendar timestamp = file.getTimestamp();

                        return new FileInfo(file.getName(),
                                            file.getSize(),
                                            file.getTimestamp().getTime(),
                                            file.isDirectory());
                    })
                    .collect(Collectors.toList());

            if (files.size() == 0) {
                return false;
            }

            if (files.size() > 1) {
                String message = String.format("%d files found when checking if %s exists.",
                                               files.size(),
                                               path);
                throw new FileTransferException(message);
            }

            FileInfo info = files.get(0);

            if (info.isDirectory()) {
                String message = String.format("%s exists, but is a directory.",
                                               locationLabel);
                throw new FileTransferException(message);
            }

            return true;
        } catch (IOException ex) {
            String message = String.format("I/O error checking if %s exists.",
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
    public static Optional<String> optionalSlurp(FtpConnector conn, String path) {
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
    public static String slurp(FtpConnector conn, String path) {
        return optionalSlurp(conn, path).orElseThrow(() -> remoteFileNotFound(conn, path));
    }

    /**
     * Lists the files in a directory on the host.
     *
     * @param conn an FTPClient instance.
     * @param path a path to a directory on the host.
     * @return a list of files.
     */
    public static List<FileInfo> list(FtpConnector conn, String path) {
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

                        return new FileInfo(file.getName(),
                                            file.getSize(),
                                            file.getTimestamp().getTime(),
                                            file.isDirectory());
                    })
                    .collect(Collectors.toList());
            log.debug(String.format("Found %d files.", files.size()));
            return files;
        } catch (IOException ex) {
            String message = "I/O error listing directory.";
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
    public static Boolean optionalDownload(FtpConnector conn, String path, String localPath) {
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
    public static Optional<OutputStream> optionalDownload(FtpConnector conn, String path, OutputStream stream) {
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
    public static void download(FtpConnector conn, String path, String localPath) {
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
    public static Map<String, Boolean> downloadAll(FtpConnector conn, Map<String, OutputStream> targets) {
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
    public static OutputStream download(FtpConnector conn, String path, OutputStream stream) {
        return optionalDownload(conn, path, stream).orElseThrow(() -> remoteFileNotFound(conn, path));
    }

    /**
     * Uploads the contents of the file at path to the given path on the host.
     *
     * @param conn   an FtpConnector instance.
     * @param dest   a path to write to on the host
     * @param source a path of a file
     * @return the path written to
     */
    public static String upload(FtpConnector conn, String source, String dest) {
        String locationLabel = getLocationLabel(conn, dest);
        log.debug(String.format("Uploading local file %s to %s.", source, locationLabel));

        if (!IO.exists(source)) {
            String message = String.format("Local source file %s does not exist.", source);
            throw new MissingLocalFileException(message);
        }
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
    public static String upload(FtpConnector conn, InputStream source, String dest) {
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
            OutputStream stream) {
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

    private static MissingRemoteFileException remoteFileNotFound(FtpConnector conn, String path) {
        String locationLabel = getLocationLabel(conn, path);
        String message = String.format("Remote file %s not found.", locationLabel);
        return new MissingRemoteFileException(message);
    }
}

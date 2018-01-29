package fun.mike.frontier.impl.alpha;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import fun.mike.frontier.alpha.FileTransferException;
import fun.mike.frontier.alpha.MissingRemoteFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JschSftp {
    private static final Logger log = LoggerFactory.getLogger(JschSftp.class);

    public static Boolean fileExists(SftpConnector conn, String path) {
        String locationLabel = getLocationLabel(conn, path);
        log.debug(String.format("Checking if file %s exists.",
                                locationLabel));

        try {
            ChannelSftp chan = conn.getChannel();
            SftpATTRS attrs = chan.lstat(path);

            if (attrs.isDir()) {
                String message = String.format("%s exists, but is a directory.",
                                               locationLabel);
                throw new FileTransferException(message);
            }

            return true;
        } catch (SftpException e) {
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                return false;
            } else {
                String message = String.format("Error asserting if file %s exists.", locationLabel);
                throw new FileTransferException(message);
            }
        }
    }

    private static String getLocationLabel(SftpConnector conn, String path) {
        return String.format("%s:%s", getHostLabel(conn), path);
    }

    private static String getHostLabel(SftpConnector conn) {
        if (conn.getPort() == 21) {
            return conn.getHost();
        }
        return String.format("%s:%d", conn.getHost(), conn.getPort());
    }

    private static MissingRemoteFileException remoteFileNotFound(SftpConnector conn, String path) {
        String locationLabel = getLocationLabel(conn, path);
        String message = String.format("File %s not found.", path);
        return new MissingRemoteFileException(message);
    }
}

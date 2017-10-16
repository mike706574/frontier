package fun.mike.frontier.alpha;

/**
 * Signals that an error has occurred during execution of a file transfer operation.
 */
public class FileTransferException extends RuntimeException {
    public FileTransferException(String msg) {
        super(msg);
    }

    public FileTransferException(Throwable t) {
        super(t);
    }

    public FileTransferException(String msg, Throwable t) {
        super(msg, t);
    }
}

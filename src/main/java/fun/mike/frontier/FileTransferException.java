package fun.mike.frontier;

/**
 * Signals that an error has occurred during execution of a file transfer operation.
 */
public class FileTransferException extends Exception {
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

package fun.mike.frontier.alpha;

/**
 * Signals that a file necessary for the completion of an operation was not found.
 */
public class MissingFileException extends RuntimeException {
    public MissingFileException(String msg) {
        super(msg);
    }

    public MissingFileException(Throwable t) {
        super(t);
    }

    public MissingFileException(String msg, Throwable t) {
        super(msg, t);
    }
}

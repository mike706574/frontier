package fun.mike.frontier;

/**
 * Signals that a file necessary for the completion of an operation was not found.
 */
public class FileNotFoundException extends Exception {
    public FileNotFoundException(String msg) {
        super(msg);
    }

    public FileNotFoundException(Throwable t) {
        super(t);
    }

    public FileNotFoundException(String msg, Throwable t) {
        super(msg, t);
    }
}

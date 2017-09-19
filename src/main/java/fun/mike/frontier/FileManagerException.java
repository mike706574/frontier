package fun.mike.frontier;

public class FileManagerException extends RuntimeException {
    public FileManagerException(String msg) {
        super(msg);
    }

    public FileManagerException(Throwable t) {
        super(t);
    }

    public FileManagerException(String msg, Throwable t) {
        super(msg, t);
    }
}

package fun.mike.frontier;

public class FileRetrieverException extends RuntimeException {
    public FileRetrieverException(String msg) {
        super(msg);
    }

    public FileRetrieverException(Throwable t) {
        super(t);
    }

    public FileRetrieverException(String msg, Throwable t) {
        super(msg, t);
    }
}

package fun.mike.frontier.alpha;

import org.apache.commons.net.ftp.FTPClient;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface IFileTransferClient{
    
    
    Optional<InputStream> optionalStream(String path) throws FileTransferException ;
    public InputStream stream(String path) throws FileTransferException, FileNotFoundException ;
    public Optional<InputStream> optionalStream(FTPClient client, String path) throws FileTransferException ;
    public InputStream stream(FTPClient client, String path) throws FileTransferException, FileNotFoundException ;
    public Boolean dirExists(String path) throws FileTransferException ;
    public Boolean dirExists(FTPClient client, String path) throws FileTransferException ;
    public Optional<String> optionalSlurp(String path) throws FileTransferException, FileNotFoundException ;
    public String slurp(String path) throws FileTransferException, FileNotFoundException ;
    public Optional<String> optionalSlurp(FTPClient client, String path) throws FileTransferException, FileNotFoundException ;
    public String slurp(FTPClient client, String path) throws FileTransferException, FileNotFoundException ;
    public List<FileInfo> list(String path) throws FileTransferException ;
    public List<FileInfo> list(FTPClient client, String path) throws FileTransferException ;
    public Boolean optionalDownload(String path, String localPath) throws FileTransferException ;
    public Optional<OutputStream> optionalDownload(String path, OutputStream stream) throws FileTransferException ;
    public void download(String path, String localPath) throws FileTransferException, FileNotFoundException ;
    public OutputStream download(String path, OutputStream stream) throws FileTransferException, FileNotFoundException ;
    public Optional<OutputStream> optionalDownload(FTPClient client, String path, OutputStream stream) throws FileTransferException ;
    public OutputStream download(FTPClient client, String path, OutputStream stream) throws FileTransferException, FileNotFoundException ;
    public Map<String, Boolean> downloadAll(Map<String, OutputStream> targets) throws FileTransferException ;
    public Map<String, Boolean> downloadAll(FTPClient client, Map<String, OutputStream> targets) throws FileTransferException ;
    public String upload(String source, String dest) throws FileTransferException ;
    public String upload(FTPClient client, String source, String dest) throws FileTransferException ;
    public String upload(FTPClient client, InputStream source, String dest) throws FileTransferException ;
    public String upload(InputStream is, String path) throws FileTransferException ;
    public void disconnect(FTPClient client) throws FileTransferException ;
    public FTPClient connect() throws FileTransferException;
    
}
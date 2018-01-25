package fun.mike.frontier.alpha;

import java.util.Date;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FileInfo {
    private final String name;
    private final Long size;
    private final Date time;
    private final Boolean isDirectory;

    @JsonCreator
    public FileInfo(@JsonProperty("name") String name,
            @JsonProperty("size") Long size,
            @JsonProperty("time") Date time,
            @JsonProperty("isDirectory") Boolean isDirectory) {
        this.name = name;
        this.time = time;
        this.size = size;
        this.isDirectory = isDirectory;
    }

    public String getName() {
        return name;
    }

    public Long getSize() {
        return size;
    }

    public Date getTime() {
        return time;
    }

    public Boolean getDirectory() {
        return isDirectory;
    }

    @Override
    public String toString() {
        return "FileInfo{" +
                "name='" + name + '\'' +
                ", size=" + size +
                ", time=" + time +
                ", isDirectory=" + isDirectory +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileInfo fileInfo = (FileInfo) o;
        return Objects.equals(name, fileInfo.name) &&
                Objects.equals(size, fileInfo.size) &&
                Objects.equals(time, fileInfo.time) &&
                Objects.equals(isDirectory, fileInfo.isDirectory);
    }

    @Override
    public int hashCode() {

        return Objects.hash(name, size, time, isDirectory);
    }
}

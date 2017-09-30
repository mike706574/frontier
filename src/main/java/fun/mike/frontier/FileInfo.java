package fun.mike.frontier;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FileInfo {
    private final String name;
    private final Long size;
    private final LocalDateTime time;

    @JsonCreator
    public FileInfo(@JsonProperty("name") String name,
                    @JsonProperty("name") Long size,
                    @JsonProperty("name") LocalDateTime time) {
        this.name = name;
        this.time = time;
        this.size = size;
    }

    public String getName() {
        return this.name;
    }

    public Long getSize() {
        return this.size;
    }

    public LocalDateTime getTime() {
        return time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileInfo fileInfo = (FileInfo) o;

        if (!name.equals(fileInfo.name)) return false;
        if (!size.equals(fileInfo.size)) return false;
        return time.equals(fileInfo.time);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + size.hashCode();
        result = 31 * result + time.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "FileInfo{" +
                "name='" + name + '\'' +
                ", size=" + size +
                ", time=" + time +
                '}';
    }
}

package org.example.utils;

public class FileEntry {
    private final String path;
    private final String hash;
    private final long lastModifiedTime;
    private final long size;

    public FileEntry(String path, String hash, long lastModifiedTime, long size) {
        this.path = path;
        this.hash = hash;
        this.lastModifiedTime = lastModifiedTime;
        this.size = size;
    }

    public String getPath() {
        return path;
    }

    public String getHash() {
        return hash;
    }

    public long getLastModifiedTime() {
        return lastModifiedTime;
    }

    public long getSize() {
        return size;
    }

    @Override
    public String toString() {
        return "FileEntry{" +
                "path='" + path + '\'' +
                ", hash='" + hash + '\'' +
                ", lastModifiedTime=" + lastModifiedTime +
                ", size=" + size +
                '}';
    }
}

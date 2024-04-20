package org.example.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class CommitEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    private String treeHash;
    private String parents;
    private Map<String, String> filesHashes;
    private String message;
    private Date timestamp;
    private String commitHash;
    private String author;
    private Map<String, String> blobs;

    public String getCommitHash() {
        return commitHash;
    }

    public void setCommitHash(String commitHash) {
        this.commitHash = commitHash;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Map<String, String> getBlobs() {
        return blobs;
    }

    public void setBlobs(Map<String, String> blobs) {
        this.blobs = blobs;
    }

    public CommitEntity(String commitHash, String treeHash, String parentCommitHash, Map<String, String> filesHashes, String message, Date timestamp, String author, Map<String, String> blobs) {
        this.commitHash = commitHash;
        this.treeHash = treeHash;
        this.parents = parentCommitHash;
        this.filesHashes = filesHashes;
        this.message = message;
        this.timestamp = timestamp;
        this.author = author;
        this.blobs = blobs;
    }

    public String getTreeHash() {
        return treeHash;
    }

    public void setTreeHash(String treeHash) {
        this.treeHash = treeHash;
    }

    public String getParents() {
        return parents;
    }

    public void setParents(String parents) {
        this.parents = parents;
    }

    public Map<String, String> getFilesHashes() {
        return filesHashes;
    }

    public void setFilesHashes(Map<String, String> fileHashes) {
        this.filesHashes = fileHashes;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CommitEntity {");
        sb.append("\n commit hash ").append(commitHash).append(",");
        sb.append("\n  treeHash: '").append(treeHash).append("',");
        sb.append("\n  parents: '").append(parents).append("',");
        sb.append("\n  fileHashes: ").append(filesHashes);
        sb.append("\n  },");
        sb.append("\n  message: '").append(message).append("',");
        sb.append("\n  timestamp: '").append(timestamp).append("'");
        sb.append("\n  author: '").append(author).append("'");
        sb.append("\n}");
        return sb.toString();
    }
}

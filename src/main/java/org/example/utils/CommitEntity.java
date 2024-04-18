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
    private List<String> filesHashes;
    private String message;
    private Date timestamp;

    public CommitEntity(String treeHash, String parentCommitHash, List<String> filesHashes, String message, Date timestamp) {
        this.treeHash = treeHash;
        this.parents = parentCommitHash;
        this.filesHashes = filesHashes;
        this.message = message;
        this.timestamp = timestamp;
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

    public List<String> getFilesHashes() {
        return filesHashes;
    }

    public void setFilesHashes(List<String> fileHashes) {
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
        sb.append("\n  treeHash: '").append(treeHash).append("',");
        sb.append("\n  parents: '").append(parents).append("',");
        sb.append("\n  fileHashes: {");
        if (filesHashes != null) {
            filesHashes.forEach((str) -> sb.append("\n    ").append(str));
        }
        sb.append("\n  },");
        sb.append("\n  message: '").append(message).append("',");
        sb.append("\n  timestamp: '").append(timestamp).append("'");
        sb.append("\n}");
        return sb.toString();
    }
}

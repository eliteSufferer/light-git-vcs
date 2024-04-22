package org.example.utils;

import java.util.Map;
import java.io.Serializable;

public class StashEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private Map<String, String> snapshotWorkingDirectory;
    private String snapshotIndex;
    private String branchName;

    public StashEntry(Map<String, String> snapshotWorkingDirectory, String snapshotIndex, String branchName) {
        this.snapshotWorkingDirectory = snapshotWorkingDirectory;
        this.snapshotIndex = snapshotIndex;
        this.branchName = branchName;
    }

    public Map<String, String> getSnapshotWorkingDirectory() {
        return snapshotWorkingDirectory;
    }

    public String getSnapshotIndex() {
        return snapshotIndex;
    }

    public String getBranchName() {
        return branchName;
    }
}

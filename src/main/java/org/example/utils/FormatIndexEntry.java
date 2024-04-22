package org.example.utils;

import java.nio.file.attribute.BasicFileAttributes;

public class FormatIndexEntry {

    public static String formatIndexEntry(String filePath, String hashCode, BasicFileAttributes fileData) {
        return String.format("%s %s %d %d%s",
                filePath,
                hashCode,
                fileData.lastModifiedTime().toMillis(),
                fileData.size(),
                System.lineSeparator());
    }
}

package org.example.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class IndexParser {
    public static List<FileEntry> parseIndex(Path indexPath) throws IOException {
        List<String> lines = Files.readAllLines(indexPath);
        List<FileEntry> entries = new ArrayList<>();

        for (String line : lines) {
            String[] parts = line.split(" ");
            if (parts.length != 4) continue; // Возможно, стоит добавить логирование или ошибку

            String path = parts[0];
            String hash = parts[1];
            long lastModifiedTime = Long.parseLong(parts[2]);
            long size = Long.parseLong(parts[3]);

            entries.add(new FileEntry(path, hash, lastModifiedTime, size));
        }

        return entries;
    }
}

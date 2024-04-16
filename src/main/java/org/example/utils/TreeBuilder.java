package org.example.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class TreeBuilder {
    public static Map<String, String> buildTrees(List<FileEntry> entries, Path objectsPath) throws IOException {
        // Группировка по папкам
        Map<String, List<FileEntry>> groupedByDirectory = entries.stream()
                .collect(Collectors.groupingBy(entry -> {
                    Path p = Paths.get(entry.getPath()).getParent();
                    return p == null ? "" : p.toString().replace("\\", "/");
                }));

        Map<String, String> directoryHashes = new HashMap<>();

        // Сортируем ключи по глубине вложенности (для корректной обработки)
        List<String> sortedDirectories = new ArrayList<>(groupedByDirectory.keySet());
        sortedDirectories.sort(Comparator.comparingInt(dir -> dir.length() - dir.replace("/", "").length()));

        for (String dir : sortedDirectories) {
            StringBuilder treeContent = new StringBuilder();
            for (FileEntry file : groupedByDirectory.get(dir)) {
                Path filePath = Paths.get(file.getPath());
                String fileName = filePath.getFileName().toString();
                treeContent.append("blob ").append(file.getHash()).append(" ").append(fileName).append("\n");
            }

            String treeHash = SHA1.apply(treeContent.toString().getBytes());
            directoryHashes.put(dir, treeHash);

            // Используем первые два символа хеша для имени директории
            String dirName = treeHash.substring(0, 2);
            String fileName = treeHash.substring(2);
            Path treeDirectory = objectsPath.resolve(dirName);
            Path treeFile = treeDirectory.resolve(fileName);

            Files.createDirectories(treeDirectory);  // Убедитесь, что директория создана
            Files.writeString(treeFile, treeContent.toString());  // Запись содержимого дерева
        }

        return directoryHashes;
    }
}

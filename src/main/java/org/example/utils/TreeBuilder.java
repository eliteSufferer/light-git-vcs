package org.example.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class TreeBuilder {
    public static Map<String, String> buildTrees(List<FileEntry> entries, Path objectsPath) throws IOException {
        Map<String, List<FileEntry>> groupedByDirectory = entries.stream()
                .collect(Collectors.groupingBy(entry -> {
                    Path p = Paths.get(entry.getPath()).getParent();
                    return p == null ? "" : p.toString().replace("\\", "/");
                }));


        // Добавляем корневую директорию, если она отсутствует
        groupedByDirectory.putIfAbsent("", new ArrayList<>());
        System.out.println(groupedByDirectory);

        Map<String, String> directoryHashes = new HashMap<>();
        List<String> sortedDirectories = new ArrayList<>(groupedByDirectory.keySet());
        sortedDirectories.sort(Comparator.comparingInt(dir -> -dir.length() + dir.replace("/", "").length()));
        System.out.println(sortedDirectories);
        for (String dir : sortedDirectories) {
            StringBuilder treeContent = new StringBuilder();
            for (FileEntry file : groupedByDirectory.get(dir)) {
                Path filePath = Paths.get(file.getPath());
                String fileName = filePath.getFileName().toString();
                treeContent.append("blob ").append(file.getHash()).append(" ").append(fileName).append("\n");
            }

            String treeHash = SHA1.apply(treeContent.toString().getBytes());
            directoryHashes.put(dir, treeHash);

            // Сохраняем дерево
            saveTree(treeHash, treeContent.toString(), objectsPath);
        }

        // Обновляем ссылки на поддиректории
        for (String dir : sortedDirectories) {
            System.out.println(dir);
            if (!dir.isEmpty()) {
                Path dirPath = Paths.get(dir);
                System.out.println("dir: " + dir);
                String parentDir = dirPath.getParent() == null ? "" : dirPath.getParent().toString().replace("\\", "/");
                System.out.println("parent dir: " + parentDir);
                String dirName = dirPath.getFileName().toString();
                System.out.println("dir name: " + dirName);
                String treeHash = directoryHashes.get(dir);
                System.out.println("dir hash: " + treeHash);

                if (directoryHashes.containsKey(parentDir)) {
                    String parentTreeContent = Files.readString(objectsPath.resolve(directoryHashes.get(parentDir).substring(0, 2)).resolve(directoryHashes.get(parentDir).substring(2)));
                    parentTreeContent += "tree " + treeHash + " " + dirName + "\n";
                    String updatedParentTreeHash = SHA1.apply(parentTreeContent.getBytes());
                    directoryHashes.put(parentDir, updatedParentTreeHash);

                    // Сохраняем обновленное дерево
                    saveTree(updatedParentTreeHash, parentTreeContent, objectsPath);
                }
            }
        }

        return directoryHashes;
    }

    private static void saveTree(String treeHash, String treeContent, Path objectsPath) throws IOException {
        String dirName = treeHash.substring(0, 2);
        String fileName = treeHash.substring(2);
        Path treeDirectory = objectsPath.resolve(dirName);
        Path treeFile = treeDirectory.resolve(fileName);

        Files.createDirectories(treeDirectory);
        Files.writeString(treeFile, treeContent);
    }
}

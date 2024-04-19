package org.example.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class TreeBuilder {
    public static Map<String, String> buildTrees(List<FileEntry> entries, Path objectsPath) throws IOException {
        Map<String, List<FileEntry>> groupedByDirectory = new HashMap<>();

        // Заполнение groupedByDirectory, добавляя файлы в их непосредственные директории и учитывая все родительские директории
        for (FileEntry entry : entries) {
            Path filePath = Paths.get(entry.getPath());
            Path parentPath = filePath.getParent();
            if (parentPath == null){
                groupedByDirectory.putIfAbsent("", new ArrayList<>());
                groupedByDirectory.get("").add(entry);
            }
            while (parentPath != null) {
                String directory = parentPath.toString().replace("\\", "/");
                groupedByDirectory.putIfAbsent(directory, new ArrayList<>());
                // Добавляем файл только в его непосредственную директорию
                if (parentPath.equals(filePath.getParent())) {
                    groupedByDirectory.get(directory).add(entry);
                }
                parentPath = parentPath.getParent();
            }
        }

        // Добавляем корневую директорию, если она отсутствует
        groupedByDirectory.putIfAbsent("", new ArrayList<>());

        Map<String, String> directoryHashes = new HashMap<>();
        List<String> sortedDirectories = new ArrayList<>(groupedByDirectory.keySet());
        sortedDirectories.sort((a, b) -> b.length() - a.length());  // Сортируем директории по убыванию глубины
        System.out.println(sortedDirectories);
        for (String dir : sortedDirectories) {
            System.out.println("SEE DIR : " + dir);
            StringBuilder treeContent = new StringBuilder();
            System.out.println("grop DIR: " + groupedByDirectory.get(dir));
            for (FileEntry file : groupedByDirectory.get(dir)) {
                System.out.println("file" + file);
                Path filePath = Paths.get(file.getPath());
                String fileName = filePath.getFileName().toString();
                treeContent.append("blob ").append(file.getHash()).append(" ").append(fileName).append("\n");
                System.out.println("tree Content: " + treeContent);
            }

            String treeHash = SHA1.apply(treeContent.toString().getBytes());
            directoryHashes.put(dir, treeHash);

            // Сохраняем дерево
            saveTree(treeHash, treeContent.toString(), objectsPath);
        }
        System.out.println("DHASH: " + directoryHashes);

        // Обновляем ссылки на поддиректории
        for (String dir : sortedDirectories) {
            System.out.println("dirNOW: " + dir);
            System.out.println(!dir.isEmpty());
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
        System.out.println("DIR GASH: " + directoryHashes);
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

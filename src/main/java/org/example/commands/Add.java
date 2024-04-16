package org.example.commands;
import org.example.utils.RecursiveSearch;
import org.example.utils.SHA1;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Stream;

public class Add extends AbstractCommand {
    public Add() {
        super("add", "Add file contents to the index");
    }

    @Override
    public void execute(String[] args) {
        String fileName = args[1];
        Path filePath = Paths.get(fileName).toAbsolutePath().normalize();

        Path repositoryRoot = RecursiveSearch.findRepositoryRoot(filePath);
        if (repositoryRoot == null) {
            System.out.println("Не найден корень репозитория .gitler.");
            return;
        }

        if (!Files.exists(filePath)) {
            System.out.println("Файл или директория " + fileName + " не найдена.");
            return;
        }

        try (Stream<Path> paths = Files.walk(filePath, FileVisitOption.FOLLOW_LINKS)) {
            paths.filter(Files::isRegularFile)
                    .forEach(path -> addFile(path, repositoryRoot));
        } catch (IOException e) {
            System.out.println("Ошибка при добавлении: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addFile(Path filePath, Path repositoryRoot) {
        try {
            byte[] fileContent = Files.readAllBytes(filePath);
            String hashCode = SHA1.apply(fileContent);

            Path indexPath = repositoryRoot.resolve(".gitler/index");
            if (!Files.exists(indexPath)) {
                Files.createFile(indexPath);
            }
            Path objectsPath = repositoryRoot.resolve(".gitler/objects");
            Path hashDir = objectsPath.resolve(hashCode.substring(0, 2));
            Path blobFile = hashDir.resolve(hashCode.substring(2));
            BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);

            Files.createDirectories(hashDir);

            if (!Files.exists(blobFile)) {
                Files.write(blobFile, fileContent);
                Files.writeString(indexPath, formatIndexEntry(filePath, repositoryRoot, hashCode, attrs), java.nio.file.StandardOpenOption.APPEND);
                System.out.println("Файл " + filePath.getFileName() + " добавлен в индекс.");
            } else {
                System.out.println("Файл " + filePath.getFileName() + " уже существует в objects.");
            }
        } catch (IOException e) {
            System.out.println("Ошибка при добавлении файла " + filePath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String formatIndexEntry(Path filePath, Path repositoryRoot, String hashCode, BasicFileAttributes attrs) {
        Path relativePath = repositoryRoot.relativize(filePath);
        return String.format("%s %s %d %d%s",
                relativePath.toString(),
                hashCode,
                attrs.lastModifiedTime().toMillis(),
                attrs.size(),
                System.lineSeparator());
    }
}

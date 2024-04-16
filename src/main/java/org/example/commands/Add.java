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
    public void execute(String fileName) {
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
            paths.forEach(path -> {
                if (Files.isRegularFile(path)) {
                    addFile(path, repositoryRoot);
                }
            });
        } catch (IOException e) {
            System.out.println("Ошибка при добавлении: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void addFile(Path filePath, Path repositoryRoot) {
        try {
            Path relativePath = repositoryRoot.relativize(filePath);
            BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
            String fileContent = Files.readString(filePath);
            String hashCode = SHA1.apply(fileContent);
            Path indexPath = repositoryRoot.resolve(".gitler/index");
            Path objectsPath = repositoryRoot.resolve(".gitler/objects");

            if (!Files.exists(indexPath)) {
                Files.createFile(indexPath);
            }

            Path hashDir = objectsPath.resolve(hashCode.substring(0, 2));
            Files.createDirectories(hashDir);
            Path blobFile = hashDir.resolve(hashCode.substring(2));

            if (!Files.exists(blobFile)) {
                Files.createFile(blobFile);
                Files.writeString(blobFile, fileContent);

                String indexEntry = String.format("%s %s %d %d %s%n",
                        relativePath.toString(),
                        hashCode,
                        attrs.lastModifiedTime().toMillis(),
                        attrs.size(),
                        System.lineSeparator());

                Files.writeString(indexPath, indexEntry, java.nio.file.StandardOpenOption.APPEND);
                System.out.println("Файл " + relativePath + " добавлен в индекс.");
            } else {
                System.out.println("Файл " + relativePath + " уже существует в objects.");
            }
        } catch (IOException e) {
            System.out.println("Ошибка при добавлении файла " + filePath + ": " + e.getMessage());
        }
    }
}

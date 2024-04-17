package org.example.commands;
import org.example.utils.Constants;
import org.example.utils.RecursiveSearch;
import org.example.utils.SHA1;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Add extends AbstractCommand {
    public Add() {
        super("add", "Add file contents to the index");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 2) {
            System.out.println("Нет файлов для добавления.");
            return;
        }

        Path repositoryRoot = RecursiveSearch.findRepositoryRoot(Paths.get(".").toAbsolutePath().normalize());
        if (repositoryRoot == null) {
            System.out.println("Не найден корень репозитория .gitler.");
            return;
        }

        // Обработка всех аргументов начиная с args[1]
        for (int i = 1; i < args.length; i++) {
            Path filePath = Paths.get(args[i]).toAbsolutePath().normalize();
            if (!Files.exists(filePath)) {
                System.out.println("Файл или директория " + filePath + " не найдена.");
                continue;
            }

            try (Stream<Path> paths = Files.walk(filePath, FileVisitOption.FOLLOW_LINKS)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> !path.startsWith(repositoryRoot.resolve(".gitler")))  // Добавляем фильтр для игнорирования .gitler
                        .forEach(path -> addFile(path, repositoryRoot));
            }
            catch (IOException e) {
                System.out.println("Ошибка при добавлении: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void addFile(Path filePath, Path repositoryRoot) {
        try {
            byte[] fileContent = Files.readAllBytes(filePath);
            String currentHash = SHA1.apply(fileContent);

            Path indexPath = repositoryRoot.resolve(".gitler/index");
            if (!Files.exists(indexPath)){
                Files.createFile(indexPath);
            }
            Map<String, String> indexEntries = readIndexEntries(indexPath);
            String indexHash = indexEntries.get(repositoryRoot.relativize(filePath).toString());
            if (!Files.exists(filePath)){
                System.err.println("file doesn't exist");
                return;
            }
            else{
                if (indexHash != null && indexHash.equals(currentHash)) {
                    System.out.println("Файл " + filePath.getFileName() + " не изменился и не будет добавлен в индекс повторно.");
                    return;
                }
            }
            List<String> lines = Files.readAllLines(Paths.get(indexPath.toUri()));
            System.out.println(repositoryRoot.relativize(filePath));
            List<String> changedIndex = lines.stream().filter(line -> !line.startsWith(repositoryRoot.relativize(filePath).toString())).toList();
            Files.write(indexPath, changedIndex);

            Path objectsPath = repositoryRoot.resolve(".gitler/objects");
            Path hashDir = objectsPath.resolve(currentHash.substring(0, 2));
            Path blobFile = hashDir.resolve(currentHash.substring(2));
            BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);

            Files.createDirectories(hashDir);
            Files.write(blobFile, fileContent);
            Files.writeString(indexPath, formatIndexEntry(filePath, repositoryRoot, currentHash, attrs), java.nio.file.StandardOpenOption.APPEND);
            System.out.println("Файл " + filePath.getFileName() + " обновлен и добавлен в индекс.");

        } catch (IOException e) {
            System.out.println("Ошибка при добавлении файла " + filePath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Map<String, String> readIndexEntries(Path indexPath) throws IOException {
        Map<String, String> entries = new HashMap<>();
        if (Files.exists(indexPath)) {
            List<String> lines = Files.readAllLines(indexPath);
            for (String line : lines) {
                System.out.println("LINE: " + line);
                String[] parts = line.split(" ");
                if (parts.length < 4) continue;
                entries.put(parts[0], parts[1]); // path -> hash
                System.out.println(parts[0] + " " + parts[1]);
            }
        }
        System.out.println(entries.entrySet());
        return entries;
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

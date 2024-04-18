package org.example.commands;

import org.example.utils.Constants;
import org.example.utils.FlagParser;
import org.example.utils.RecursiveSearch;
import org.example.utils.SHA1;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Add extends AbstractCommand {
    private Map<String, Boolean> options = new HashMap<>();

    public Add() {
        super("add", "Add file contents to the index");
        this.options.put("-A", false);
        this.options.put("--all", false);
        this.options.put("-n", false);
        this.options.put("--dry-run", true);

    }

    @Override
    public void execute(String[] args) {
        Map<Boolean, Map<String, Object>> parsedData = FlagParser.parseFlags(options, args);
        Boolean key = parsedData.keySet().iterator().next();
        if (!key) {
            System.out.println("Некорректное использование комманды add");
        }
//        System.out.println(key);
//        System.out.println(parsedData.keySet());
//        if (parsedData.keySet() == true)
//        System.out.println(parsedData);
//        System.exit(1);
        Path repositoryRoot = RecursiveSearch.findRepositoryRoot(Paths.get(".").toAbsolutePath().normalize());
        List<String> ignorePatterns;
        try {
            ignorePatterns = readIgnorePatterns(repositoryRoot);
        } catch (IOException e) {
            System.err.println("Ошибка при чтении .gitlerignore: " + e.getMessage());
            return;
        }

        Map<String, String> flagsMap = (Map<String, String>) parsedData.get(key).get("flags");
        ArrayList<String> argPaths = (ArrayList<String>) parsedData.get(key).get("args");
//        System.out.println(flagsMap);
//        System.out.println(argPaths);
//
//        System.out.println("1");


        if (args.length < 2) {
            System.out.println("Нет файлов для добавления."); // Важно для избежания ошибок при отсутствии аргументов
            return;
        }


        if (repositoryRoot == null) {
            System.out.println("Не найден корень репозитория .gitler.");
            return;
        }

        // Обработка всех аргументов начиная с args[1]
        if (flagsMap.containsKey("--all") || flagsMap.containsKey("-A")) {
            filerFilesToAdd(repositoryRoot, ignorePatterns, repositoryRoot);
            return;
        }

        for (int i = 0; i < argPaths.size(); i++) {
            Path filePath = Paths.get(argPaths.get(i)).toAbsolutePath().normalize();
            if (!Files.exists(filePath)) {

                System.out.println("Файл или директория " + filePath + " не найдена.");
                continue;
            }

            filerFilesToAdd(filePath, ignorePatterns, repositoryRoot);
        }
    }

    private void filerFilesToAdd(Path directoryPath, List<String> ignorePatterns, Path repositoryRoot) {
        try (Stream<Path> paths = Files.walk(directoryPath, FileVisitOption.FOLLOW_LINKS)) {
            Set<Path> existingFiles = paths.filter(Files::isRegularFile)
                    .filter(path -> !shouldIgnore(path, ignorePatterns, repositoryRoot))
                    .filter(path -> !path.startsWith(repositoryRoot.resolve(".gitler")))
                    .collect(Collectors.toSet());

            // Получаем все пути из индекса
            Map<String, String> indexEntries = readIndexEntries(repositoryRoot.resolve(".gitler/index"));
            Set<Path> indexedPaths = indexEntries.keySet().stream()
                    .map(pathStr -> repositoryRoot.resolve(pathStr))
                    .collect(Collectors.toSet());

            // Добавляем новые или измененные файлы
            existingFiles.forEach(file -> addFile(file, repositoryRoot));

            // Проверяем удаленные файлы
            indexedPaths.stream()
                    .filter(path -> !existingFiles.contains(path))
                    .forEach(path -> {
                        try {
                            removeFileFromIndex(path, repositoryRoot);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (IOException e) {
            System.out.println("Ошибка при добавлении: " + e.getMessage());
            e.printStackTrace();
        }
    }



    private void addFile(Path filePath, Path repositoryRoot) {
        try {
            System.out.println("filePath: " + filePath);
            if (!Files.exists(filePath)) {
                removeFileFromIndex(filePath, repositoryRoot); // Удаление записи из индекса
                System.out.println("Файл " + filePath.getFileName() + " удален и будет удален из индекса.");
                return;
            }
            byte[] fileContent = Files.readAllBytes(filePath);
            String currentHash = SHA1.apply(fileContent);

            Path indexPath = repositoryRoot.resolve(".gitler/index");
            if (!Files.exists(indexPath)) {
                Files.createFile(indexPath);
            }
            Map<String, String> indexEntries = readIndexEntries(indexPath);
            String indexHash = indexEntries.get(repositoryRoot.relativize(filePath).toString());
            if (!Files.exists(filePath)) {
                System.err.println("file doesn't exist");
                return;
            } else {
                if (indexHash != null && indexHash.equals(currentHash)) {
                    System.out.println("Файл " + filePath.getFileName() + " не изменился и не будет добавлен в индекс повторно.");
                    return;
                }
            }
            List<String> lines = Files.readAllLines(Paths.get(indexPath.toUri()));
//            System.out.println(repositoryRoot.relativize(filePath));
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

    private void removeFileFromIndex(Path filePath, Path repositoryRoot) throws IOException {
        Path indexPath = repositoryRoot.resolve(".gitler/index");
        List<String> lines = Files.readAllLines(indexPath);
        List<String> updatedLines = lines.stream()
                .filter(line -> !line.startsWith(repositoryRoot.relativize(filePath).toString()))
                .collect(Collectors.toList());
        Files.write(indexPath, updatedLines, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        System.out.println("Файл " + filePath.getFileName() + " удален из индекса.");
    }

    private Map<String, String> readIndexEntries(Path indexPath) throws IOException {
        Map<String, String> entries = new HashMap<>();
        if (Files.exists(indexPath)) {
            List<String> lines = Files.readAllLines(indexPath);
            for (String line : lines) {
//                System.out.println("LINE: " + line);
                String[] parts = line.split(" ");
                if (parts.length < 4) continue;
                entries.put(parts[0], parts[1]); // path -> hash
//                System.out.println(parts[0] + " " + parts[1]);
            }
        }
//        System.out.println(entries.entrySet());
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

    private boolean shouldIgnore(Path filePath, List<String> ignorePatterns, Path repositoryRoot) {
        String relativePath = repositoryRoot.relativize(filePath).toString();
        for (String pattern : ignorePatterns) {
            if (relativePath.matches(pattern)) { // Простая проверка соответствия паттерну (можно усложнить)
                return true;
            }
        }
        return false;
    }

    private List<String> readIgnorePatterns(Path repositoryRoot) throws IOException {
        Path ignoreFilePath = repositoryRoot.resolve(".gitlerignore");
        if (Files.exists(ignoreFilePath)) {
            return Files.readAllLines(ignoreFilePath).stream()
                    .filter(line -> !line.trim().isEmpty() && !line.startsWith("#")) // Игнорировать пустые строки и комментарии
                    .collect(Collectors.toList());
        }
        return new ArrayList<>(); // Возвращаем пустой список, если файл .gitlerignore не существует
    }


}

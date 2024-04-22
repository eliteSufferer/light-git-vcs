package org.example.commands;

import org.example.utils.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Stash extends AbstractCommand {
    private Map<String, Boolean> options = new HashMap<>();
    public Stash() {
        super("stash", "stash ");
    }

    @Override
    public void execute(String[] commandArgument) throws IOException {
        Map<Boolean, Map<String, Object>> parsedData = FlagParser.parseFlags(options, commandArgument);
        Boolean key = parsedData.keySet().iterator().next();
        if (!key) {
            System.out.println("Некорректное использование комманды commit");
        }
        Map<String, String> flagsMap = (Map<String, String>) parsedData.get(key).get("flags");
        ArrayList<String> argPaths = (ArrayList<String>) parsedData.get(key).get("args");

        Path stashPath = Paths.get(Constants.STASH_DIR);



        Stack<StashEntry> stack;
        if (!argPaths.isEmpty()){
            if (!Files.exists(stashPath)) {
                System.out.println("stash - пуст");
                return;
            }else{
                if (Objects.equals(argPaths.get(0), "apply")){
                    applyStash(false);
                } else if (Objects.equals(argPaths.get(0), "pop")) {
                    applyStash(true);
                }

                return;
            }
        }


        if (!Files.exists(stashPath)) {
            Files.createDirectory(stashPath);
            Files.createFile(Paths.get(Constants.STASH_FILE));
        }
        Path repositoryRoot = RecursiveSearch.findRepositoryRoot(Paths.get(".").toAbsolutePath().normalize());
        Path indexPath = Paths.get(Constants.INDEX_FILE);
        Map<String, String> indexEntries = new HashMap<>();
        if (Files.exists(indexPath)) {
            indexEntries = Status.readIndexEntries(indexPath);
        }

        List<Path> workingDirectoryFiles = Files.walk(repositoryRoot)
                .filter(Files::isRegularFile)
                .toList();

        Map<String, String> workingDirSnapshot = createSnapshotOfWorkingDirectory(indexEntries, workingDirectoryFiles, repositoryRoot);
        String indexSnapShot = createSnapshotOfIndex();
        String branchName = Branch.getCurrentBranchName();
        StashEntry stashEntry = new StashEntry(
                workingDirSnapshot,
                indexSnapShot,
                branchName
        );




        if (!Commit.isFileEmpty(Paths.get(Constants.STASH_FILE))) {
            stack = SerializationUtil.deserialize(Constants.STASH_FILE);
//            System.out.println("CUM: " + stack.toString());
            if (stack == null) {
                stack = new Stack<>();
            }
            stack.push(stashEntry);
            SerializationUtil.serialize(stack, Constants.STASH_FILE);
        }else{
            stack = new Stack<>();
            stack.push(stashEntry);
            System.out.println(stack);
            SerializationUtil.serialize(stack, Constants.STASH_FILE);
        }

        clearIndexedFilesFromWorkingDirectory(indexEntries, repositoryRoot);
        String currentCommitHash = Branch.getCurrentCommit();
        Reset.resetWorkingDirectory(currentCommitHash);
        RestoreIndex.restoreIndex(currentCommitHash);


    }
    public void applyStash(boolean isPop) throws IOException {
        Path stashFilePath = Paths.get(Constants.STASH_FILE);
        if (Files.exists(stashFilePath) && !Commit.isFileEmpty(stashFilePath)) {
            Stack<StashEntry> stashStack = SerializationUtil.deserialize(Constants.STASH_FILE);
            StashEntry lastStash;
            if (!stashStack.isEmpty()) {
                if (isPop){
                    lastStash = stashStack.pop(); // Получаем последний StashEntry с удалением
                }else{
                 lastStash = stashStack.peek(); // Получаем последний StashEntry без удаления
                }

                restoreWorkingDirectory(lastStash.getSnapshotWorkingDirectory());
                restoreIndex(lastStash.getSnapshotIndex());

                System.out.println("Stash applied successfully.");
            } else {
                System.out.println("No stashes to apply.");
            }
        } else {
            System.out.println("Stash file is empty or does not exist.");
        }
    }
    private void restoreWorkingDirectory(Map<String, String> workingDirectorySnapshot) throws IOException {
        Path repositoryRoot = RecursiveSearch.findRepositoryRoot(Paths.get(".").toAbsolutePath().normalize());
        System.out.println(workingDirectorySnapshot);
        for (Map.Entry<String, String> fileEntry : workingDirectorySnapshot.entrySet()) {
            Path filePath = repositoryRoot.resolve(fileEntry.getKey());
            System.out.println("File path: " + filePath);
            if (!Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent()); // Создаем родительскую директорию, если она не существует
            }
            try {
                Files.writeString(filePath, fileEntry.getValue(), StandardCharsets.UTF_8);
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }
    private void restoreIndex(String indexSnapshot) throws IOException {
        Path indexPath = Paths.get(Constants.INDEX_FILE);
        Files.writeString(indexPath, indexSnapshot, StandardCharsets.UTF_8);
    }

    public Map<String, String> createSnapshotOfWorkingDirectory(Map<String, String> indexEntries, List<Path> workingDirectoryFiles, Path repositoryRoot) throws IOException {
        Map<String, String> snapshot = new HashMap<>();
        Set<String> workingDirectoryPaths = workingDirectoryFiles.stream()
                .filter(Files::isRegularFile)
                .map(file -> repositoryRoot.relativize(file).toString().replace("\\", "/"))
                .collect(Collectors.toSet());

        // Проверка на модифицированные и удаленные файлы
        for (Map.Entry<String, String> entry : indexEntries.entrySet()) {
            String indexedPath = entry.getKey();

            if (workingDirectoryPaths.contains(indexedPath)) {
                try {
                    String content = Files.readString(Paths.get(indexedPath));
                    snapshot.put(indexedPath, content);
                } catch (IOException e) {
                    System.err.println("Error reading file: " + indexedPath);
                }
            }
        }
        return snapshot;
    }

    public String createSnapshotOfIndex() throws IOException {
        return Files.readString(Paths.get(Constants.INDEX_FILE));
    }

    public void clearIndexedFilesFromWorkingDirectory(Map<String, String> indexEntries, Path repositoryRoot) throws IOException {
        Set<Path> directoriesToCheck = new TreeSet<>(Comparator.comparingInt(path -> path.toString().length()).reversed()); // Сортируем пути по убыванию глубины

        for (String filePath : indexEntries.keySet()) {
            Path fullPath = repositoryRoot.resolve(filePath);
            try {
                Files.deleteIfExists(fullPath);
                System.out.println("Deleted file: " + fullPath);
                // Добавляем родительскую директорию в список на проверку
                if (fullPath.getParent() != null) {
                    directoriesToCheck.add(fullPath.getParent());
                }
            } catch (IOException e) {
                System.err.println("Failed to delete file: " + fullPath);
                e.printStackTrace();
            }
        }

        // Проверяем и удаляем пустые директории начиная с самой вложенной
        for (Path directory : directoriesToCheck) {
            removeEmptyDirectories(directory, repositoryRoot);
        }
    }
    private void removeEmptyDirectories(Path directory, Path repositoryRoot) throws IOException {
        // Не удаляем корневую директорию и выше не поднимаемся
        while (directory != null && !repositoryRoot.equals(directory) && Files.exists(directory)) {
            if (isDirectoryEmpty(directory)) {
                Files.delete(directory);
                System.out.println("Deleted empty directory: " + directory);
                directory = directory.getParent();
            } else {
                break;
            }
        }
    }
    private boolean isDirectoryEmpty(Path directory) throws IOException {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
            return !dirStream.iterator().hasNext(); // Проверяем, есть ли в директории файлы
        }
    }




}

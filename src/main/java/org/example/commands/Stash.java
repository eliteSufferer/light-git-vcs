package org.example.commands;

import org.example.utils.*;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Stash extends AbstractCommand {
    public Stash() {
        super("stash", "stash ");
    }

    @Override
    public void execute(String[] commandArgument) throws IOException {

        Path stashPath = Paths.get(Constants.STASH_DIR);
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


        Stack<StashEntry> stack;
        if (!Commit.isFileEmpty(Paths.get(Constants.STASH_FILE))) {
            stack = SerializationUtil.deserialize(Constants.STASH_FILE);
//            System.out.println("CUM: " + stack.toString());
            if (stack == null) {
                stack = new Stack<>();
            }
            stack.push(stashEntry);
            SerializationUtil.serialize(stashEntry, Constants.STASH_FILE);
        }else{
            stack = new Stack<>();
            stack.push(stashEntry);
            System.out.println(stack);
            SerializationUtil.serialize(stack, Constants.STASH_FILE);
        }

        clearIndexedFilesFromWorkingDirectory(indexEntries, repositoryRoot);
        String currentCommitHash = Branch.getCurrentCommit();
        Checkout.restoreWorkingDir();


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

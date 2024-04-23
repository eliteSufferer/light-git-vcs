package org.example.commands;

import org.example.utils.Constants;
import org.example.utils.RecursiveSearch;
import org.example.utils.SHA1;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Status extends AbstractCommand {
    public Status() {
        super("status", "Show the working tree status");
    }
    private Map<String, String> fileHashes;
    @Override
    public void execute(String[] args) {
        try {
            Path repositoryRoot = RecursiveSearch.findRepositoryRoot(Paths.get(".").toAbsolutePath().normalize()); // Получаем корень репозитория
            Path indexPath = repositoryRoot.resolve(".gitler/index");
            Map<String, String> indexEntries = new HashMap<>();
            if (Files.exists(indexPath)){
                indexEntries = readIndexEntries(indexPath);
            }
            List<Path> workingDirectoryFiles = Files.walk(repositoryRoot)
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());


            checkForChangesToBeCommitted(indexEntries, repositoryRoot);

            checkForChangesNotStagedForCommit(indexEntries, workingDirectoryFiles, repositoryRoot);
            if (Files.exists(indexPath)) {
//                System.out.println("Нет индексных файлов, репозиторий пуст.");
                checkForUntrackedFiles(indexEntries, workingDirectoryFiles, repositoryRoot);
            }


        } catch (IOException e) {
            System.err.println("Ошибка при чтении файлов репозитория: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Map<String, String> readIndexEntries(Path indexPath) throws IOException {
        return Files.readAllLines(indexPath).stream()
                .collect(Collectors.toMap(
                        line -> line.split(" ")[0].replace("\\", "/"),
                        line -> line.split(" ")[1].replace("\\", "/")
                ));
    }






    private String getLastCommitHash(Path repositoryRoot) throws IOException {
        Path headPath = repositoryRoot.resolve(".gitler/HEAD");
        String refContent = Files.readString(headPath).trim();  // Пример: "ref: refs/heads/master"


        String branchPathStr = refContent.split(" ")[1];
        Path branchPath = repositoryRoot.resolve(Constants.VCS_DIR + branchPathStr);
        if (Files.exists(branchPath)) {
            return Files.readString(branchPath).trim();  // Хеш последнего коммита
        }

        return null; // В случае отсутствия файла ветки вернуть null
    }
    private String getTreeHashFromCommit(String commitHash, Path repositoryRoot) throws IOException {
        Path commitPath = getFilePath(commitHash, repositoryRoot);
        for (String line : Files.readAllLines(commitPath)) {
            if (line.startsWith("tree ")) {
                return line.split(" ")[1];
            }
        }
        return null;
    }



    private Map<String, String> loadCommitTree(String commitHash, Path repositoryRoot) throws IOException {
        this.fileHashes = new HashMap<>();
        String treeHash = getTreeHashFromCommit(commitHash, repositoryRoot);
        loadTree(treeHash, repositoryRoot, "");
        return fileHashes;
    }

    private void loadTree(String treeHash, Path repositoryRoot, String basePath) throws IOException {
        Path treePath = getFilePath(treeHash, repositoryRoot);
        if (!Files.exists(treePath)) return;

        List<String> treeContents = Files.readAllLines(treePath);
        for (String content : treeContents) {
            String[] parts = content.split(" ");
            if (parts[0].equals("blob")) {
                fileHashes.put(basePath + parts[2], parts[1]);
//                System.out.println(basePath + parts[2] + " ::: " + parts[1]);
            } else if (parts[0].equals("tree")) {
                loadTree(parts[1], repositoryRoot, basePath + parts[2] + "/");
            }
        }
    }

    private Path getFilePath(String hash, Path repositoryRoot) {
        return repositoryRoot.resolve(".gitler/objects").resolve(hash.substring(0, 2)).resolve(hash.substring(2));
    }



    private void checkForChangesToBeCommitted(Map<String, String> indexEntries, Path repositoryRoot) throws IOException {
        System.out.println("Changes to be committed:");
        String lastCommitHash = getLastCommitHash(repositoryRoot);
        if (lastCommitHash == null && Files.exists(Paths.get(Constants.INDEX_FILE))) {
            for (Map.Entry<String, String> indexEntry : indexEntries.entrySet()) {
                String filePath = indexEntry.getKey();
                System.out.println("new file: " + "\033[32m" + filePath + "\033[0m");
            }
            return;

        }else if(lastCommitHash == null && !Files.exists(Paths.get(Constants.INDEX_FILE))){
            System.out.println(indexEntries);
            System.out.println("No previous commits found.");
            return;
        }
        Map<String, String> lastCommitFileHashes = loadCommitTree(lastCommitHash, repositoryRoot);

        for (Map.Entry<String, String> indexEntry : indexEntries.entrySet()) {
            String filePath = indexEntry.getKey();
            String indexFileHash = indexEntry.getValue();
            String lastCommitFileHash = lastCommitFileHashes.get(filePath);

            if (lastCommitFileHash == null) {
                System.out.println("new file: " + "\033[32m" + filePath + "\033[0m");
            } else if (!lastCommitFileHash.equals(indexFileHash)) {
                System.out.println("modified: " + "\033[32m" + filePath + "\033[0m");
            }
        }
        boolean hasParent = false;
        for (String lastCommitFilePath : lastCommitFileHashes.keySet()) {
//            System.out.println("lastCommitHash: " + lastCommitHash);
            for (String line : Files.readAllLines(Paths.get(Constants.OBJECTS_DIR + lastCommitHash.substring(0, 2) + "/" + lastCommitHash.substring(2)))){
                if (line.startsWith("parent commit:")){
                    hasParent = true;
                    break;
                }
            }
//            System.out.println("index ent: " + indexEntries);
//            System.out.println("LCFP: " + lastCommitFilePath);
            if (!indexEntries.containsKey(lastCommitFilePath) && lastCommitFilePath != null) {
                System.out.println("deleted: " + "\033[33m" + lastCommitFilePath + "\033[0m");
            }
        }
    }

    private void checkForChangesNotStagedForCommit(Map<String, String> indexEntries, List<Path> workingDirectoryFiles, Path repositoryRoot) throws IOException {
        System.out.println("Changes not staged for commit:");

        // Создаем множество путей файлов из рабочего каталога для быстрой проверки
        Set<String> workingDirectoryPaths = workingDirectoryFiles.stream()
                .filter(Files::isRegularFile)
                .map(file -> repositoryRoot.relativize(file).toString().replace("\\", "/"))
                .collect(Collectors.toSet());

        // Проверка на модифицированные и удаленные файлы
        for (Map.Entry<String, String> entry : indexEntries.entrySet()) {
            String indexedPath = entry.getKey();
            String indexedHash = entry.getValue();

            if (workingDirectoryPaths.contains(indexedPath)) {
                // Файл существует, проверяем его хеш
                Path filePath = repositoryRoot.resolve(indexedPath);
                byte[] currentContent = Files.readAllBytes(filePath);
                String currentHash = SHA1.apply(currentContent);

                if (!currentHash.equals(indexedHash)) {
                    System.out.println("modified: " + "\033[31m" + indexedPath + "\033[0m");
                }
            } else {
                // Файл отсутствует в рабочем каталоге, значит он удален
                System.out.println("deleted: " + "\033[31m" + indexedPath + "\033[0m");
            }
        }
    }



    private void checkForUntrackedFiles(Map<String, String> indexEntries, List<Path> workingDirectoryFiles, Path repositoryRoot) {
        System.out.println("Untracked files:");

        for (Path file : workingDirectoryFiles) {
            if (Files.isRegularFile(file)) {
                String relativePath = repositoryRoot.relativize(file).toString().replace("\\", "/");
                if (!indexEntries.containsKey(relativePath)) {
                    System.out.println("\33[31m" + relativePath + "\33[0m");
                }
            }
        }
    }
}



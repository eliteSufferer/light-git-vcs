package org.example.commands;

import org.example.utils.Constants;
import org.example.utils.SHA1;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Status extends AbstractCommand {
    public Status() {
        super("status", "Show the working tree status");
    }
    private Map<String, String> fileHashes;
    @Override
    public void execute(String[] args) {
        try {
            Path repositoryRoot = Paths.get(".").toAbsolutePath().normalize(); // Получаем корень репозитория
            Path indexPath = repositoryRoot.resolve(".gitler/index");
            Path objectsPath = repositoryRoot.resolve(".gitler/objects");

            if (!Files.exists(indexPath)) {
                System.out.println("Нет индексных файлов, репозиторий пуст.");
                return;
            }

            Map<String, String> indexEntries = readIndexEntries(indexPath);
            List<Path> workingDirectoryFiles = Files.walk(repositoryRoot)
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());

            checkForChangesToBeCommitted(indexEntries, repositoryRoot);

            checkForChangesNotStagedForCommit(indexEntries, workingDirectoryFiles, repositoryRoot);

            checkForUntrackedFiles(indexEntries, workingDirectoryFiles, repositoryRoot);

        } catch (IOException e) {
            System.err.println("Ошибка при чтении файлов репозитория: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Map<String, String> readIndexEntries(Path indexPath) throws IOException {
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
        if (lastCommitHash == null) {
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

        for (String lastCommitFilePath : lastCommitFileHashes.keySet()) {
//            System.err.println(indexEntries + " ::: " + lastCommitFilePath);
            if (!indexEntries.containsKey(lastCommitFilePath)) {
                System.out.println("deleted: " + "\033[31m" + lastCommitFilePath + "\033[0m");
            }
        }
    }

    private void checkForChangesNotStagedForCommit(Map<String, String> indexEntries, List<Path> workingDirectoryFiles, Path repositoryRoot) throws IOException {
        System.out.println("Changes not staged for commit:");

        for (Path file : workingDirectoryFiles) {
            if (Files.isRegularFile(file)) {
                String relativePath = repositoryRoot.relativize(file).toString().replace("\\", "/");
//                System.err.println("relativePath: " + relativePath);
                String fileHash = indexEntries.get(relativePath);
//                System.err.println("indexFileHash: " + fileHash);
                byte[] currentContent = Files.readAllBytes(file);
                String currentHash = SHA1.apply(currentContent);
//                System.err.println("current Hash: " + currentHash);

                if (fileHash != null && !fileHash.equals(currentHash)) {
                    System.out.println("modified: " + "\033[31m" + relativePath +"\033[0m");
                }
            }
        }
    }


    private void checkForUntrackedFiles(Map<String, String> indexEntries, List<Path> workingDirectoryFiles, Path repositoryRoot) {
        System.out.println("Untracked files:");

        for (Path file : workingDirectoryFiles) {
            if (Files.isRegularFile(file)) {
                String relativePath = repositoryRoot.relativize(file).toString().replace("\\", "/");
//                System.out.println(relativePath);
                if (!indexEntries.containsKey(relativePath)) {
                    System.out.println("\33[31m" + relativePath + "\33[0m");
                }
            }
        }
    }

}



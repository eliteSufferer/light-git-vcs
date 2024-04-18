package org.example.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CheckoutPossibility {
    private static Map<String, String> fileHashes;
    public static boolean allCommited() {
        try {
            Path repositoryRoot = Paths.get(".").toAbsolutePath().normalize(); // Получаем корень репозитория
            Path indexPath = repositoryRoot.resolve(".gitler/index");
            Map<String, String> indexEntries = readIndexEntries(indexPath);
            List<Path> workingDirectoryFiles = Files.walk(repositoryRoot)
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());

            if (checkForChangesToBeCommitted(indexEntries, repositoryRoot) &&
                    checkForChangesNotStagedForCommit(indexEntries, workingDirectoryFiles, repositoryRoot)){
                return true;
            }else{
                return false;
            }
        } catch (IOException e) {
//            System.err.println("Ошибка при чтении файлов репозитория: " + e.getMessage());
            e.printStackTrace();
            return false;

        }
    }

    public static Map<String, String> readIndexEntries(Path indexPath) throws IOException {
        return Files.readAllLines(indexPath).stream()
                .collect(Collectors.toMap(
                        line -> line.split(" ")[0].replace("\\", "/"),
                        line -> line.split(" ")[1].replace("\\", "/")
                ));

    }

    private static String getLastCommitHash(Path repositoryRoot) throws IOException {
        Path headPath = repositoryRoot.resolve(".gitler/HEAD");
        String refContent = Files.readString(headPath).trim();  // Пример: "ref: refs/heads/master"


        String branchPathStr = refContent.split(" ")[1];
        Path branchPath = repositoryRoot.resolve(Constants.VCS_DIR + branchPathStr);
        if (Files.exists(branchPath)) {
            return Files.readString(branchPath).trim();
        }

        return null;
    }
    private static String getTreeHashFromCommit(String commitHash, Path repositoryRoot) throws IOException {
        Path commitPath = getFilePath(commitHash, repositoryRoot);
        for (String line : Files.readAllLines(commitPath)) {
            if (line.startsWith("tree ")) {
                return line.split(" ")[1];
            }
        }
        return null;
    }



    private static Map<String, String> loadCommitTree(String commitHash, Path repositoryRoot) throws IOException {
        fileHashes = new HashMap<>();
        String treeHash = getTreeHashFromCommit(commitHash, repositoryRoot);
        loadTree(treeHash, repositoryRoot, "");
        return fileHashes;
    }

    private static void loadTree(String treeHash, Path repositoryRoot, String basePath) throws IOException {
        Path treePath = getFilePath(treeHash, repositoryRoot);
        if (!Files.exists(treePath)) return;

        List<String> treeContents = Files.readAllLines(treePath);
        for (String content : treeContents) {
            String[] parts = content.split(" ");
            if (parts[0].equals("blob")) {
                fileHashes.put(basePath + parts[2], parts[1]);
            } else if (parts[0].equals("tree")) {
                loadTree(parts[1], repositoryRoot, basePath + parts[2] + "/");
            }
        }
    }

    private static Path getFilePath(String hash, Path repositoryRoot) {
        return repositoryRoot.resolve(".gitler/objects").resolve(hash.substring(0, 2)).resolve(hash.substring(2));
    }



    public static boolean checkForChangesToBeCommitted(Map<String, String> indexEntries, Path repositoryRoot) throws IOException {
//        System.out.println("Changes to be committed:");
        String lastCommitHash = getLastCommitHash(repositoryRoot);
        if (lastCommitHash == null && Files.exists(Paths.get(Constants.INDEX_FILE))) {
            return false;

        }else if(lastCommitHash == null && !Files.exists(Paths.get(Constants.INDEX_FILE))){
            return true;
        }
//        if (lastCommitHash == null) {
//            System.out.println("No commits yet");
//            return true;
//        }
        Map<String, String> lastCommitFileHashes = loadCommitTree(lastCommitHash, repositoryRoot);

        for (Map.Entry<String, String> indexEntry : indexEntries.entrySet()) {
            String filePath = indexEntry.getKey();
            String indexFileHash = indexEntry.getValue();
            String lastCommitFileHash = lastCommitFileHashes.get(filePath);

            if (lastCommitFileHash == null) {
                return false;
//                System.out.println("new file: " + "\033[32m" + filePath + "\033[0m");
            } else if (!lastCommitFileHash.equals(indexFileHash)) {
                return false;
//                System.out.println("modified: " + "\033[32m" + filePath + "\033[0m");
            }
        }

        for (String lastCommitFilePath : lastCommitFileHashes.keySet()) {
            if (!indexEntries.containsKey(lastCommitFilePath)) {
                return false;
//                System.out.println("deleted: " + "\033[31m" + lastCommitFilePath + "\033[0m");
            }
        }
        return true;
    }

    public static boolean checkForChangesNotStagedForCommit(Map<String, String> indexEntries, List<Path> workingDirectoryFiles, Path repositoryRoot) throws IOException {
//        System.out.println("Changes not staged for commit:");

        for (Path file : workingDirectoryFiles) {
            if (Files.isRegularFile(file)) {
                String relativePath = repositoryRoot.relativize(file).toString().replace("\\", "/");
                String fileHash = indexEntries.get(relativePath);
                byte[] currentContent = Files.readAllBytes(file);
                String currentHash = SHA1.apply(currentContent);

                if (fileHash != null && !fileHash.equals(currentHash)) {
                    return false;
//                    System.out.println("modified: " + "\033[31m" + relativePath +"\033[0m");
                }
            }
        }
        return true;
    }



}

package org.example.commands;

import org.example.utils.Constants;
import org.example.utils.FlagParser;
import org.example.utils.RecursiveSearch;
import org.example.utils.RestoreIndex;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class Reset extends AbstractCommand{
    private Map<String, Boolean> options = new HashMap<>();
    public Reset() {
        super("reset", "reset commit");
        this.options.put("--soft", false);
        this.options.put("--mixed", false);
        this.options.put("--hard", false);
    }

    @Override
    public void execute(String[] commandArgument) throws IOException {
        Map<Boolean, Map<String, Object>> parsedData = FlagParser.parseFlags(options, commandArgument);
        Boolean key = parsedData.keySet().iterator().next();
        if (!key) {
            System.out.println("Некорректное использование комманды reset");
        }
        Map<String, String> flagsMap = (Map<String, String>) parsedData.get(key).get("flags");
        ArrayList<String> argPaths = (ArrayList<String>) parsedData.get(key).get("args");
        String commitHash = argPaths.get(0);
        if (flagsMap.containsKey("--soft")) {
            String previousHEAD = updateHEAD(commitHash);
            updateBranchCommit(commitHash);
            checkOrigHead(previousHEAD);
        } else if (flagsMap.containsKey("--hard")) {
            String previousHEAD = updateHEAD(commitHash);
            updateBranchCommit(commitHash);
            checkOrigHead(previousHEAD);
            resetIndex(commitHash);
            resetWorkingDirectory(commitHash);
        }else if(flagsMap.containsKey("--mixed") || flagsMap.isEmpty()){
            String previousHEAD = updateHEAD(commitHash);
            updateBranchCommit(commitHash);
            checkOrigHead(previousHEAD);
            resetIndex(commitHash);
        }

        System.out.println("Reset was successful.");
    }

    private void checkOrigHead(String previousHEAD) throws IOException {
        if (!Files.exists(Paths.get(Constants.ORIG_HEAD))){
            Files.writeString(Paths.get(Constants.ORIG_HEAD), previousHEAD, StandardOpenOption.TRUNCATE_EXISTING);
        }else{
            Files.writeString(Paths.get(Constants.ORIG_HEAD), previousHEAD);
        }
    }


    public String updateHEAD(String commitHash) throws IOException {
        String headHash = Files.readString(Paths.get(Constants.HEAD_FILE));
        Files.writeString(Paths.get(Constants.HEAD_FILE), commitHash, StandardOpenOption.TRUNCATE_EXISTING);
        return headHash;
    }
    public void updateBranchCommit(String commitHash) throws IOException {
        Files.writeString(Paths.get(Constants.REFS_HEADS + Branch.getCurrentBranchName()), commitHash);

    }

    public void resetIndex(String commitHash) throws IOException {
        RestoreIndex.restoreIndex(commitHash);
    }

    public static void resetWorkingDirectory(String commitHash) throws IOException {
        Path repositoryPath = RecursiveSearch.findRepositoryRoot(Paths.get(".").toAbsolutePath().normalize());
        Map<String, String> indexEntries = Status.readIndexEntries(Paths.get(Constants.INDEX_FILE));
        List<Path> fullWorkDir = CherryPick.getFilesFromDirectory(repositoryPath);
        Map<String, String> filesInWorkingDirToRemove = Stash.createSnapshotOfWorkingDirectory(indexEntries, fullWorkDir, repositoryPath);

        Set<Path> directoriesToDelete = new TreeSet<>(Comparator.comparingInt(path -> -path.getNameCount()));

        for (String filePath : filesInWorkingDirToRemove.keySet()) {
            Path fullFilePath = repositoryPath.resolve(filePath);
            try {
                Files.deleteIfExists(fullFilePath);
                System.out.println("Deleted file: " + fullFilePath);
                // Добавление всех родительских директорий в список удаления
                collectDirectoriesToDelete(fullFilePath.getParent(), repositoryPath, directoriesToDelete);
            } catch (IOException e) {
                System.err.println("Failed to delete file: " + fullFilePath + " due to " + e.getMessage());
            }
        }

        // Удаление всех собранных директорий в правильном порядке
        for (Path directory : directoriesToDelete) {
            try {
                Files.delete(directory);
                System.out.println("Deleted directory: " + directory);
            } catch (DirectoryNotEmptyException e) {
                System.out.println("Directory not empty, stopped deletion: " + directory);
                // Если директория не пуста, прекращаем попытки дальнейшего удаления
            } catch (IOException e) {
                System.err.println("Failed to delete directory: " + directory + " due to " + e.getMessage());
            }
        }





        File rootTree = null;
        for(String line : Files.readAllLines(Paths.get(Constants.OBJECTS_DIR + commitHash.substring(0, 2) + "/" + commitHash.substring(2)))){
            if (line.startsWith("tree")){

                String rootHash = line.split(" ")[1];
                rootTree = new File(Constants.OBJECTS_DIR + rootHash.substring(0, 2) + "/" + rootHash.substring(2));
                break;
            }
        }


        Checkout.restoreWorkingDir(repositoryPath, rootTree);
    }

    public static void collectDirectoriesToDelete(Path directory, Path rootPath, Set<Path> directoriesToDelete) {
        while (directory != null && !directory.equals(rootPath)) { // Исключаем корневую директорию
            if (directory.startsWith(rootPath)) {
                directoriesToDelete.add(directory);
                directory = directory.getParent(); // Поднимаемся на уровень выше
            } else {
                break; // Выходим, если вышли за пределы корневого пути
            }
        }
    }

}

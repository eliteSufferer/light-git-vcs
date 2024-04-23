package org.example.commands;

import difflib.DiffUtils;
import difflib.Patch;
import difflib.PatchFailedException;
import org.example.utils.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

public class CherryPick extends AbstractCommand{
    private Map<String, Boolean> options = new HashMap<>();
    public CherryPick(){
        super("cheery-pick", "pick target commit and place to current branch");
    }
    @Override
    public void execute(String[] commandArgument) throws IOException {
        this.options.put("--continue", false);
        this.options.put("--abort", false);
        Command addObject = new Add();
        Command stash = new Stash();
        Command commit = new Commit();





        Map<Boolean, Map<String, Object>> parsedData = FlagParser.parseFlags(options, commandArgument);
        Boolean key = parsedData.keySet().iterator().next();
        Path repositoryPath = RecursiveSearch.findRepositoryRoot(Paths.get(".").toAbsolutePath().normalize());
        if (!key) {
            System.out.println("Некорректное использование комманды cheery-pick");
            return;
        }




        Map<String, String> flagsMap = (Map<String, String>) parsedData.get(key).get("flags");
        ArrayList<String> argPaths = (ArrayList<String>) parsedData.get(key).get("args");

        if (flagsMap.containsKey("--continue") ){
            if(!Files.exists(Paths.get(Constants.MERGE_HEAD))){
                System.out.println("no cherry-pick in progress");
                return;
            }else {
                List<String> filePaths = Files.readAllLines(Paths.get(Constants.MERGE_HEAD)) ; // ваш список путей
                filePaths.remove(0);

                String[] addArgs = new String[filePaths.size() + 1];
                addArgs[0] = "add"; // команда
                for (int i = 0; i < filePaths.size(); i++) {
                    addArgs[i + 1] = filePaths.get(i); // добавляем пути файлов
                }
                Files.delete(Paths.get(Constants.MERGE_HEAD));
                addObject.execute(addArgs);
                createCommitForCherryPick();
                return;

            }
        } else if (flagsMap.containsKey("--abort")) {
            if(!Files.exists(Paths.get(Constants.MERGE_HEAD))){
                System.out.println("no cherry-pick in progress");
                return;
            }else {
                Files.delete(Paths.get(Constants.MERGE_HEAD));
                String[] stashArgs = new String[]{"stash", "pop"};
                stash.execute(stashArgs);
                return;
            }

        }

        // вызываем stash,чтобы сохранить состояние и возвращаем обратно
        if(!Files.exists(Paths.get(Constants.MERGE_HEAD))){
            String[] stashArgs = new String[]{"stash"};
            stash.execute(stashArgs);
            stashArgs = new String[]{"stash", "apply"};
            stash.execute(stashArgs);
        }else{
            System.out.println("Разрешите все конфликты или используется флаг --abort");
            return;
        }
        Files.writeString(Paths.get(Constants.MERGE_HEAD), argPaths.get(0));






        Map<String, CommitEntity> commits = SerializationUtil.deserialize(Constants.COMMITS);
        CommitEntity currentCommit = commits.get(Branch.getCurrentCommit()); // Получаем текущий коммит
        CommitEntity cherryPickCommit = commits.get(argPaths.get(0)); // Коммит для cherry-pick
        String baseCommitHash = Merge.findCommonAncestor(commits, cherryPickCommit.getCommitHash(), currentCommit.getCommitHash()); // Найдем базовый коммит
        CommitEntity baseCommit = commits.get(baseCommitHash);
        Map<String, String> workingDirectoryFiles = Stash.createSnapshotOfWorkingDirectory(Status.readIndexEntries(Paths.get(Constants.INDEX_FILE)), getFilesFromDirectory(repositoryPath), repositoryPath);

        Map<Boolean, Map<String, Boolean>> conflicts = checkForConflicts(baseCommit, cherryPickCommit, workingDirectoryFiles, repositoryPath);
        boolean hasConflicts = conflicts.keySet().iterator().next();
        //String filePath, List<String> baseContent, List<String> currentContent, List<String> cherryContent, Path repositoryRoot
        if (hasConflicts) {
            // Если есть конфликты, предоставляем пользователю возможность их решить
            for (String filePath : cherryPickCommit.getBlobs().keySet()) {
                // Если файл имеет конфликт
                String[] addArgs = new String[]{"add", filePath};
                if (conflicts.get(true).getOrDefault(filePath, false)) {
                    Path path = Paths.get(filePath);
                    // Создаём файл с метками конфликтов
                    createConflictFile(filePath,
                            extractFileContentFromCommit(baseCommit, path),
                            extractFileContentFromCommit(currentCommit, path),
                            extractFileContentFromCommit(cherryPickCommit, path),
                            repositoryPath);

                    try {
                        // Открываем файл для записи с дополнением (append)
                        BufferedWriter writer = Files.newBufferedWriter(Paths.get(Constants.MERGE_HEAD), StandardOpenOption.APPEND);

                        // Добавляем новую строку
                        writer.write(filePath);
                        writer.newLine(); // Добавляем символ новой строки
                        writer.close(); // Важно закрыть поток
                    } catch (IOException e) {
                        e.printStackTrace();
                    }



                } else {
                    // Если конфликтов нет, перезаписываем файл содержимым из cherry-pick коммита
                    Path fileToWrite = repositoryPath.resolve(filePath);
                    Files.createDirectories(fileToWrite.getParent());
                    String content = String.join("\n", Objects.requireNonNull(getFileContentByHash(cherryPickCommit.getBlobs().get(filePath))));
                    Files.writeString(fileToWrite, content, StandardCharsets.UTF_8);
                    addObject.execute(addArgs);
                }
            }

            // Действия пользователя через команды --continue или --abort
        } else {
            // Применяем изменения и создаём новый коммит
            applyChangesFromCommit(cherryPickCommit, repositoryPath, addObject);
            createCommitForCherryPick();
        }
    }

    public static List<Path> getFilesFromDirectory(Path repositoryRoot){
        try {
            return Files.walk(repositoryRoot)
                    .filter(Files::isRegularFile)
                    .toList();
        }catch (Exception e){
            System.err.println("getFilesFromDirectory ERROR: " + e.getMessage());
            e.printStackTrace();
            return null;
        }

    }

    private void createCommitForCherryPick() throws IOException {
        Command commit = new Commit();
        String commitMessage = Files.readAllLines(Paths.get(Constants.MERGE_HEAD)).get(0);
        String[] addArgs = new String[]{"commit", "-m", "(CHEERY-PICK) FROM: " + commitMessage};
        try {
            commit.execute(addArgs);
        }catch (IOException e){
            System.err.println("Ошибка при коммите в (CHEERY-PICK): " + e.getMessage());
            e.printStackTrace();
        }

    }



    public static Map<Boolean, Map<String, Boolean>> checkForConflicts(CommitEntity baseCommit, CommitEntity cherryPickCommit, Map<String, String> workingDirectoryContent, Path repositoryRoot) throws IOException {
        Map<String, Boolean> fileConflicts = new HashMap<>();
        boolean hasGlobalConflict = false;

        Map<String, String> baseFiles = baseCommit.getBlobs();
        Map<String, String> cherryFiles = cherryPickCommit.getBlobs();

        for (String filePath : cherryFiles.keySet()) {
            Path fullPath = repositoryRoot.resolve(filePath);
            boolean fileHasConflict = false;
            if (Files.exists(fullPath)) {
                List<String> baseContent = extractFileContentFromCommit(baseCommit, fullPath);
                List<String> currentContent = Arrays.stream(workingDirectoryContent.get(filePath).split("\n")).toList();
                List<String> cherryContent = extractFileContentFromCommit(cherryPickCommit, fullPath);
                Patch<String> patchFromBaseToCurrent = DiffUtils.diff(baseContent, currentContent);
                Patch<String> patchFromBaseToCherry = DiffUtils.diff(baseContent, cherryContent);



                try {
                    List<String> mergeResult = DiffUtils.patch(baseContent, patchFromBaseToCurrent);
                    mergeResult = DiffUtils.patch(mergeResult, patchFromBaseToCherry);
                } catch (PatchFailedException e) {
                    fileHasConflict = true;
                    hasGlobalConflict = true;
                }
            }
            fileConflicts.put(filePath, fileHasConflict);
        }

        return Map.of(hasGlobalConflict, fileConflicts);
    }


    public static List<String> extractFileContentFromCommit(CommitEntity commit, Path filePath) throws IOException {
        Map<String, String> blobs = commit.getBlobs(); // предполагается, что getBlobs() возвращает карту, где ключ - путь файла, а значение - хеш содержимого файла

        // Проверяем, содержит ли коммит указанный файл
//        System.out.println(blobs);
        Path repositoryPath = RecursiveSearch.findRepositoryRoot(Paths.get(".").toAbsolutePath().normalize());
//        System.out.println();
        String normFilePath = repositoryPath.relativize(filePath).toString().replace("\\", "/");

        if (blobs.containsKey(normFilePath)) {
            // Если файл есть в коммите, читаем его содержимое и возвращаем в виде списка строк

            String fileHash = blobs.get(normFilePath);
            System.out.println(fileHash);
            if (fileHash != null){
                List<String> fileContent = getFileContentByHash(fileHash);
                return fileContent;
            }
            return null;
        } else {
            // Если файла нет в коммите
            System.err.println("File not tracked in this commit: " + filePath);
            return Collections.emptyList(); // Возвращаем пустой список
        }
    }



    public static List<String> getFileContentByHash(String hash){
        try {

            Path filePath = Paths.get(Constants.OBJECTS_DIR + hash.substring(0, 2) + "/" + hash.substring(2));
            if (Files.exists(filePath)) {
                return Files.readAllLines(filePath);
            } else {
                return Collections.emptyList();
            }
        }catch (IOException e){
            System.err.println("Ошибка при чтнении файла: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }


    public void createConflictFile(String filePath, List<String> baseContent, List<String> currentContent, List<String> cherryContent, Path repositoryRoot) throws IOException {
        Path conflictFilePath = repositoryRoot.resolve(filePath);
        List<String> mergedContent = new ArrayList<>();
        boolean inConflict = false;

        for (int i = 0; i < baseContent.size(); i++) {
            String baseLine = baseContent.get(i);
            String currentLine = i < currentContent.size() ? currentContent.get(i) : "";
            String cherryLine = i < cherryContent.size() ? cherryContent.get(i) : "";

            if (!currentLine.equals(cherryLine)) {
                if (!inConflict) {
                    mergedContent.add("<<<<<<< HEAD");
                    inConflict = true;
                }
                mergedContent.add(currentLine);
            } else {
                if (inConflict) {
                    mergedContent.add("=======");
                    mergedContent.addAll(cherryContent.subList(Math.max(0, i - 1), i));
                    mergedContent.add(">>>>>>> REVERT");
                    inConflict = false;
                }
                mergedContent.add(baseLine);
            }
        }

        // Если файл закончился, но мы все еще в конфликте
        if (inConflict) {
            mergedContent.add("=======");
            mergedContent.addAll(cherryContent.subList(Math.max(0, baseContent.size() - 1), cherryContent.size()));
            mergedContent.add(">>>>>>> REVERT");
        }

        Files.createDirectories(conflictFilePath.getParent()); // Убедимся, что родительская директория существует
        Files.writeString(conflictFilePath, String.join("\n", mergedContent), StandardCharsets.UTF_8);
        System.out.println("Conflict in file: " + filePath + " has been written to " + conflictFilePath.toString());
    }

    public void applyChangesFromCommit(CommitEntity cherryPickCommit, Path repositoryRoot, Command Add) throws IOException {
        Map<String, String> fileContents = cherryPickCommit.getBlobs();

        for (Map.Entry<String, String> entry : fileContents.entrySet()) {
            Path filePath = repositoryRoot.resolve(entry.getKey());
            Files.createDirectories(filePath.getParent()); // Создаем все родительские директории, если они еще не существуют
            Files.writeString(filePath, String.join("\n", Objects.requireNonNull(getFileContentByHash(entry.getValue()))), StandardCharsets.UTF_8); // Записываем содержимое файла
            String[] addArgs = new String[]{"add", entry.getKey()};
            Add.execute(addArgs);
        }
    }



}

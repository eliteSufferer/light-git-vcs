package org.example.commands;

import difflib.Delta;
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

public class Revert extends AbstractCommand{
    private Map<String, Boolean> options = new HashMap<>();
    public Revert(){
        super("revert", "reverts the changes target commit");
        this.options.put("--continue", false);
        this.options.put("--abort", false);
        this.options.put("-m", true);
    }

    @Override
    public void execute(String[] commandArgument) throws IOException {
        Command addObject = new Add();
        Command stash = new Stash();



        Map<Boolean, Map<String, Object>> parsedData = FlagParser.parseFlags(options, commandArgument);
        Boolean key = parsedData.keySet().iterator().next();
        Path repositoryPath = RecursiveSearch.findRepositoryRoot(Paths.get(".").toAbsolutePath().normalize());
        if (!key) {
            System.out.println("Некорректное использование комманды cheery-pick");
            return;
        }


        ArrayList<String> argPaths = (ArrayList<String>) parsedData.get(true).get("args");
        Map<String, String> flagsMap = (Map<String, String>) parsedData.get(key).get("flags");

        Files.writeString(Paths.get(Constants.COMMIT_EDITMSG), flagsMap.getOrDefault("-m", ""), StandardOpenOption.TRUNCATE_EXISTING);


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
                createRevertCommit();
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


        String commitHash = argPaths.get(0);





        Map<String, CommitEntity> commits = SerializationUtil.deserialize(Constants.COMMITS);
        CommitEntity currentCommit = commits.get(Branch.getCurrentCommit());
        CommitEntity revertCommit = commits.get(commitHash);
        CommitEntity parentCommit = commits.get(revertCommit.getParents());

        Map<String, List<String>> revertChanges = calculateRevertChanges(revertCommit, parentCommit);
        applyRevertChanges(revertChanges, repositoryPath);
    }


    private List<String> calculateDifferences(List<String> currentLines, List<String> targetLines) {
        Patch<String> patch = DiffUtils.diff(currentLines, targetLines);
        List<String> differences = new ArrayList<>();

        for (Delta<String> delta : patch.getDeltas()) {
            List<String> deltaLines = delta.getOriginal().getLines(); // Получаем строки из оригинальной версии

            switch (delta.getType()) {
                case DELETE:
                    // Если строки были удалены в target, они должны быть также удалены из current для восстановления
                    differences.addAll(deltaLines);
                    break;
                case INSERT:
                    // INSERT обрабатывать не нужно, если мы ищем, что удалить из current, чтобы сделать его как target
                    break;
                case CHANGE:
                    // Для CHANGE добавляем строки из current, которые должны быть изменены на строки из target
                    differences.addAll(deltaLines);
                    break;
            }
        }
        return differences;
    }





    private Map<String, List<String>> calculateRevertChanges(CommitEntity revertCommit, CommitEntity parentCommit) throws IOException {
        Map<String, List<String>> revertChanges = new HashMap<>();
        Map<String, String> parentFiles = parentCommit.getBlobs();
        Map<String, String> revertFiles = revertCommit.getBlobs();

        // Обработка изменений файлов
        for (Map.Entry<String, String> entry : revertFiles.entrySet()) {
            String filePath = entry.getKey();
            List<String> revertFileContent = CherryPick.getFileContentByHash(entry.getValue());
            List<String> parentFileContent = parentFiles.containsKey(filePath)
                    ? CherryPick.getFileContentByHash(parentFiles.get(filePath))
                    : Collections.emptyList();

            List<String> differences = calculateDifferences(revertFileContent, parentFileContent);
            if (!differences.isEmpty()) {
                revertChanges.put(filePath, differences);
            }
        }

        // Обработка удаленных файлов в revert commit
        for (String parentFilePath : parentFiles.keySet()) {
            if (!revertFiles.containsKey(parentFilePath)) {
                revertChanges.put(parentFilePath, Collections.emptyList()); // Пометка файла к удалению
            }
        }

        return revertChanges;
    }



    private void applyRevertChanges(Map<String, List<String>> changes, Path repositoryRoot) throws IOException {
        boolean isConflict = true;
        List<String> toCommit = new ArrayList<>();
        for (Map.Entry<String, List<String>> change : changes.entrySet()) {
            Path filePath = repositoryRoot.resolve(change.getKey());
            List<String> targetContent = Files.exists(filePath) ? Files.readAllLines(filePath) : new ArrayList<>();
            List<String> contentToRemove = change.getValue();

            try {
                List<String> resultContent = new ArrayList<>(targetContent);
                resultContent.removeAll(contentToRemove);  // Попытка удалить строки
                Files.writeString(filePath, String.join("\n", resultContent), StandardCharsets.UTF_8);
                String[] addArgs = new String[] {"add", filePath.toString().replace("\\", "/")};
                toCommit.add(filePath.toString().replace("\\", "/"));
                Command addObject = new Add();
                addObject.execute(addArgs);
            } catch (Exception e) {
                // Если удаление строк не возможно без конфликта, создаем файл конфликта
                isConflict = false;
                BufferedWriter writer = Files.newBufferedWriter(Paths.get(Constants.MERGE_HEAD), StandardOpenOption.APPEND);

                // Добавляем новую строку
                writer.write(filePath.toString().replace("\\", "/"));
                writer.newLine(); // Добавляем символ новой строки
                writer.close(); // Важно закрыть поток

                createConflictFile(filePath, targetContent, contentToRemove, repositoryRoot);


            }
        }
        if (!isConflict){
            for (String filePath: toCommit){
                String[] addArgs = new String[] {"add", filePath};
                Command addObject = new Add();
                addObject.execute(addArgs);
            }
            createRevertCommit();
            Files.deleteIfExists(Paths.get(Constants.MERGE_HEAD));
        }
    }


    private void createRevertCommit() throws IOException {
        // Формирование сообщения коммита
        String revertMessage = "Revert: " + Files.readString(Paths.get(Constants.COMMIT_EDITMSG));

        // Выполнение команды коммита
        Command commitCommand = new Commit();
        String[] commitArgs = new String[]{"commit", "-m", revertMessage};
        commitCommand.execute(commitArgs);
    }

    public void createConflictFile(Path filePath, List<String> currentContent, List<String> revertedContent, Path repositoryRoot) throws IOException {
        StringBuilder conflictFileContent = new StringBuilder();

        int maxLines = Math.max(currentContent.size(), revertedContent.size());
        boolean inConflict = false;

        for (int i = 0; i < maxLines; i++) {
            String currentLine = i < currentContent.size() ? currentContent.get(i) : "";
            String revertedLine = i < revertedContent.size() ? revertedContent.get(i) : "";

            if (!currentLine.equals(revertedLine)) {
                if (!inConflict) {
                    conflictFileContent.append("<<<<<<< HEAD\n");
                    inConflict = true;
                }
                conflictFileContent.append(currentLine).append("\n");
            } else {
                if (inConflict) {
                    conflictFileContent.append("=======\n");
                    conflictFileContent.append(revertedLine).append("\n");
                    conflictFileContent.append(">>>>>>> REVERT\n");
                    inConflict = false;
                }
                conflictFileContent.append(currentLine).append("\n");
            }
        }

        if (inConflict) {
            conflictFileContent.append("=======\n");
            conflictFileContent.append(">>>>>>> REVERT\n");
        }

        Files.writeString(filePath, conflictFileContent.toString(), StandardCharsets.UTF_8);
        System.out.println("Conflict in file: " + filePath + " has been written to " + filePath.toString());
    }







}

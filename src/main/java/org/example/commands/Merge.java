package org.example.commands;

import org.example.utils.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Merge extends AbstractCommand{

    public Merge() {
        super("merge", "Merges 2 branches");
    }

    @Override
    public void execute(String[] commandArgument) throws IOException {
        if (commandArgument.length != 2){
            System.out.println("Использование: merge <branch_name>");
            return;
        }

        Map<String, CommitEntity> commits;

        commits = SerializationUtil.deserialize(Constants.COMMITS);


        String latestCommit = Branch.getCurrentCommit();

        String sourceCommit = new String(Files.readAllBytes(Paths.get(Constants.REFS_HEADS + commandArgument[1])));

        String commonAncestor = findCommonAncestor(commits, sourceCommit, latestCommit);

        assert commits != null;
        Map<String, String> mergedFiles = mergeFiles(commits, commonAncestor, sourceCommit, latestCommit);

        System.out.println(mergedFiles);

        boolean hasConflicts = false;


        for (String filePath : mergedFiles.keySet()) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(RecursiveSearch.findRepositoryRoot(Paths.get(".")) + "/" + filePath))) {
                if (mergedFiles.get(filePath).contains("<<<<<<<")) {
                    hasConflicts = true;
                    System.out.println("Конфликт слияния в файле: " + filePath + " Автоматическое слияние невозможно, решите конфликты вручную и сделайте коммит результата");
                }

                String[] lines = mergedFiles.get(filePath).replace("[", "").replace("]", "").split(",");
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (!hasConflicts){
            Commit commit = new Commit();
            Add add = new Add();
            add.execute(new String[]{"add", "."});
            commit.execute(new String[]{"commit", latestCommit, sourceCommit, "Merge with fixed conflicts"});

        }
    }

    private static Map<String, String> mergeFiles(Map<String, CommitEntity> commits, String commonAncestor, String sourceCommit, String latestCommit) {
        Map<String, String> mergedFiles = new HashMap<>();

        assert commits != null;
        CommitEntity ancestor = commits.get(commonAncestor);
        CommitEntity source = commits.get(sourceCommit);
        CommitEntity target = commits.get(latestCommit);

        Map<String, String> ancestorFiles = ancestor.getBlobs();
        Map<String, String> sourceFiles = source.getBlobs();
        Map<String, String> targetFiles = target.getBlobs();

        for (String filePath : sourceFiles.keySet()) {
            if (!targetFiles.containsKey(filePath)) {
                mergedFiles.put(filePath, ReadFile.readFile(Paths.get(Constants.OBJECTS_DIR + sourceFiles.get(filePath).substring(0, 2) + "/" + sourceFiles.get(filePath).substring(2))).toString());
            } else {
                String ancestorContent = ReadFile.readFile(Paths.get(Constants.OBJECTS_DIR + ancestorFiles.get(filePath).substring(0, 2) + "/" + ancestorFiles.get(filePath).substring(2))).toString();
                String targetContent = ReadFile.readFile(Paths.get(Constants.OBJECTS_DIR + targetFiles.get(filePath).substring(0, 2) + "/" + targetFiles.get(filePath).substring(2))).toString();
                String sourceContent = ReadFile.readFile(Paths.get(Constants.OBJECTS_DIR + sourceFiles.get(filePath).substring(0, 2) + "/" + sourceFiles.get(filePath).substring(2))).toString();

                if (sourceContent.equals(ancestorContent) && !targetContent.equals(ancestorContent)) {
                    mergedFiles.put(filePath, targetContent);
                    System.out.println("Сохранена версия из target");
                } else if (targetContent.equals(ancestorContent) && !sourceContent.equals(ancestorContent)) {
                    mergedFiles.put(filePath, sourceContent);
                    System.out.println("Сохранена версия из source");
                } else if (!sourceFiles.get(filePath).equals(targetFiles.get(filePath))) {
                    System.out.println("Пизда всему, начинаем мерж");
                    String mergedContent = performThreeWayMerge(ancestorContent, targetContent, sourceContent);
                    mergedFiles.put(filePath, mergedContent);
                }
            }
        }

        for (String filePath : targetFiles.keySet()) {
            if (!sourceFiles.containsKey(filePath)) {
                mergedFiles.put(filePath, ReadFile.readFile(Paths.get(Constants.OBJECTS_DIR + targetFiles.get(filePath).substring(0, 2) + "/" + targetFiles.get(filePath).substring(2))).toString());
            }
        }

        return mergedFiles;
    }


    private static String performThreeWayMerge(String ancestorContent, String targetContent, String sourceContent) {
        String[] ancestorLines = ancestorContent.split("\\r?\\n");
        String[] targetLines = targetContent.split("\\r?\\n");
        String[] sourceLines = sourceContent.split("\\r?\\n");

        List<String> mergedLines = new ArrayList<>();

        int ancestorIndex = 0;
        int targetIndex = 0;
        int sourceIndex = 0;

        while (ancestorIndex < ancestorLines.length || targetIndex < targetLines.length || sourceIndex < sourceLines.length) {
            String ancestorLine = (ancestorIndex < ancestorLines.length) ? ancestorLines[ancestorIndex] : null;
            String targetLine = (targetIndex < targetLines.length) ? targetLines[targetIndex] : null;
            String sourceLine = (sourceIndex < sourceLines.length) ? sourceLines[sourceIndex] : null;

            if (targetLine != null && sourceLine != null && targetLine.equals(sourceLine)) {
                mergedLines.add(targetLine);
                targetIndex++;
                sourceIndex++;
                if (ancestorLine != null && targetLine.equals(ancestorLine)) {
                    ancestorIndex++;
                }
            } else if (ancestorLine != null && targetLine != null && targetLine.equals(ancestorLine)) {
                mergedLines.add(sourceLine);
                sourceIndex++;
                ancestorIndex++;
            } else if (ancestorLine != null && sourceLine != null && sourceLine.equals(ancestorLine)) {
                mergedLines.add(targetLine);
                targetIndex++;
                ancestorIndex++;
            } else {
                // Конфликт: обе ветки внесли изменения в одну и ту же строку
                mergedLines.add("<<<<<<< Target");
                if (targetLine != null) {
                    mergedLines.add(targetLine);
                    targetIndex++;
                }
                mergedLines.add("=======");
                if (sourceLine != null) {
                    mergedLines.add(sourceLine);
                    sourceIndex++;
                }
                mergedLines.add(">>>>>>> Source");
            }
        }

        return String.join("\n", mergedLines);
    }



    public static String findCommonAncestor(Map<String, CommitEntity> commits, String sourceCommit, String currentCommit){
        List<String> sourceHistory = branchCommitsHistory(commits, sourceCommit); //ветка, КОТОРУЮ вливаем
        List<String> currentHistory = branchCommitsHistory(commits, currentCommit); //текущая ветка, то есть В КОТОРУЮ вливаем

        int i = sourceHistory.size() - 1;
        int j = currentHistory.size() - 1;

        String commonAncestor = null;

        while (i >= 0 && j >= 0) {
            if (sourceHistory.get(i).equals(currentHistory.get(j))) {
                commonAncestor = sourceHistory.get(i);
                i--;
                j--;
            } else {
                break;
            }
        }

        return commonAncestor;
    }



    public static List<String> branchCommitsHistory(Map<String, CommitEntity> commits, String commitHash){

        List<String> history = new ArrayList<>();

        assert commits != null;
        CommitEntity commit =  commits.get(commitHash);

        while (commit != null){
            history.add(commit.getCommitHash());
            String parentHash = commit.getParents();
            commit = commits.get(parentHash);
        }

        return history;

    }
}

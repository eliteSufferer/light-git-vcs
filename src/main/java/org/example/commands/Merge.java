package org.example.commands;

import org.example.utils.*;

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


        for (String filePath : mergedFiles.keySet()) {
            try (FileWriter writer = new FileWriter(RecursiveSearch.findRepositoryRoot(Paths.get(".")) + "/" + filePath)) {
                if (mergedFiles.get(filePath).contains("<<<<<<<")) {
                    System.out.println("Конфликт слияния в файле: " + filePath + " Автоматическое слияние невозможно, решите конфликты вручную и сделайте коммит результата");
                }
                writer.write(mergedFiles.get(filePath));

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //TODO: допилить до ума
    private static Map<String, String> mergeFiles(Map<String, CommitEntity> commits, String commonAncestor, String sourceCommit, String latestCommit){
        Map<String, String> mergedFiles = new HashMap<>();

        assert commits != null;
        CommitEntity ancestor = commits.get(commonAncestor);
        CommitEntity source = commits.get(sourceCommit);
        CommitEntity target = commits.get(latestCommit);

        Map<String, String> ancestorFiles = ancestor.getBlobs();
        Map<String, String> sourceFiles = source.getBlobs();
        Map<String, String> targetFiles = target.getBlobs();

        for (String filePath : sourceFiles.keySet()){
            if (!targetFiles.containsKey(filePath)) {
                mergedFiles.put(filePath, sourceFiles.get(filePath));
            } else if (!sourceFiles.get(filePath).equals(targetFiles.get(filePath))){
                String ancestorContent = ReadFile.readFile(Paths.get(Constants.OBJECTS_DIR + ancestorFiles.get(filePath).substring(0, 2) + "/" + ancestorFiles.get(filePath).substring(2))).toString();
                String targetContent = ReadFile.readFile(Paths.get(Constants.OBJECTS_DIR + targetFiles.get(filePath).substring(0, 2) + "/" + targetFiles.get(filePath).substring(2))).toString();
                String sourceContent = ReadFile.readFile(Paths.get(Constants.OBJECTS_DIR + sourceFiles.get(filePath).substring(0, 2) + "/" + sourceFiles.get(filePath).substring(2))).toString();

                String mergedContent = performThreeWayMerge(ancestorContent, targetContent, sourceContent);
                mergedFiles.put(filePath, mergedContent);

            }
        }

        for (String filePath : targetFiles.keySet()){
            if (!sourceFiles.containsKey(filePath)){
                mergedFiles.put(filePath, targetFiles.get(filePath));
            }


        }

        return mergedFiles;

    }


    private static String performThreeWayMerge(String ancestorContent, String targetContent, String sourceContent) {
        String[] ancestorLines = ancestorContent.split("\\r?\\n");
        String[] targetLines = targetContent.split("\\r?\\n");
        String[] sourceLines = sourceContent.split("\\r?\\n");

        StringBuilder mergedContent = new StringBuilder();

        int maxLength = Math.max(targetLines.length, sourceLines.length);
        for (int i = 0; i < maxLength; i++) {
            String ancestorLine = (i < ancestorLines.length) ? ancestorLines[i] : "";
            String targetLine = (i < targetLines.length) ? targetLines[i] : "";
            String sourceLine = (i < sourceLines.length) ? sourceLines[i] : "";

            if (targetLine.equals(sourceLine)) {
                mergedContent.append(targetLine).append("\n");
            } else if (targetLine.equals(ancestorLine)) {
                mergedContent.append(sourceLine).append("\n");
            } else if (sourceLine.equals(ancestorLine)) {
                mergedContent.append(targetLine).append("\n");
            } else {
                // Конфликт: обе ветки внесли изменения в одну и ту же строку
                mergedContent.append("<<<<<<< Target\n")
                        .append(targetLine).append("\n")
                        .append("=======\n")
                        .append(sourceLine).append("\n")
                        .append(">>>>>>> Source\n");
            }
        }

        return mergedContent.toString();
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

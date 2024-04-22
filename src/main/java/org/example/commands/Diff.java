package org.example.commands;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import org.example.utils.*;

import difflib.DiffUtils;
import difflib.Patch;

import javax.swing.text.html.parser.Entity;

public class Diff extends AbstractCommand {
    private Map<String, Boolean> options = new HashMap<>();

    public Diff() {
        super("diff", "Show file differences that haven't been staged");
        this.options.put("--cached", false);

    }

    @Override
    public void execute(String[] args) throws IOException {
        Map<Boolean, Map<String, Object>> parsedData = FlagParser.parseFlags(options, args);
        Path repositoryPath = RecursiveSearch.findRepositoryRoot(Paths.get(".")).toAbsolutePath().normalize();
        Boolean key = parsedData.keySet().iterator().next();
        if (!key) {
            System.out.println("Некорректное использование комманды commit");
            return;
        }
        Map<String, String> flagsMap = (Map<String, String>) parsedData.get(key).get("flags");
        ArrayList<String> commandArguments = (ArrayList<String>) parsedData.get(key).get("args");
//        if (!flagsMap.isEmpty() && !commandArguments.isEmpty()){
//            System.out.println("Некорректное использование комманды commit");
//            return;
//        }

        Path indexPath = repositoryPath.resolve(".gitler/index");

        // Считываем состояние индекса
        Map<String, String> indexedFiles = CheckoutPossibility.readIndexEntries(indexPath);

        if (flagsMap.containsKey("--cached")){
            if (!commandArguments.isEmpty()){
                for (String fileName : commandArguments){
                    if (!Files.exists(Paths.get(fileName))){
                        System.out.println("ambiguous argument '" + fileName + "': unknown revision or path not in the working tree.");
                        return;
                    }
                }
            }
            diffCached(repositoryPath, indexedFiles, commandArguments);
            return;
        } else if (commandArguments.size() == 2 && isCommit(commandArguments.get(0)) && isCommit(commandArguments.get(1))) {
            diffTwoCommits(commandArguments.get(0), commandArguments.get(1));
        }
        if (!commandArguments.isEmpty()){
            for (String fileName : commandArguments){
                if (!Files.exists(Paths.get(fileName))){
                    System.out.println("ambiguous argument '" + fileName + "': unknown revision or path not in the working tree.");
                    return;
                }
            }
        }
        diff(repositoryPath, indexedFiles, commandArguments);
        // Получаем список всех файлов в рабочем каталоге

    }
    private void diff(Path repositoryPath, Map<String, String> indexedFiles, List<String> filesForDiff) throws IOException {
        List<Path> files = new ArrayList<>();
        if (filesForDiff.isEmpty()){
            files = Files.walk(repositoryPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> !path.startsWith(repositoryPath.resolve(".gitler")))
                    .collect(Collectors.toList());
        }else{
            for (String filePath: filesForDiff){
                files.addAll(Files.walk(Paths.get(filePath))
                        .filter(Files::isRegularFile)
                        .filter(path -> !path.startsWith(repositoryPath.resolve(".gitler")))
                        .collect(Collectors.toList()));
            }
        }




        for (Path file : files) {
            file = file.toAbsolutePath().normalize();
            String relativePath = repositoryPath.relativize(file).toString().replace("\\", "/");
            if (indexedFiles.containsKey(relativePath)) {

                String indexFileHash = indexedFiles.get(relativePath);
                String indexedContent = new String(Files.readAllBytes(Paths.get(Constants.OBJECTS_DIR + indexFileHash.substring(0, 2) + "/" + indexFileHash.substring(2))), "UTF-8");
                String currentContent = new String(Files.readAllBytes(file), "UTF-8");

                if (!currentContent.equals(indexedContent)) {
                    List<String> original = indexedContent.lines().collect(Collectors.toList());
                    List<String> revised = currentContent.lines().collect(Collectors.toList());

                    Patch<String> patch = DiffUtils.diff(original, revised);
                    List<String> diff = DiffUtils.generateUnifiedDiff(relativePath, relativePath, original, patch, 0);

                    diff.forEach(System.out::println);
                }
            }
        }
    }
    private boolean isCommit(String commitHash){
        Map<String, CommitEntity> commits = (Map<String, CommitEntity>) SerializationUtil.deserialize(Constants.COMMITS);
        if (commits.containsKey(commitHash)){
            return true;
        }else{
            return false;
        }
    }

    private void diffTwoCommits(String firstCommitHash, String secondCommitHash) {
        try {
            Map<String, CommitEntity> commits = (Map<String, CommitEntity>) SerializationUtil.deserialize(Constants.COMMITS);
            Map<String, String> firstCommitFiles = getCommitFiles(commits, firstCommitHash, true);
            Map<String, String> secondCommitFiles = getCommitFiles(commits, secondCommitHash, true);

            // Check for changes and deletions
            for (String filePath : firstCommitFiles.keySet()) {
//                System.out.println("BU: " + firstCommitFiles.keySet());
                String firstFileHash = firstCommitFiles.get(filePath);
                String secondFileHash = secondCommitFiles.getOrDefault(filePath, "");
//                System.out.println("-=========");
//                System.out.println(secondFileHash);
//                System.out.println(filePath);
//                System.out.println("-=========");
                if (!secondCommitFiles.containsKey(filePath)){
                    System.out.println("File deleted: " + filePath);
                }
                else if (!firstFileHash.equals(secondFileHash)) {
                    String firstContent = new String(Files.readAllBytes(Paths.get(Constants.OBJECTS_DIR + firstFileHash.substring(0, 2) + "/" + firstFileHash.substring(2))), "UTF-8");
                    String secondContent = secondFileHash.isEmpty() ? "" : new String(Files.readAllBytes(Paths.get(Constants.OBJECTS_DIR + secondFileHash.substring(0, 2) + "/" + secondFileHash.substring(2))), "UTF-8");
                    List<String> original = firstContent.lines().collect(Collectors.toList());
                    List<String> revised = secondContent.lines().collect(Collectors.toList());
                    Patch<String> patch = DiffUtils.diff(original, revised);

                    if (!firstContent.equals(secondContent)) {
                        List<String> diff = DiffUtils.generateUnifiedDiff(filePath, filePath, original, patch, 0);
                        System.out.println("Diff for file: " + filePath);
                        diff.forEach(System.out::println);
                    }
                }
            }

            // Check for additions
            for (String filePath : secondCommitFiles.keySet()) {
                if (!firstCommitFiles.containsKey(filePath)) {
                    String secondFileHash = secondCommitFiles.get(filePath);
                    String secondContent = new String(Files.readAllBytes(Paths.get(Constants.OBJECTS_DIR + secondFileHash.substring(0, 2) + "/" + secondFileHash.substring(2))), "UTF-8");

                    System.out.println("File added: " + filePath);
                    System.out.println(secondContent);
                }
            }
        } catch (IOException e) {
            System.out.println("Error processing diff between commits: " + e.getMessage());
        }
    }

    private void diffCached(Path repositoryPath, Map<String, String> indexedFiles, List<String> commandArguments) throws IOException {

        String lastCommitHash = CheckoutPossibility.getLastCommitHash(repositoryPath);  // Получаем хеш последнего коммита
        Map<String, CommitEntity> commits = (HashMap<String, CommitEntity>) SerializationUtil.deserialize(Constants.COMMITS);

        Map<String, String> lastCommitFiles = getCommitFiles(commits, lastCommitHash, false);  // Получаем файлы из последнего коммита
        List<Path> allCommand = new ArrayList<>();
        if (!commandArguments.isEmpty()){

            for (String filePath: commandArguments){
                allCommand.addAll(Files.walk(Paths.get(filePath))
                        .filter(Files::isRegularFile)
                        .filter(path -> !path.startsWith(repositoryPath.resolve(".gitler")))
                        .collect(Collectors.toList()));
            }

        }

        for (Map.Entry<String, String> entry : indexedFiles.entrySet()) {
            String filePath;
            String fileHash;
            if (!commandArguments.isEmpty()){
                if (allCommand.contains(Paths.get(entry.getKey()))){
                    filePath = entry.getKey();
                    fileHash = entry.getValue();
                }else{
                    continue;
                }
            }else{
                filePath = entry.getKey();
                fileHash = entry.getValue();
            }

//            System.out.println("------");
//            System.out.println(lastCommitFiles);
//            System.out.println(filePath);
//            System.out.println(lastCommitFiles.containsKey(filePath));
//            System.out.println("------");
            if (lastCommitFiles.containsKey(filePath)) {
                String commitFileHash = lastCommitFiles.get(filePath);
                String indexedContent = new String(Files.readAllBytes(Paths.get(Constants.OBJECTS_DIR + fileHash.substring(0, 2) + "/" + fileHash.substring(2))), "UTF-8");
                String lastCommitContent = new String(Files.readAllBytes(Paths.get(Constants.OBJECTS_DIR + commitFileHash.substring(0, 2) + "/" + commitFileHash.substring(2))), "UTF-8");
//                System.out.println("L: " + lastCommitContent);
                if (!indexedContent.equals(lastCommitContent)) {
                    List<String> original = lastCommitContent.lines().collect(Collectors.toList());
                    List<String> revised = indexedContent.lines().collect(Collectors.toList());
                    Patch<String> patch = DiffUtils.diff(original, revised);
                    List<String> diff = DiffUtils.generateUnifiedDiff(filePath, filePath, original, patch, 0);
                    diff.forEach(System.out::println);
                }
            }



        }
    }
    public Map<String, String> getCommitFiles(Map<String, CommitEntity> commits, String lastCommitHash, boolean isBetweenTwoCommits){

        // Получаем файлы и их хеши из последнего коммита
        System.out.println(commits);
        Map<String, String> filesHashes = commits.get(lastCommitHash).getFilesHashes();
        System.out.println(filesHashes);
        if (isBetweenTwoCommits){
            return filesHashes.entrySet().stream()
                    .filter(entry -> !Files.isDirectory(Paths.get(entry.getKey())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        // Используем стрим для фильтрации и сборки результатов в карту
        return filesHashes.entrySet().stream()
                .filter(entry -> {
                    // Проверяем, существует ли файл в файловой системе
                    return Files.isRegularFile(Paths.get(entry.getKey()));
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}

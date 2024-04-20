package org.example.commands;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.example.utils.*;

import difflib.DiffUtils;
import difflib.Patch;

import javax.swing.text.html.parser.Entity;

public class Diff extends AbstractCommand {
    public Diff() {
        super("diff", "Show file differences that haven't been staged");
    }

    @Override
    public void execute(String[] args) throws IOException {
        Path repositoryPath = RecursiveSearch.findRepositoryRoot(Paths.get(".")).toAbsolutePath().normalize();
        if (args.length < 2){
            return;
        }

        Path indexPath = repositoryPath.resolve(".gitler/index");

        // Считываем состояние индекса
        Map<String, String> indexedFiles = CheckoutPossibility.readIndexEntries(indexPath);

        if (Objects.equals(args[1], "--cached")){
            diffCached(repositoryPath, indexedFiles);
            return;
        }
        // Получаем список всех файлов в рабочем каталоге
        List<Path> files = Files.walk(repositoryPath)
                .filter(Files::isRegularFile)
                .filter(path -> !path.startsWith(repositoryPath.resolve(".gitler")))
                .collect(Collectors.toList());

        for (Path file : files) {
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
    private void diffCached(Path repositoryPath, Map<String, String> indexedFiles) throws IOException {
        String lastCommitHash = CheckoutPossibility.getLastCommitHash(repositoryPath);  // Получаем хеш последнего коммита
        Map<String, CommitEntity> commits = (HashMap<String, CommitEntity>) SerializationUtil.deserialize(Constants.COMMITS);

        Map<String, String> lastCommitFiles = getCommitFiles(commits, lastCommitHash);  // Получаем файлы из последнего коммита

        for (Map.Entry<String, String> entry : indexedFiles.entrySet()) {
            String filePath = entry.getKey();
            String fileHash = entry.getValue();
            System.out.println("------");
            System.out.println(lastCommitFiles);
            System.out.println(filePath);
            System.out.println(lastCommitFiles.containsKey(filePath));
            System.out.println("------");
            if (lastCommitFiles.containsKey(filePath)) {
                String commitFileHash = lastCommitFiles.get(filePath);
                String indexedContent = new String(Files.readAllBytes(Paths.get(Constants.OBJECTS_DIR + fileHash.substring(0, 2) + "/" + fileHash.substring(2))), "UTF-8");
                String lastCommitContent = new String(Files.readAllBytes(Paths.get(Constants.OBJECTS_DIR + commitFileHash.substring(0, 2) + "/" + commitFileHash.substring(2))), "UTF-8");
                System.out.println("L: " + lastCommitContent);
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
    public Map<String, String> getCommitFiles(Map<String, CommitEntity> commits, String lastCommitHash){

        // Получаем файлы и их хеши из последнего коммита
        System.out.println(commits);
        Map<String, String> filesHashes = commits.get(lastCommitHash).getFilesHashes();
        System.out.println(filesHashes);

        // Используем стрим для фильтрации и сборки результатов в карту
        return filesHashes.entrySet().stream()
                .filter(entry -> {
                    // Проверяем, существует ли файл в файловой системе
                    return Files.isRegularFile(Paths.get(entry.getKey()));
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}

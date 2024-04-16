package org.example.commands;

import org.example.utils.FileEntry;
import org.example.utils.IndexParser;
import org.example.utils.SHA1;
import org.example.utils.TreeBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class Commit extends AbstractCommand {
    public Commit() {
        super("commit", "Save changes to the repository");
    }

    @Override
    public void execute(String[] commitMessage) throws IOException {
        Path repositoryPath = Paths.get("./");
        Path indexPath = repositoryPath.resolve(".gitler/index");
        Path objectsPath = repositoryPath.resolve(".gitler/objects");
        List<FileEntry> entries = IndexParser.parseIndex(indexPath);

        Map<String, String> trees = TreeBuilder.buildTrees(entries, objectsPath);

        String rootTreeHash = trees.get("");
        String commitContent = "tree " + rootTreeHash + "\nAuthor: " + Config.getUsername() + "\nmessage " + commitMessage[1];
        String commitHash = SHA1.apply(commitContent.getBytes());

        String dirName = commitHash.substring(0, 2);
        String fileName = commitHash.substring(2);
        Path commitDirectory = objectsPath.resolve(dirName);
        Path commitFile = commitDirectory.resolve(fileName);

        Files.createDirectories(commitDirectory);
        Files.writeString(commitFile, commitContent);
        Path headPath = Paths.get(repositoryPath.resolve(".gitler/HEAD").toUri());
        if (isFileEmpty(headPath)){
            Files.createFile(repositoryPath.resolve(".gitler/refs/master"));
            Files.writeString(repositoryPath.resolve(".gitler/HEAD"), "ref: refs/heads/master");
        }
        String branch = Files.readString(repositoryPath.resolve(".gitler/HEAD"));
        if (branch.split(" ").length > 1){
            branch = branch.split(" ")[1];
        } // TODO: Можно сделать для Detached HEAD
        Files.writeString(repositoryPath.resolve(".gitler/" + branch), commitHash);
        System.out.println("Commit created with hash: " + commitHash);
    }
    public static boolean isFileEmpty(Path filePath) {
        try {
            // Проверяем, существует ли файл и не является ли директорией
            if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                // Проверяем, равен ли размер файла 0
                return Files.size(filePath) == 0;
            }
        } catch (Exception e) {
            System.err.println("Произошла ошибка при проверке файла: " + e.getMessage());
        }
        return false;
    }
}
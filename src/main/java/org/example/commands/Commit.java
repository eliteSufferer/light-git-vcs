package org.example.commands;

import org.example.utils.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.spec.RSAOtherPrimeInfo;
import java.util.*;
import java.util.stream.Collectors;

public class Commit extends AbstractCommand {
    private Map<String, Boolean> options = new HashMap<>();

    public Commit() {
        super("commit", "Save changes to the repository");
        this.options.put("-m", true);
        this.options.put("--message", true);
        this.options.put("--all", false);
        this.options.put("-a", false);
        this.options.put("--amend", false);
        //TODO: --no-verify

    }

    @Override
    public void execute(String[] args) throws IOException {
        Map<Boolean, Map<String, Object>> parsedData = FlagParser.parseFlags(options, args);
        Boolean key = parsedData.keySet().iterator().next();
        if (!key) {
            System.out.println("Некорректное использование комманды commit");
        }
        Map<String, String> flagsMap = (Map<String, String>) parsedData.get(key).get("flags");
        ArrayList<String> argPaths = (ArrayList<String>) parsedData.get(key).get("args");


        Path repositoryPath = RecursiveSearch.findRepositoryRoot(Paths.get(".").toAbsolutePath().normalize());
//        System.out.println(repositoryPath);
        Path indexPath = repositoryPath.resolve(".gitler/index");
        Path objectsPath = repositoryPath.resolve(".gitler/objects");
        List<FileEntry> entries = IndexParser.parseIndex(indexPath);


        Map<String, String> indexEntries = CheckoutPossibility.readIndexEntries(indexPath);
        System.out.println("inf enties: " + indexEntries);
        List<Path> workingDirectoryFiles = Files.walk(repositoryPath)
                .filter(Files::isRegularFile)
                .collect(Collectors.toList());

        if ((flagsMap.containsKey("-a") || flagsMap.containsKey("--all")) && !CheckoutPossibility.checkForChangesNotStagedForCommit(indexEntries, workingDirectoryFiles, repositoryPath)) {

            Command command = new Add();
            String[] stringArray = new String[entries.size() + 1];
            stringArray[0] = "add";
            for (int i = 0; i < entries.size(); i++) {
                System.out.println("INDEX: " + entries.get(i) + "\n" + entries.get(i).getPath().toString());
                stringArray[i + 1] = entries.get(i).getPath().toString();
            }
            command.execute(stringArray);
            entries = IndexParser.parseIndex(indexPath);
        } else if (CheckoutPossibility.checkForChangesToBeCommitted(indexEntries, repositoryPath)) {
            System.out.println("Nothing to commit");
            return;

        }

        Map<String, Map<String, String>> buildResultTree = TreeBuilder.buildTrees(entries, objectsPath);

        Map<String, String> trees = buildResultTree.get("dir-hashes");
        Map<String, String> blobHashes = buildResultTree.get("blob-hashes");
        Map<String, String> allHashes = new HashMap<>();
        allHashes.putAll(trees);
        allHashes.putAll(blobHashes);

//                System.out.println(filesHashes.keySet());
//        for (Map.Entry<String, String> entry : trees.entrySet()){
//            System.out.println("ENT: " + entry);
//            String objectHash = entry.getValue();
//            for(String line : Files.readAllLines(Paths.get(Constants.OBJECTS_DIR + objectHash.substring(0, 2) + "/" + objectHash.substring(2)))){
//                filesHashes.put(line.split(" ")[0], line.split(" ")[1]);
//            }
//        }
//        System.out.println(trees);

        String rootTreeHash = trees.get("");
//        System.out.println(rootTreeHash);
        String commitContent = null;
        String commitMessage = getCommitMessage(flagsMap);
        String branch = Files.readString(Paths.get(Constants.HEAD_FILE));
        String parentCommit = null;
        if (!Files.exists(Paths.get(Constants.VCS_DIR + branch.split(" ")[1]))) {


            commitContent = "tree " + rootTreeHash + "\nAuthor: " + Config.getUsername() + "\nmessage " + commitMessage;
//            System.out.println(1);
        } else {
            String ref = Files.readString(Paths.get(repositoryPath.resolve(".gitler/HEAD").toUri()));
            parentCommit = Files.readString(Paths.get(repositoryPath.resolve(".gitler/" + ref.split(" ")[1]).toUri()));
            boolean isCommitWithUseAmend = false;
            if (flagsMap.containsKey("--amend")) {
//                Files.readString(Paths.get(repositoryPath.resolve(".gitler/" + ref.split(" ")[1]).toUri()));
                for (String line : Files.readAllLines(Paths.get(Constants.OBJECTS_DIR + parentCommit.substring(0, 2) + "/" + parentCommit.substring(2)))) {
                    if (!(flagsMap.containsKey("-m") || flagsMap.containsKey("--message"))) {
                        if (line.startsWith("message")) {
                            commitMessage = line.split(" ")[1];
                        }
                    }
                    if (line.startsWith("parent commit:")) {
                        System.out.println("LINE :" + line);
                        parentCommit = line.split(" ")[2];
                    }
                }
                if (Objects.equals(parentCommit, Files.readString(Paths.get(repositoryPath.resolve(".gitler/" + ref.split(" ")[1]).toUri())))) {
                    commitContent = "tree " + rootTreeHash + "\nAuthor: " + Config.getUsername() + "\nmessage " + commitMessage;
                    isCommitWithUseAmend = true;
                }

            }
            if (!isCommitWithUseAmend) {
                commitContent = "tree " + rootTreeHash + "\nAuthor: " + Config.getUsername() + "\nmessage " + commitMessage + "\nparent commit: " + parentCommit;
            }

//            System.out.println(2);
        }

        String commitHash = SHA1.apply(commitContent.getBytes());


        // Создаем новый объект CommitEntity
        Date commitDate = new Date(); // Текущее время для timestamp
        String ojbParentCommits = "";
        if (parentCommit != null) {
            ojbParentCommits = parentCommit;
        }

        CommitEntity newCommit = new CommitEntity(
                commitHash,
                rootTreeHash,
                ojbParentCommits,
                allHashes,
                commitMessage,
                commitDate,
                Config.getUsername(),
                blobHashes


        );
        Map<String, CommitEntity> commits;
        // Сериализация и десериализация коммитов
        if (!isFileEmpty(Paths.get(Constants.COMMITS))) {
            commits = SerializationUtil.deserialize(Constants.COMMITS);
            System.out.println("CUM: " + commits.toString());
            if (commits == null) {
                commits = new HashMap<>();
            }
            commits.put(commitHash, newCommit);
            SerializationUtil.serialize(commits, Constants.COMMITS);
        }else{
            commits = new HashMap<>();
            commits.put(commitHash, newCommit);
            System.out.println(commits);
            SerializationUtil.serialize(commits, Constants.COMMITS);

        }

        String dirName = commitHash.substring(0, 2);
        String fileName = commitHash.substring(2);
        Path commitDirectory = objectsPath.resolve(dirName);
        Path commitFile = commitDirectory.resolve(fileName);

        Files.createDirectories(commitDirectory);
        Files.writeString(commitFile, commitContent);
        Path headPath = Paths.get(repositoryPath.resolve(".gitler/HEAD").toUri());
//        if (isFileEmpty(headPath)){
//            Files.createFile(repositoryPath.resolve(".gitler/refs/heads/master"));
//            Files.writeString(repositoryPath.resolve(".gitler/HEAD"), "ref: refs/heads/master");
//        }

        if (branch.split(" ").length > 1) {
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

    public String getCommitMessage(Map<String, String> flagsMap) {
        String commitMessage = "";
        if (flagsMap.containsKey("-m")) {
            commitMessage = flagsMap.get("-m").replace("\"", "");
        } else if (flagsMap.containsKey("--message")) {
            commitMessage = flagsMap.get("--message").replace("\"", "");
        }
        return commitMessage;
    }
}

package org.example.commands;

import org.example.utils.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Show extends AbstractCommand{

    public Show() {
        super("show", "Shows details about object files");
    }

    @Override
    public void execute(String[] commandArgument) throws IOException {

        if (commandArgument.length < 2){
            System.out.println("Укажите, что хотите показать: хэш объекта, тег или ветку");
            return;
        }

        if (Files.exists(Path.of(Constants.REFS_TAGS + commandArgument[1]))){

            List<String> lines = ReadFile.readFile(Paths.get(Constants.REFS_TAGS + commandArgument[1]));

            assert lines != null;

            String[] tagMetadata = lines.get(0).split(" ");
            if (tagMetadata[0].equals("annotated")){
                Path tagObjectDirPath = Paths.get(Constants.OBJECTS_DIR + tagMetadata[2].substring(0, 2));
                File tagDir = new File(String.valueOf(tagObjectDirPath));

                File[] files = tagDir.listFiles();

                assert files != null;
                List<String> tagInfo = ReadFile.readFile(files[0].toPath());

                assert tagInfo != null;
                System.out.println("Тег: " + commandArgument[1]);
                for (String line : tagInfo){
                    System.out.println(line);
                }

            }
            showCommit(Objects.requireNonNull(ReadFile.readFile(Paths.get(Constants.REFS_HEADS + Branch.getCurrentBranchName()))).get(0));

        } else if (Files.exists(Paths.get(Constants.REFS_HEADS + commandArgument[1]))){
            String latestCommit = Objects.requireNonNull(ReadFile.readFile(Paths.get(Constants.REFS_HEADS + commandArgument[1]))).get(0);

            System.out.println("Последний коммит на ветке " + commandArgument[1] + ": " + latestCommit);
            showCommit(latestCommit);

        } else {
            if (Files.exists(Paths.get(Constants.OBJECTS_DIR + commandArgument[1].substring(0, 2) + "/" + commandArgument[1].substring(2)))){
                showCommit(commandArgument[1]);
            } else {
                System.out.println("Вы передали непонятно че в качестве аргумента, проверьте валидность");
            }
        }

    }

    private static void showCommit(String hash){
        Map<String, CommitEntity> commits;

        commits = SerializationUtil.deserialize(Constants.COMMITS);


        assert commits != null;
        for (CommitEntity commit : commits.values()){
            if (commit.getCommitHash().equals(hash)){
                System.out.println("Commit: " + commit.getCommitHash());
                System.out.println("Message: " + commit.getMessage());
                System.out.println("Timestamp: " + commit.getTimestamp());
                System.out.println("Author: " + commit.getAuthor());

                System.out.println("Файлы коммита: " + commit.getFilesHashes().keySet());

            } else if (commit.getTreeHash().equals(hash) || commit.getFilesHashes().containsValue(hash)){
                System.out.println("Содержимое файла коммита: ");
                System.out.println(ReadFile.readFile(Paths.get(Constants.OBJECTS_DIR + hash.substring(0,2) + "/" + hash.substring(2))));
            }
        }
    }
}

package org.example.commands;

import org.example.utils.Constants;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Tag extends AbstractCommand{
    public Tag() {
        super("tag", "Adds tags to objects");
    }

    @Override
    public void execute(String[] commandArgument) throws IOException {
        if (commandArgument.length == 1){
            listTags();
        } else {
            String tagName = commandArgument[1];
            String commitHash;
            if (commandArgument.length == 3){
                commitHash = commandArgument[2];
            } else {
                commitHash = new String(Files.readAllBytes(Paths.get(Constants.REFS_HEADS + Branch.getCurrentBranchName())));
            }
            File tagFile = new File(Constants.REFS_TAGS + tagName);

            if (tagFile.exists()) {
                System.out.println("Тег " + tagName + " уже существует.");
                return;
            }

            try (FileWriter writer = new FileWriter(tagFile)) {
                writer.write(commitHash);
                System.out.println("Тег " + tagName + " создан и указывает на коммит " + commitHash);
            } catch (IOException e){
                System.out.println(e.getMessage());
            }
        }

    }

    private static void listTags(){
        File tagsDir = new File(Constants.REFS_TAGS);
        String[] tags = tagsDir.list();

        assert tags != null;
        for (String tag : tags){
            System.out.println(tag);
        }
    }
}

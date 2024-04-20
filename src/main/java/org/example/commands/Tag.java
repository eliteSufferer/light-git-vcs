package org.example.commands;

import org.example.utils.Constants;
import org.example.utils.DateFormatter;
import org.example.utils.SHA1;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Objects;

public class Tag extends AbstractCommand{
    public Tag() {
        super("tag", "Adds tags to objects");
    }

    @Override
    public void execute(String[] commandArgument) throws IOException {
        if (commandArgument.length == 1){
            listTags();
        } else if (commandArgument[1].equals("-d")) {
            deleteTag(commandArgument[2]);
        } else {
            String commitHash;
            if (commandArgument.length == 3){
                commitHash = commandArgument[2];
                if (!objectFileExist(commitHash)){
                    System.out.println("Указан неверный хэш для тега, проверьте правильность");
                    return;
                }
            } else {
                commitHash = new String(Files.readAllBytes(Paths.get(Constants.REFS_HEADS + Branch.getCurrentBranchName())));
            }

            if (commandArgument.length == 5 && commandArgument[1].equals("-a") && commandArgument[3].equals("-m")){


                String tagContent = "Author: " + Config.getUsername() + "\nDate: " + DateFormatter.formDate() + "\nMessage: " + commandArgument[4];

                String tagObjectHash = SHA1.apply(tagContent.getBytes());

                if (!createTag("annotated", commandArgument[2], commitHash, tagObjectHash)){
                    System.out.println("Новый объект тега не будет создан");
                    return;
                }

                System.out.println(tagObjectHash);

                String tagObjDirPath = Constants.OBJECTS_DIR + tagObjectHash.substring(0, 2);


                String tagFilePath = tagObjDirPath + "/" + tagObjectHash.substring(2);

                Path path = Paths.get(tagObjDirPath);
                Path path1 = Paths.get(tagFilePath);

                if (Files.exists(path) || Files.exists(path1)){
                    System.out.println("Такой объект тега уже есть");
                    return;
                }



                Files.createDirectory(path);
                Files.createFile(path1);

                File tagFile = new File(tagFilePath);

                try {
                    FileWriter fileWriter = new FileWriter(tagFile);
                    fileWriter.write(tagContent);
                    fileWriter.close();
                    System.out.println("Файл с данными о теге успешно создан");
                } catch (IOException e) {
                    System.out.println("Ошибка создания файла тега: " + e.getMessage());
                }


            } else {
                createTag("lightweight", commandArgument[1], commitHash, "");

            }

        }

    }

    private static void deleteTag(String tagName) throws IOException {
        File tagFile = new File(Constants.REFS_TAGS + tagName);
        if (!tagFile.exists()) {
            System.out.println("Тег " + tagName + " и так не существует.");
            return;
        }


        Files.delete(tagFile.toPath());
        System.out.println("Тег " + tagName + " был успешно удален");
    }


    private static boolean createTag(String type, String tagName, String commitHash, String tagObjectHash){
        File tagFile = new File(Constants.REFS_TAGS + tagName);

        if (tagFile.exists()) {
            System.out.println("Тег " + tagName + " уже существует.");
            return false;
        }

        try (FileWriter writer = new FileWriter(tagFile)) {
            if (tagObjectHash.isEmpty()){
                writer.write(type + " " + commitHash);
            } else {
                writer.write(type + " " + commitHash + " " + tagObjectHash);
            }
            System.out.println("Тег " + tagName + " создан и указывает на объект с хэшем " + commitHash);
            return true;
        } catch (IOException e){
            System.out.println(e.getMessage());
        }
        return false;
    }

    private static boolean objectFileExist(String commitHash){
        File commitDir = new File(Constants.OBJECTS_DIR + commitHash.substring(0, 2));
        File commitFile = new File(commitDir.getPath() + "/" + commitHash.substring(2));

        return (commitDir.exists() && commitFile.exists());

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

package org.example.commands;
import org.example.utils.SHA1;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class Add extends AbstractCommand{
    public Add() {
        super("add", " Add file contents to the index");
    }


    @Override
    public void execute(String fileName) {
        System.out.println(Paths.get(fileName).toUri());
        File file = new File(Paths.get(fileName).toUri());
        if (file.isDirectory()) {

        }

        if (!file.exists()) {
            System.out.println("Файл" + fileName + " не найден: ");
            return;
        }

        try {
            // Чтение содержимого файла
            String fileContent = Files.readString(file.toPath());
            System.out.println("FileContent::: " + fileContent);

            // Вычисление хеша содержимого файла
            String hashCode = SHA1.apply(fileContent);
            Path indexPath = Paths.get(".gitler/index");
            Path objectsPath = Paths.get(".gitler/objects");
            File index;
            if (!Files.exists(indexPath)){
                index = new File(".gitler", "index");
                index.createNewFile();
            }
            // Создание файла кэша с именем, равным хешу содержимого
            File objDir = new File(".gitler/objects", hashCode.substring(0, 2));

            objDir.mkdir();
            File blobFile = new File(objDir, hashCode.substring(3));
            blobFile.createNewFile();
            Files.writeString(blobFile.toPath(), fileContent);
            Files.writeString(indexPath, hashCode);

        } catch (IOException e) {
            System.out.println("Ошибка при добавлении файла: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

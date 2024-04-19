package org.example.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ReadFile {

    public static List<String> readFile(Path path){
        try {
            return Files.readAllLines(path);
        } catch (IOException e) {
            System.out.println("Ошибка при чтении файла " + path);
        }
        return null;
    }
}

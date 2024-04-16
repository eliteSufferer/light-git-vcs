package org.example;

import org.example.commands.*;
import org.example.utils.RecursiveSearch;
import org.example.utils.SHA1;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length > 0) {
            String command = args[0];

            switch (command) {
                case "init":
                    Command init = new Init();
                    init.execute(args);
                    break;
                case "add":
                    Command add = new Add();
                    if (args.length > 1){
                        add.execute(args);
                    } else {
                        System.out.println("Укажите файл для добавления");
                    }
                    break;
                case "config":
                    Command config = new Config();
                    config.execute(args);
                    break;
                case "commit":
                    Command commit = new Commit();
                    commit.execute(args);
                    break;
                case "branch":
                    Command branch = new Branch();
                    branch.execute(args);
                    break;
                default:
                    System.out.println("Неизвестная команда" + command);
                    break;
            }
        } else {
            System.out.println("Narr, gib mir den Befehl");
        }
    }

}

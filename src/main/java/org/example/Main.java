package org.example;

import org.example.commands.*;
import org.example.utils.RecursiveSearch;
import org.example.utils.SHA1;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
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
                case "status":
                    Command status = new Status();
                    status.execute(args);
                    break;
                case "branch":
                    Command branch = new Branch();
                    branch.execute(args);
                    break;
                case "checkout":
                    Command checkout = new Checkout();
                    checkout.execute(args);
                    break;
                case "tag":
                    Command tag = new Tag();
                    tag.execute(args);
                    break;
                case "show":
                    Command show = new Show();
                    show.execute(args);
                    break;
                case "diff":
                    Command diff = new Diff();
                    diff.execute(args);
                    break;
                case "merge":
                    Command merge = new Merge();
                    merge.execute(args);
                    break;
                case "rm":
                    Command rm = new Rm();
                    rm.execute(args);
                    break;
                case "reset":
                    Command reset = new Reset();
                    reset.execute(args);
                    break;
                case "log":
                    Command log = new Log();
                    log.execute(args);
                    break;
                case "stash":
                    Command stash = new Stash();
                    stash.execute(args);
                    break;
                case "cherry-pick":
                    Command cheeryPick = new CherryPick();
                    cheeryPick.execute(args);
                    break;
                case "revert":
                    Command revert = new Revert();
                    revert.execute(args);
                    break;
                default:
                    System.out.println("Неизвестная команда " + command);
                    break;
            }
        } else {
            System.out.println("Narr, gib mir den Befehl");
        }
    }

}

package org.example;

import org.example.commands.Add;
import org.example.commands.Command;
import org.example.commands.Init;
import org.example.utils.RecursiveSearch;
import org.example.utils.SHA1;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main {
    private static final String VCS_DIR = ".gitler";
    public static void main(String[] args) throws IOException {
        /*Path current = Paths.get("").toAbsolutePath();
        if (RecursiveSearch.findRepositoryRoot(current) == null) {

            System.out.println("fatal: not a gitler repository (or any of the parent directories): .gitler");
            return;
        }*/
        Command init = new Init();
        Command add = new Add();
        init.execute("ebbebe");
        add.execute(".");
    }


}
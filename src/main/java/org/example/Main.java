package org.example;

import org.example.commands.Add;
import org.example.commands.Command;
import org.example.commands.Init;
import org.example.utils.SHA1;

import java.io.IOException;
import java.nio.file.Files;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main {
    public static void main(String[] args) throws IOException {
        Command init = new Init();
        Command add = new Add();
        init.execute("ebbebe");
        add.execute("bebebe");
    }
}
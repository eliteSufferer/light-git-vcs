package org.example.commands;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Init implements Command{

    @Override
    public void execute(String commandArgument) throws IOException {

        Path path = Paths.get("/fuck-git");
        Files.createDirectory(path);
    }
}

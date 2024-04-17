package org.example.commands;
import org.example.utils.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Init extends AbstractCommand {

    public Init() {
        super("init", "Инициализация репозитория gitler)))");
    }

    @Override
    public void execute(String[] arguments) {
        Path vcsDir = Paths.get(Constants.VCS_DIR);

        if (Files.exists(vcsDir)) {
            System.out.println("Репозиторий уже инициализирован.");
            return;
        }

        try {
            Files.createDirectory(vcsDir);

            Files.createDirectories(Paths.get(Constants.OBJECTS_DIR));
            Files.createDirectories(Paths.get(Constants.PACK));
            Files.createDirectories(Paths.get(Constants.OBJECTS_INFO));

            Files.createDirectories(Paths.get(Constants.REFS_DIR));
            Files.createDirectories(Paths.get(Constants.REFS_HEADS));
            Files.createDirectories(Paths.get(Constants.REFS_TAGS));

            Files.createDirectory(Paths.get(Constants.HOOKS));

            Files.createDirectory(Paths.get(Constants.VCS_DIR_INFO));



            Files.createFile(Paths.get(Constants.HEAD_FILE));
            Files.createFile(Paths.get(Constants.DESCRIPTION));
            Files.createFile(Paths.get(Constants.CONFIG_FILE));

            System.out.println("Репозиторий инициализирован.");
        } catch (IOException e) {
            System.out.println("Ошибка при инициализации репозитория: " + e.getMessage());
        }
    }
}

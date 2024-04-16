package org.example.commands;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Init extends AbstractCommand {
    private static final String VCS_DIR = ".gitler";
    private static final String OBJECTS_DIR = "objects";
    private static final String REFS_DIR = "refs";
    private static final String HEAD_FILE = "HEAD";
    private static final String CONFIG_FILE = "config";
    private static final String HOOKS = "hooks";
    private static final String INFO = "info";
    private static final String DESCRIPTION = "description";

    public Init() {
        super("init", "Инициализация репозитория gitler)))");
    }

    @Override
    public void execute(String arguments) {
        Path vcsDir = Paths.get(VCS_DIR);

        if (Files.exists(vcsDir)) {
            System.out.println("Репозиторий уже инициализирован.");
            return;
        }

        try {
            Files.createDirectory(vcsDir);

            Path objectsDir = Files.createDirectories(vcsDir.resolve(OBJECTS_DIR));
            Files.createDirectories(objectsDir.resolve("info"));
            Files.createDirectories(objectsDir.resolve("pack"));

            Path refsDir = Files.createDirectories(vcsDir.resolve(REFS_DIR));
            Files.createDirectories(refsDir.resolve("heads"));
            Files.createDirectories(refsDir.resolve("tags"));

            Files.createDirectory(vcsDir.resolve(HOOKS));
            Files.createDirectory(vcsDir.resolve(INFO));

            Files.createFile(vcsDir.resolve(HEAD_FILE));
            Files.createFile(vcsDir.resolve(DESCRIPTION));
            Files.createFile(vcsDir.resolve(CONFIG_FILE));

            System.out.println("Репозиторий инициализирован.");
        } catch (IOException e) {
            System.out.println("Ошибка при инициализации репозитория: " + e.getMessage());
        }
    }
}

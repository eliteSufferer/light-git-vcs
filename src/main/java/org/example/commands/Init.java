package org.example.commands;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Init extends AbstractCommand{
    ///HEAD  config  description  hooks/  info/  objects/  refs/
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
        File vcsDir = new File(VCS_DIR);

        if (vcsDir.exists()) {
            System.out.println("Репозиторий уже инициализирован.");
            return;
        }

        try {
            System.out.println(vcsDir.mkdir());

            File objectsDir = new File(vcsDir, OBJECTS_DIR);
            objectsDir.mkdir();
            new File(objectsDir, "info").mkdir();
            new File(objectsDir, "pack").mkdir();


            File refsDir = new File(vcsDir, REFS_DIR);
            refsDir.mkdir();
            new File(refsDir, "heads").mkdir();
            new File(refsDir, "tags").mkdir();

            new File(vcsDir, HOOKS).mkdir();
            new File(vcsDir, INFO).mkdir();


            File headFile = new File(vcsDir, HEAD_FILE);

            headFile.createNewFile();

            File descriptionFile = new File(vcsDir, DESCRIPTION);
            descriptionFile.createNewFile();

            File configFile = new File(vcsDir, CONFIG_FILE);
            configFile.createNewFile();

            System.out.println("Репозиторий инициализирован.");
        } catch (IOException e) {
            System.out.println("Ошибка при инициализации репозитория: " + e.getMessage());
        }
    }
}

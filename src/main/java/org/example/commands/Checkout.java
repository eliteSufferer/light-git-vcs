package org.example.commands;

import org.example.utils.CheckoutPossibility;
import org.example.utils.Constants;
import org.example.utils.RecursiveSearch;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Checkout extends AbstractCommand{

    public Checkout() {
        super("checkout", "Does checkouts");
    }

    @Override
    public void execute(String[] commandArgument) throws IOException {
        if (!CheckoutPossibility.allCommited()) {
            System.out.println("Есть незакоммиченые изменения, слейте все, потом делайте чекаут");
        } else if (commandArgument.length == 2) {
            String branchName = commandArgument[1];
            checkoutBranch(branchName);
        } else {
            System.out.println("Использование: checkout <имя_ветки>");
        }
    }

    private static String dirName(String hash){
        return hash.substring(0, 2);
    }

    private static List<String> readLinesFromFile(String path) throws FileNotFoundException {
        List<String>  lines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(path))){
            String line;
            while ((line = reader.readLine()) != null){
                lines.add(line);
            }
            return lines;
        } catch (IOException e) {
            System.out.println("Ошибка при чтении файла: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static void clearWorkingDir(Path directory) throws IOException {
        File[] files = directory.toFile().listFiles();
        if (files != null){
            for (File file : files){
                if (!file.getName().equals(".gitler")){
                    if (file.isDirectory()){
                        clearWorkingDir(file.toPath());
                    }

                    Files.delete(file.toPath());
                }
            }
        }
    }

    public static void copyFiles(File source, File dist){
        try {
            Path sourcePath = source.toPath();
            Path distPath = dist.toPath();

            byte[] content = Files.readAllBytes(sourcePath);

            Files.write(distPath, content);

            System.out.println("Содержимое файла " + dist + " успешно восстановлено");
        } catch (IOException e) {
            System.out.println("Ошибка при копировании файлов: " + e.getMessage());
        }
    }

    private static void restoreWorkingDir(Path root, File start) throws IOException {
        File base = new File(String.valueOf(start));
        File currentFile = Objects.requireNonNull(base.listFiles())[0];
        List<String> lines = readLinesFromFile(currentFile.getPath());

        for (String line : lines) {
            String[] parts = line.split(" ");
            String type = parts[0];
            String hash = parts[1];
            String name = parts[2];
            Path newPath = Path.of(root + "/" + name);

            if (type.equals("tree")) {
                Files.createDirectory(newPath);
                File subDir = new File(Constants.OBJECTS_DIR + dirName(hash));
                restoreWorkingDir(newPath, subDir);
            } else if (type.equals("blob")) {
                File newNode = Files.createFile(newPath).toFile();
                base = new File(Constants.OBJECTS_DIR + dirName(hash));
                currentFile = Objects.requireNonNull(base.listFiles())[0];
                copyFiles(currentFile, newNode);
            }
        }
    }

    private static void checkoutBranch(String branchName) {
        File branchFile = new File(Constants.REFS_HEADS + branchName);
        if (!branchFile.exists()) {
            System.out.println("Ветка " + branchName + " не существует.");
            return;
        }

        try {

            String commitHash = new String(Files.readAllBytes(Paths.get(branchFile.toURI())));
            System.out.println(commitHash);

            FileWriter writer = new FileWriter(Constants.HEAD_FILE);
            writer.write("ref: refs/heads/" + branchName);
            writer.close();

            // Обновление рабочей директории в соответствии с состоянием коммита

            File treeDir = new File(Constants.OBJECTS_DIR + dirName(commitHash));
            File treeFile = Objects.requireNonNull(treeDir.listFiles())[0];
            String treeRootHash = readLinesFromFile(treeFile.getPath()).get(0).split(" ")[1];

            File startMainDir = new File(Constants.OBJECTS_DIR + dirName(treeRootHash));

            System.out.println(startMainDir);
            Path repositoryRoot = RecursiveSearch.findRepositoryRoot(Paths.get(".").toAbsolutePath().normalize());

            assert repositoryRoot != null;
            clearWorkingDir(repositoryRoot);

            System.out.println(repositoryRoot);

            restoreWorkingDir(repositoryRoot, startMainDir);

            System.out.println("Переключено на ветку " + branchName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

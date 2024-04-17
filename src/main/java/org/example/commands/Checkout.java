package org.example.commands;

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
        if (commandArgument.length == 2) {
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
                if (!file.getName().equals(Constants.VCS_DIR)){
                    if (file.isDirectory()){
                        clearWorkingDir(file.toPath());
                    }

                    Files.delete(file.toPath());
                }
            }
        }
    }

    private static void restoreWorkingDir(Path root, File start) throws IOException {
        Path currentPath = Paths.get(start.getAbsolutePath());
        Path globalPath = root;
        while (Files.exists(currentPath)){
            File node = new File(String.valueOf(currentPath));
            File currentFile = Objects.requireNonNull(node.listFiles())[0];
            List<String> lines = readLinesFromFile(currentFile.getPath());

            for (String line : lines){
                Path newPath = Path.of(globalPath + "/" + line.split(" ")[2]);
                if (line.split(" ")[0].equals("tree")){
                    Files.createDirectory(newPath);
                } else if (line.split(" ")[0].equals("blob")){
                    Files.createFile(newPath);
                }
            }


        }
    }

    private static void checkoutBranch(String branchName) {
        File branchFile = new File(Constants.REFS_DIR + "/heads/" + branchName);
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

            File treeDir = new File(Constants.OBJECTS_DIR + "/" + dirName(commitHash)); //19
            File treeFile = Objects.requireNonNull(treeDir.listFiles())[0]; //9b...
            String treeRootHash = readLinesFromFile(treeFile.getPath()).get(0).split(" ")[1]; //561d01862b8daf7d31019d89f97ee61ee119259e

            File startMainDir = new File(Constants.OBJECTS_DIR + "/" + dirName(treeRootHash));
            Path repositoryRoot = RecursiveSearch.findRepositoryRoot(Paths.get(".").toAbsolutePath().normalize());

            assert repositoryRoot != null;
            clearWorkingDir(repositoryRoot);

            System.out.println(repositoryRoot);

            restoreWorkingDir(Path.of("/repo"), startMainDir);




            System.out.println("Переключено на ветку " + branchName);
        } catch (IOException e) {
            System.out.println("Ошибка при переключении на ветку: " + e.getMessage());
        }
    }
}
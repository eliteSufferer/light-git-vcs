package org.example.commands;

import org.example.utils.*;

import java.io.*;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static org.example.commands.Status.readIndexEntries;

public class Checkout extends AbstractCommand{

    private Map<String, Boolean> options = new HashMap<>();

    public Checkout() {
        super("checkout", "Does checkouts");
        this.options.put("-b", false);
        this.options.put("-f", false);

    }

    @Override
    public void execute(String[] commandArgument) throws IOException {
        Map<Boolean, Map<String, Object>> parsedData = FlagParser.parseFlags(options, commandArgument);
        Boolean key = parsedData.keySet().iterator().next();

        System.out.println(parsedData);
        if (!key) {
            System.out.println("Некорректное использование команды checkout");
        }
        Map<String, String> flagsMap = (Map<String, String>) parsedData.get(key).get("flags");
        ArrayList<String> args = (ArrayList<String>) parsedData.get(key).get("args");


        System.out.println(args.size());


        if (!CheckoutPossibility.allCommited() && !flagsMap.containsKey("-f")) {
            System.out.println("Есть незакоммиченые изменения, слейте все, потом делайте чекаут");
            return;
        } if (flagsMap.containsKey("-b")) {
            Branch.createBranch(args.get(0));
            checkoutBranch(args.get(0));
        } else if (args.size() == 1) {
            String branchName = args.get(0);
            checkoutBranch(branchName);
            String latestCommit = Objects.requireNonNull(ReadFile.readFile(Paths.get(Constants.REFS_HEADS + branchName))).get(0);
            RestoreIndex.restoreIndex(latestCommit);
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

    public static void restoreWorkingDir(Path root, File start) throws IOException {
        File base = new File(String.valueOf(start));
        List<String> lines = readLinesFromFile(base.getPath());

        for (String line : lines) {
            String[] parts = line.split(" ");
            String type = parts[0];
            String hash = parts[1];
            String name = parts[2];
            Path newPath = Path.of(root + "/" + name);

            if (type.equals("tree")) {
                Files.createDirectory(newPath);
                File subDir = new File(Constants.OBJECTS_DIR + dirName(hash) + "/" + hash.substring(2));
                restoreWorkingDir(newPath, subDir);
            } else if (type.equals("blob")) {
                File newNode = Files.createFile(newPath).toFile();
                base = new File(Constants.OBJECTS_DIR + dirName(hash));
                base = Objects.requireNonNull(base.listFiles())[0];
                copyFiles(base, newNode);
            }
        }
    }



    private static void checkoutBranch(String branchName) {
        File branchFile = new File(Constants.REFS_HEADS + branchName);
        if (!branchFile.exists()) {
            System.out.println("Ветка " + branchName + " не существует.");
            return;
        } else if (branchName.equals(Branch.getCurrentBranchName())){
            System.out.println("Вы и так в этой ветке");
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

            File startMainFile = new File(Constants.OBJECTS_DIR + dirName(treeRootHash) + "/" + treeRootHash.substring(2));

            System.out.println(startMainFile);
            Path repositoryRoot = RecursiveSearch.findRepositoryRoot(Paths.get(".").toAbsolutePath().normalize());

            assert repositoryRoot != null;
            clearWorkingDir(repositoryRoot);

            System.out.println(repositoryRoot);

            //restoreWorkingDir(repositoryRoot, startMainFile);

            Reset.resetWorkingDirectory(commitHash);

            System.out.println("Переключено на ветку " + branchName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

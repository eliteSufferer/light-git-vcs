package org.example.commands;

import org.example.utils.Constants;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class Branch extends AbstractCommand {

    public Branch() {
        super("branch", "Create new branch");
    }

    @Override
    public void execute(String[] commandArgument) throws IOException {
        if (commandArgument.length == 1) {
            listBranches();
        } else if (commandArgument.length == 2) {
            String branchName = commandArgument[1];
            createBranch(branchName);
        } else if (commandArgument[1].equals("-d")){
            String branchName = commandArgument[2];
            deleteBranch(branchName);
        } else if (commandArgument[1].equals("-m")){
            String oldBranchName = commandArgument[2];
            String newBranchName = commandArgument[3];
            renameBranch(oldBranchName, newBranchName);
        }
        else {
            System.out.println("Использование: branch [<имя_ветки>]");
        }
    }

    private static void listBranches() {
        File branchesDir = new File(Constants.REFS_HEADS);
        String[] branches = branchesDir.list();
        if (branches != null) {
            for (String branch : branches) {
                System.out.println(Objects.requireNonNull(getCurrentBranchPath()).split("/")[3].equals(branch) ? branch + "*" : branch);            }
        }
    }

    public static void createBranch(String branchName) {
        File branchFile = new File(Constants.REFS_HEADS + branchName);
        if (branchFile.exists()) {
            System.out.println("Ветка " + branchName + " уже существует.");
            return;
        }

        String currentCommit = getCurrentCommit();
        try {
            FileWriter writer = new FileWriter(branchFile);
            writer.write(currentCommit);
            writer.close();
            System.out.println("Ветка " + branchName + " создана.");
        } catch (IOException e) {
            System.out.println("Ошибка при создании ветки: " + e.getMessage());
        }
    }

    private static void deleteBranch(String branchName) throws IOException {
        File branchFile = new File(Constants.REFS_HEADS + branchName);
        if (!branchFile.exists()) {
            System.out.println("Ветка " + branchName + " и так не существует.");
            return;
        }


        if (Objects.requireNonNull(getCurrentBranchPath()).split("/")[3].equals(branchName)){
            System.out.println("Невозможно удалить ветку, вы в ней сидите. Сделайте чекаут и повторите попытку.");
            return;
        }

        Files.delete(branchFile.toPath());
        System.out.println("Ветка " + branchName + " была успешно удалена");
    }

    private static void renameBranch(String oldName, String newName){
        File branchFile = new File(Constants.REFS_HEADS + oldName);
        File newBranchFile = new File(Constants.REFS_HEADS + newName);
        if (!branchFile.exists()) {
            System.out.println("Ветка " + oldName + " не существует.");
            return;
        }

        if (Objects.requireNonNull(getCurrentBranchPath()).split("/")[3].equals(oldName)){
            String head = "ref: refs/heads/" + newName;

            try{
                Path headPath = Paths.get(Constants.HEAD_FILE);
                Files.write(headPath, head.getBytes());
                System.out.println("НEAD успешно перезаписан");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        branchFile.renameTo(newBranchFile);

        System.out.println("Ветка " + oldName + " успешно переименована на " + newName);

    }

    public static String getCurrentBranchPath() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(Constants.HEAD_FILE)));

            if (content.startsWith("ref: ")) {
                String refPath = content.split(" ")[1];
                return Constants.VCS_DIR + refPath;
            } else {
                return null;
            }
        } catch (IOException e) {
            System.out.println("Ошибка при чтении файла HEAD: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static String getCurrentBranchName(){
        return Objects.requireNonNull(getCurrentBranchPath()).split("/")[3];
    }

    public static String getCurrentCommit(){
        try{
            return new String(Files.readAllBytes(Paths.get(Objects.requireNonNull(getCurrentBranchPath()))));
        } catch (IOException e) {
            System.out.println("Ошибка при чтении файла коммита: " + e.getMessage());
            throw new RuntimeException();
        }
    }
}

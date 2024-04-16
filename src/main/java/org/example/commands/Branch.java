package org.example.commands;

import org.example.utils.Constants;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class Branch extends AbstractCommand {

    public Branch() {
        super("branch", "Create new branch");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 1) {
            listBranches();
        } else if (args.length == 2) {
            String branchName = args[1];
            createBranch(branchName);
        } else {
            System.out.println("Использование: branch [<имя_ветки>]");
        }
    }

    private static void listBranches() {
        File branchesDir = new File(Constants.REFS_DIR + "/heads");
        String[] branches = branchesDir.list();
        if (branches != null) {
            for (String branch : branches) {
                System.out.println(branch);
            }
        }
    }

    private static void createBranch(String branchName) {
        File branchFile = new File(Constants.REFS_DIR + "/heads/" + branchName);
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

    public static String getCurrentBranchPath() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(Constants.HEAD_FILE)));

            if (content.startsWith("ref: ")) {
                String refPath = content.split(" ")[1];
                return Constants.VCS_DIR + "/" + refPath;
            } else {
                return null;
            }
        } catch (IOException e) {
            System.out.println("Ошибка при чтении файла HEAD: " + e.getMessage());
            return null;
        }
    }

    private static String getCurrentCommit(){
        try{
            return new String(Files.readAllBytes(Paths.get(Objects.requireNonNull(getCurrentBranchPath()))));
        } catch (IOException e) {
            System.out.println("Ошибка при чтении файла коммита: " + e.getMessage());
            throw new RuntimeException();
        }
    }
}

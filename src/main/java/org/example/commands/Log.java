package org.example.commands;

import org.example.utils.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Log extends AbstractCommand {
    private Map<String, Boolean> options = new HashMap<>();
    private static final String COMMIT_GRAPH_NODE = "*";
    private static final String GRAPH_VERTICAL_LINE = "|";
    private static final String GRAPH_SPACE = " ";

    public Log() {
        super("log", "show commits history");
        this.options.put("--graph", false);

    }

    @Override
    public void execute(String[] commandArgument) throws IOException {
        Map<Boolean, Map<String, Object>> parsedData = FlagParser.parseFlags(options, commandArgument);
        Boolean key = parsedData.keySet().iterator().next();
        if (!key) {
            System.out.println("Некорректное использование комманды add");
        }
        Map<String, String> flagsMap = (Map<String, String>) parsedData.get(key).get("flags");
        ArrayList<String> argPaths = (ArrayList<String>) parsedData.get(key).get("args");

        Map<String, CommitEntity> commits = SerializationUtil.deserialize(Constants.COMMITS);


        if (!Files.readAllLines(Paths.get(Constants.HEAD_FILE)).get(0).startsWith("ref")) {
            System.out.println("you are in a detached head state. Try gitler log [branch name]");
            return;
        }

        String currentBranchHash;
        if (!argPaths.isEmpty()) {
            if (!Files.exists(Paths.get(Constants.REFS_HEADS + argPaths.get(0)))) {
                System.out.println();
                return;
            } else {
                currentBranchHash = Files.readString(Paths.get(Constants.REFS_HEADS + argPaths.get(0)));
            }

        } else {
            currentBranchHash = Branch.getCurrentCommit();
        }

        List<String> currentBranchHashes = Merge.branchCommitsHistory(commits, currentBranchHash);
        List<CommitEntity> sortedCommits = new ArrayList<>();
        for (Map.Entry<String, CommitEntity> entry : commits.entrySet()) {
            if (currentBranchHashes.contains(entry.getKey())) {
                sortedCommits.add(entry.getValue());
            }
        }

        if (flagsMap.containsKey("--graph")) {
            printGraph(commits, getLastCommitHashOnBranchByName(Branch.getCurrentBranchName()));
            return;
        }


        sortedCommits.sort((c1, c2) -> c2.getTimestamp().compareTo(c1.getTimestamp()));  // Сортировка по убыванию даты
        int i = 0;
        // Вывод отсортированного списка коммитов
        for (CommitEntity commit : sortedCommits) {
            if (i == 0) {
                String branchName;
                if (!argPaths.isEmpty()) {
                    branchName = argPaths.get(0);
                } else {
                    branchName = Branch.getCurrentBranchName();
                }
                System.out.println("\033[33mcommit " + commit.getCommitHash() + " (\033[0m\033[36mHEAD -> " + "\033[32m" + branchName + "\033[0m\033[33m)\033[0m");
            } else {
                System.out.println("\033[33mcommit " + commit.getCommitHash() + "\033[0m");
            }

            System.out.println("Author: " + commit.getAuthor());
            System.out.println("Date:   " + commit.getTimestamp());
            System.out.println();
            System.out.println("    " + commit.getMessage());
            System.out.println();
            i++;
        }
    }


    public void printGraph(Map<String, CommitEntity> commits, String branchName) throws IOException {
        List<CommitEntity> sortedCommits = getSortedCommitsOfBranch(commits, branchName);
        printCommits(sortedCommits);
    }
    private List<CommitEntity> getSortedCommitsOfBranch(Map<String, CommitEntity> commits, String branchName) throws IOException {
        // Предполагаем, что каждый CommitEntity знает, к какой ветке он принадлежит

        return commits.values().stream()
                .filter(commit -> commit.getBranchName().equals(branchName))
                .sorted(Comparator.comparing(CommitEntity::getTimestamp).reversed())
                .collect(Collectors.toList());
    }
    private void printCommits(List<CommitEntity> commits) {
        int maxPosition = commits.size() * 2;  // Умножаем на 2 для пространства между маркерами
        for (int i = 0; i < commits.size(); i++) {
            CommitEntity commit = commits.get(i);
            printCommitGraphLine(commit, i, maxPosition);
            printCommitDetails(commit);
        }
    }
    private void printCommitGraphLine(CommitEntity commit, int position, int maxPosition) {
        char[] line = new char[maxPosition];
        Arrays.fill(line, ' ');
        line[position * 2] = '*';
        System.out.println(new String(line));
    }
    private void printCommitDetails(CommitEntity commit) {
        System.out.println("Commit: " + commit.getCommitHash());
        System.out.println("Author: " + commit.getAuthor());
        System.out.println("Date:   " + commit.getTimestamp());
        System.out.println("Message: " + commit.getMessage());
        System.out.println();
    }











    private String getLastCommitHashOnBranchByName(String name) throws IOException {
        return Files.readString(Paths.get(Constants.REFS_HEADS + name));
    }


    // Этот метод строит и печатает граф коммитов
    // Получение всех веток в репозитории
    private Set<String> getBranches() throws IOException {
        Set<String> branches = new HashSet<>();
        Files.list(Paths.get(Constants.REFS_HEADS)).forEach(path -> branches.add(path.getFileName().toString()));
        return branches;
    }

    // Отображение строки графа для конкретного коммита
    // Печать строки графа для коммита









    private CommitEntity findCommitByHash(String hash, List<CommitEntity> allCommits) {
        for (CommitEntity commit : allCommits) {
            if (commit.getCommitHash().equals(hash)) {
                return commit;
            }
        }
        return null;
    }











}

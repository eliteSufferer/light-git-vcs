package org.example.commands;

import org.example.utils.FlagParser;
import org.example.utils.RecursiveSearch;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Rm extends AbstractCommand {
    private Map<String, Boolean> options = new HashMap<>();
    public Rm() {
        super("rm", "Remove files from the working tree and index");
        this.options.put("--cached", false);
        this.options.put("-r", false);
    }

    @Override
    public void execute(String[] args) throws IOException {
        Map<Boolean, Map<String, Object>> parsedData = FlagParser.parseFlags(options, args);
        Path repositoryPath = RecursiveSearch.findRepositoryRoot(Paths.get(".")).toAbsolutePath().normalize();
        Boolean key = parsedData.keySet().iterator().next();
        if (!key) {
            System.out.println("Incorrect usage of the 'rm' command");
            return;
        }
        Map<String, String> flagsMap = (Map<String, String>) parsedData.get(key).get("flags");
        ArrayList<String> commandArguments = (ArrayList<String>) parsedData.get(key).get("args");

        if (commandArguments.isEmpty()) {
            System.out.println("No files specified for removal.");
            return;
        }

        boolean cached = flagsMap.containsKey("--cached");
        boolean recursive = flagsMap.containsKey("-r");

        for (String fileArg : commandArguments) {
            Path filePath = repositoryPath.resolve(fileArg).normalize();
            if (Files.isDirectory(filePath) && !recursive) {
                System.out.println("Not removing '" + filePath + "' recursively without -r");
                continue;
            }
            removeFile(commandArguments, cached, recursive, repositoryPath);
        }
    }

      public static void removeFile(List<String> filesForDiff, boolean cached, boolean recursive, Path repositoryPath) throws IOException {
        List<Path> files = new ArrayList<>();
        for (String filePath: filesForDiff){
            files.addAll(Files.walk(Paths.get(filePath))
                    .filter(Files::isRegularFile)
                    .filter(path -> !path.startsWith(repositoryPath.resolve(".gitler")))
                    .collect(Collectors.toList()));
        }
        for (String file :filesForDiff){
            if (Files.isDirectory(Paths.get(file)) && !recursive){
                System.out.println("for delete directory use flag -r");
                return;
            }
        }
        if (!cached) {
            files.stream()
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            if (Files.isDirectory(p) && !recursive) {
                                System.out.println("Not removing '" + p + "' recursively without -r");
                                return; // Пропускаем директории, если не задан флаг -r
                            }
                            if (recursive || Files.isRegularFile(p)) {
                                Files.deleteIfExists(p);
                                System.out.println("Removed " + p);
                            }
                        } catch (IOException e) {
                            System.err.println("Failed to delete " + p + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    });

        }

        for (Path smallPath : files) {

            Add.removeFileFromIndex(smallPath, repositoryPath);
            System.out.println("removed from index: " + smallPath);
        }
        for (String file :filesForDiff){
            Path path = Paths.get(file);
            if (Files.isDirectory(path)){
                deleteDirectoryRecursively(path);

            }
        }
    }
    private static void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                for (Path entry : entries) {
                    deleteDirectoryRecursively(entry);
                }
            }
        }
        Files.delete(path);
    }
}

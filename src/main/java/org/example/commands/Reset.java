package org.example.commands;

import org.example.utils.Constants;
import org.example.utils.FlagParser;
import org.example.utils.RecursiveSearch;
import org.example.utils.RestoreIndex;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Reset extends AbstractCommand{
    private Map<String, Boolean> options = new HashMap<>();
    public Reset() {
        super("reset", "reset commit");
        this.options.put("--soft", false);
        this.options.put("--mixed", false);
        this.options.put("--hard", false);
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
        String commitHash = argPaths.get(0);
        if (flagsMap.containsKey("--soft")) {
            updateHEAD(commitHash);
        } else if (flagsMap.containsKey("--hard")) {
            updateHEAD(commitHash);
            resetIndex(commitHash);
            resetWorkingDirectory(commitHash);
        }else if(flagsMap.containsKey("--mixed") || flagsMap.isEmpty()){
            updateHEAD(commitHash);
            resetIndex(commitHash);
        }

        System.out.println("Reset was successful.");
    }



    public void updateHEAD(String commitHash) throws IOException {
        Files.writeString(Paths.get(Constants.HEAD_FILE), commitHash, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public void resetIndex(String commitHash) throws IOException {
        RestoreIndex.restoreIndex(commitHash);
    }

    public static void resetWorkingDirectory(String commitHash) throws IOException {
        Path repositoryRoot = RecursiveSearch.findRepositoryRoot(Paths.get(".").toAbsolutePath().normalize());
        File rootTree = null;
        for(String line : Files.readAllLines(Paths.get(Constants.OBJECTS_DIR + commitHash.substring(0, 2) + "/" + commitHash.substring(2)))){
            if (line.startsWith("tree")){

                String rootHash = line.split(" ")[1];
                rootTree = new File(Constants.OBJECTS_DIR + rootHash.substring(0, 2) + "/" + rootHash.substring(2));
                break;
            }
        }
        Checkout.restoreWorkingDir(repositoryRoot, rootTree);
    }
}

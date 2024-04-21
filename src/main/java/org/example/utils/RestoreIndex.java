package org.example.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Objects;

public class RestoreIndex {

    public static void restoreIndex(String hash) throws IOException {
        Map<String, CommitEntity> commits;

        commits = SerializationUtil.deserialize(Constants.COMMITS);



        assert commits != null;
        CommitEntity lastBranchCommit = commits.get(hash);

        Map<String, String> fileHashes = lastBranchCommit.getFilesHashes();

        File index = new File(Constants.INDEX_FILE);
        Files.writeString(index.toPath(), "");


        try (FileWriter writer = new FileWriter(index)) {
            for (Map.Entry<String, String> entry : fileHashes.entrySet()) {
                String filePath = entry.getKey().replace("/", "\\");
                String fileHash = entry.getValue();

                System.out.println("[" + filePath + " " + fileHash + "]");

                Path originalFile = Paths.get(RecursiveSearch.findRepositoryRoot(Paths.get(".")) + "/" + filePath);

                BasicFileAttributes data = Files.readAttributes(originalFile, BasicFileAttributes.class);

                if (!filePath.isEmpty() && !Files.isDirectory(originalFile)) {
                    writer.write(FormatIndexEntry.formatIndexEntry(filePath, fileHash, data));
                    //writer.write(System.lineSeparator());
                }
            }
        } catch (IOException e) {
            System.out.println("Ошибка при записи index файла");
            e.printStackTrace();
        }
    }
}

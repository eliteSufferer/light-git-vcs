package org.example.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RecursiveSearch {
    private static final String VCS_DIR = ".gitler";
    public static Path findRepositoryRoot(Path startPath) {
        Path current = startPath;
        while (current != null) {
            if (Files.exists(current.resolve(VCS_DIR))) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }
}

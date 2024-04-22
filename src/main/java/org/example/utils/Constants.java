package org.example.utils;

public class Constants {
    public static final String VCS_DIR = ".gitler/";
    public static final String COMMITS = VCS_DIR + "commits"; //TODO: Засунуть куда надо
    public static final String OBJECTS_DIR = VCS_DIR + "objects/";
    public static final String REFS_DIR = VCS_DIR + "refs/";
    public static final String HEAD_FILE = VCS_DIR + "HEAD";
    public static final String CONFIG_FILE = VCS_DIR + "config/";
    public static final String INDEX_FILE = VCS_DIR + "index";
    public static final String HOOKS = VCS_DIR + "hooks/";
    public static final String REFS_HEADS = REFS_DIR + "heads/";
    public static final String REFS_TAGS = REFS_DIR + "tags/";
    public static final String VCS_DIR_INFO = VCS_DIR + "info/";
    public static final String OBJECTS_INFO = OBJECTS_DIR + "info/";
    public static final String DESCRIPTION = VCS_DIR + "description/";
    public static final String PACK = OBJECTS_DIR + "pack/";
    public static final String STASH_DIR = REFS_DIR + "stash/";
    public static final String STASH_FILE = STASH_DIR + "stack";
    // Добавьте другие константы путей файлов по мере необходимости
}

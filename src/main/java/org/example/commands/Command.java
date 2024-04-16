package org.example.commands;

import java.io.IOException;

public interface Command {
    String getDescription();

    String getName();

    public void execute(String[] commandArgument) throws IOException;
}

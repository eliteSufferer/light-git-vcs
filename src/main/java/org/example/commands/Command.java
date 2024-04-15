package org.example.commands;

import java.io.IOException;

public interface Command {

    public void execute(String commandArgument) throws IOException;
}

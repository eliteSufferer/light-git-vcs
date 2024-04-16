package org.example.commands;

import org.example.utils.Constants;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Config extends AbstractCommand{

    public Config() {
        super("config", "Add user data");
    }



    @Override
    public void execute(String[] commandArgument) throws IOException {
        if (commandArgument.length != 3 || !commandArgument[1].equals("user.name")) {
            System.out.println("Использование: config user.name <имя>");
            return;
        }

        String username = commandArgument[2];
        Properties properties = new Properties();

        try {
            FileInputStream inputStream = new FileInputStream(Constants.CONFIG_FILE);
            properties.load(inputStream);
            inputStream.close();

            properties.setProperty("user.name", username);

            FileOutputStream outputStream = new FileOutputStream(Constants.CONFIG_FILE);
            properties.store(outputStream, null);
            outputStream.close();

            System.out.println("Имя пользователя установлено: " + username);
        } catch (IOException e) {
            System.out.println("Ошибка при сохранении настроек: " + e.getMessage());
        }
    }

    public static String getUsername() {
        Properties properties = new Properties();

        try {
            // Загрузка настроек из файла config
            FileInputStream inputStream = new FileInputStream(Constants.CONFIG_FILE);
            properties.load(inputStream);
            inputStream.close();

            return properties.getProperty("user.name");
        } catch (IOException e) {
            System.out.println("Ошибка при чтении настроек: " + e.getMessage());
            return null;
        }
    }
}

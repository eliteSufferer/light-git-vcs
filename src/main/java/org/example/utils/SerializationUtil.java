package org.example.utils;

import java.io.*;

public class SerializationUtil {

    // Метод для сериализации объекта и записи его в файл
    public static void serialize(Object obj, String filePath) {
        System.out.println("COMMIT PATH: " + filePath);
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(obj);
        } catch (IOException e) {
            System.err.println("Ошибка при сериализации: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Метод для десериализации объекта из файла
    public static <T> T deserialize(String filePath) {
        System.out.println("COMMIT PATH: " + filePath);
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            return (T) ois.readObject();
        } catch (FileNotFoundException e) {
            System.out.println("Файл не найден, будет создан новый.");
            return null;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Ошибка при десериализации: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}

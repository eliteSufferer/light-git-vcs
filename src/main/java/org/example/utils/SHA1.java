package org.example.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA1 {
    public static String apply(String text) {
        try {
            // Создание экземпляра MessageDigest для SHA-1
            MessageDigest md = MessageDigest.getInstance("SHA-1");

            // Обновление данных, которые нужно хешировать
            md.update(text.getBytes());

            // Вычисление хеша
            byte[] digest = md.digest();

            // Преобразование байтового массива хеша в строку в шестнадцатеричном формате
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // Обработка исключения, если алгоритм SHA-1 не поддерживается
            System.err.println("SHA-1 algorithm not found.");
        }
        return "";
    }
}

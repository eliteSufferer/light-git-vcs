package org.example.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlagParser {
    public static Map<Boolean, Map<String, Object>> parseFlags(Map<String, Boolean> availableFlags, String[] args) {
        String[] userArgs = new String[args.length - 1];
        System.arraycopy(args, 1, userArgs, 0, userArgs.length);


        Map<String, Object> parsedData = new HashMap<>();
        Map<String, String> parsedFlags = new HashMap<>();
        List<String> commandArgs = new ArrayList<>();

        boolean parsingFlags = true;  // Флаг, который определяет, продолжаем ли мы анализировать флаги или уже перешли к аргументам команды

        for (int i = 0; i < userArgs.length; i++) {  // Начинаем с 1, так как предполагаем, что userArgs[0] это название команды
            String arg = userArgs[i];

            if (parsingFlags && arg.startsWith("-")) {
                // Проверяем, есть ли флаг в списке доступных флагов
                if (availableFlags.containsKey(arg)) {
                    if (availableFlags.get(arg)) {
                        // Флаг требует аргумент
                        if (i + 1 < userArgs.length && !userArgs[i + 1].startsWith("-")) {
                            // Следующий аргумент существует и не является флагом
                            parsedFlags.put(arg, userArgs[i + 1]);
                            i++;  // Пропускаем следующий аргумент, так как он уже использован
                        } else {
                            // Аргумент отсутствует, хотя требуется
                            return Map.of(false, null);
                        }
                    } else {
                        // Флаг не требует аргумент
                        parsedFlags.put(arg, null);
                    }
                } else {
                    // Неизвестный флаг
                    return Map.of(false, null);
                }
            } else {
                parsingFlags = false;  // Перестаем обрабатывать флаги, начинаем считать аргументы команды
                commandArgs.add(arg);
            }
        }

        parsedData.put("flags", parsedFlags);
        parsedData.put("args", commandArgs);

        return Map.of(true, parsedData);
    }
}

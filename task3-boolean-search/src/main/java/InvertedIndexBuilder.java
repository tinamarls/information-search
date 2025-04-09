package main.java;

import java.util.*;

import java.io.IOException;


public class InvertedIndexBuilder {
    public static void main(String[] args) throws IOException {
        String inputDir = "task2-tokenizer/processed_tokens/lemmatized"; // Путь к папке с лемматизированными файлами
        String outputFile = "task3-boolean-search/inverted_index.txt";  // Путь для сохранения индекса

        // Чтение лемм из директории
        Map<String, List<String>> fileToLemmas = FileUtils.readLemmasFromDirectory(inputDir);
        Map<String, Set<String>> invertedIndex = new HashMap<>();

        // Строим индекс для первого слова в каждой строке
        for (Map.Entry<String, List<String>> entry : fileToLemmas.entrySet()) {
            String file = entry.getKey();  // Имя файла
            List<String> lemmas = entry.getValue();  // Список лемм из файла

            for (String lemma : lemmas) {
                // Берем только первое слово из каждой строки
                String firstWord = lemma.split("\\s+")[0];  // Разделяем строку на слова и берем первое

                // Добавляем это слово в инвертированный индекс
                invertedIndex.computeIfAbsent(firstWord, k -> new HashSet<>()).add(file);
            }
        }

        // Запись инвертированного индекса в файл
        FileUtils.writeIndexToFile(invertedIndex, outputFile);
        System.out.println("Индекс построен и сохранен в " + outputFile);
    }
}
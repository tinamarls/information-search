package main.java;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class FileUtils {
    public static Map<String, List<String>> readLemmasFromDirectory(String dirPath) throws IOException {
        Map<String, List<String>> fileToLemmas = new HashMap<>();
        Files.walk(Paths.get(dirPath))
                .filter(Files::isRegularFile)
                .forEach(filePath -> {
                    try {
                        List<String> lemmas = Files.readAllLines(filePath);
                        fileToLemmas.put(filePath.getFileName().toString(), lemmas);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        return fileToLemmas;
    }

    public static void writeIndexToFile(Map<String, Set<String>> index, String outputPath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            for (Map.Entry<String, Set<String>> entry : index.entrySet()) {
                writer.write(entry.getKey() + " " + entry.getValue() + "\n");
            }
        }
    }

    public static Map<String, Set<String>> readIndexFromFile(String indexPath) throws IOException {
        Map<String, Set<String>> index = new HashMap<>();
        List<String> lines = Files.readAllLines(Paths.get(indexPath));
        for (String line : lines) {
            String[] parts = line.split(" ", 2);
            if (parts.length == 2) {
                String lemma = parts[0];
                Set<String> files = new HashSet<>(Arrays.asList(
                        parts[1].replaceAll("[\\[\\]]", "").split(", ")));
                index.put(lemma, files);
            }
        }
        return index;
    }
}

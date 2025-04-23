package main.java;

import java.nio.file.*;
import java.util.*;

import java.io.IOException;

import java.io.*;
import java.util.stream.Collectors;

public class TfIdfCalculator {

    public static void main(String[] args) throws IOException {
        String tokensPath = "task2-tokenizer/processed_tokens/tokens";
        String lemmatizedPath = "task2-tokenizer/processed_tokens/lemmatized";
        String outputTokensPath = "task4-tf-idf/tf-idf-output/tokens";
        String outputLemmasPath = "task4-tf-idf/tf-idf-output/lemmas";

        // Создание директорий
        Files.createDirectories(Paths.get(outputTokensPath));
        Files.createDirectories(Paths.get(outputLemmasPath));

        Map<String, Integer> docFrequencies = new HashMap<>();
        Map<String, Integer> lemmaDocFrequencies = new HashMap<>();
        List<List<String>> allTokensDocs = new ArrayList<>();
        List<Map<String, List<String>>> allLemmasDocs = new ArrayList<>();

        File[] tokenFiles = new File(tokensPath).listFiles((dir, name) -> name.endsWith(".txt"));
        int docCount = tokenFiles.length;

        for (File tokenFile : tokenFiles) {
            List<String> tokens = Files.readAllLines(tokenFile.toPath()).stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            allTokensDocs.add(tokens);

            // Подсчет частот терминов
            Set<String> uniqueTerms = new HashSet<>(tokens);
            for (String term : uniqueTerms) {
                docFrequencies.put(term, docFrequencies.getOrDefault(term, 0) + 1);
            }

            // Поиск соответствующего файла лемм
            String fileName = tokenFile.getName().replace("tokens_", "lemmatized_");
            Path lemmaPath = Paths.get(lemmatizedPath, fileName);
            if (!Files.exists(lemmaPath)) {
                System.err.println("Lemmatized file not found for: " + tokenFile.getName());
                continue;
            }

            Map<String, List<String>> lemmaToTokens = new HashMap<>();
            for (String line : Files.readAllLines(lemmaPath)) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length > 1) {
                    String lemma = parts[0];
                    List<String> forms = Arrays.asList(Arrays.copyOfRange(parts, 1, parts.length));
                    lemmaToTokens.put(lemma, forms);
                }
            }

            allLemmasDocs.add(lemmaToTokens);

            for (String lemma : lemmaToTokens.keySet()) {
                lemmaDocFrequencies.put(lemma, lemmaDocFrequencies.getOrDefault(lemma, 0) + 1);
            }
        }

        for (int i = 0; i < allTokensDocs.size(); i++) {
            List<String> tokens = allTokensDocs.get(i);
            Map<String, Long> tokenFreq = tokens.stream()
                    .collect(Collectors.groupingBy(t -> t, Collectors.counting()));

            File tokenFile = tokenFiles[i];
            String outputTokenFile = outputTokensPath + "/" + tokenFile.getName();

            try (PrintWriter out = new PrintWriter(outputTokenFile)) {
                for (Map.Entry<String, Long> entry : tokenFreq.entrySet()) {
                    String term = entry.getKey();
                    long tf = entry.getValue();
                    int df = docFrequencies.get(term);
                    double idf = Math.log((double) docCount / df);
                    double tfidf = tf * idf;
                    out.printf("%s %.6f %.6f%n", term, idf, tfidf);
                }
            }

            // Леммы
            if (i < allLemmasDocs.size()) {
                Map<String, List<String>> lemmaToTokens = allLemmasDocs.get(i);
                Map<String, Integer> lemmaTf = new HashMap<>();

                for (Map.Entry<String, List<String>> entry : lemmaToTokens.entrySet()) {
                    String lemma = entry.getKey();
                    int tf = 0;
                    for (String token : entry.getValue()) {
                        tf += Collections.frequency(tokens, token);
                    }
                    lemmaTf.put(lemma, tf);
                }

                int totalTokens = tokens.size();
                File tokenFileLemmas = tokenFiles[i];
                String outputLemmaFile = outputLemmasPath + "/" + tokenFileLemmas.getName().replace("tokens_", "lemmas_");

                try (PrintWriter out = new PrintWriter(outputLemmaFile)) {
                    for (Map.Entry<String, Integer> entry : lemmaTf.entrySet()) {
                        String lemma = entry.getKey();
                        int tf = entry.getValue();
                        int df = lemmaDocFrequencies.get(lemma);
                        double idf = Math.log((double) docCount / df);
                        double tfidf = ((double) tf / totalTokens) * idf;
                        out.printf("%s %.6f %.6f%n", lemma, idf, tfidf);
                    }
                }
            }
        }

        System.out.println("TF-IDF файлы успешно созданы!");
    }
}
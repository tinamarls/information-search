package main.java;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BooleanSearch {
    private static final String INDEX_PATH = "task3-boolean-search/inverted_index.txt"; // Путь к инвертированному индексу
    private static final Set<String> OPERATORS = Set.of("and", "or", "not", "(", ")");

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        Map<String, Set<String>> index = loadIndex(); // Загружаем индекс из файла

        while (true) {
            System.out.print("Введите запрос (или 'exit'): ");
            String query = scanner.nextLine().toLowerCase(); // Считываем запрос
            if (query.equals("exit")) break; // Выход из программы

            try {
                Set<String> result = evaluateQuery(query, index); // Оценка запроса
                System.out.println("Найдено " + result.size() + " файлов: " + result); // Выводим результат
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Ошибка: " + e.getMessage()); // Обработка ошибок
            }
        }
    }

    // Загружаем инвертированный индекс из файла
    private static Map<String, Set<String>> loadIndex() throws IOException {
        Map<String, Set<String>> index = new HashMap<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(INDEX_PATH), StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            int spaceIndex = line.indexOf(" ");
            if (spaceIndex > 0) {
                String key = line.substring(0, spaceIndex); // Лемма
                String setString = line.substring(spaceIndex + 1).replaceAll("[\\[\\]]", ""); // Файлы
                Set<String> files = new HashSet<>(Arrays.asList(setString.split(", ")));
                index.put(key, files); // Добавляем в индекс
            }
        }
        reader.close();
        return index;
    }

    // Оценка запроса
    private static Set<String> evaluateQuery(String query, Map<String, Set<String>> index) {
        List<String> tokens = tokenize(query); // Разбиваем запрос на токены
        Stack<Set<String>> values = new Stack<>(); // Стек для операндов
        Stack<String> ops = new Stack<>(); // Стек для операторов

        for (String token : tokens) {

            switch (token) {
                case "(" -> {
                    ops.push(token); // Обработка открывающих скобок
                }
                case ")" -> {
                    // Обработка закрывающих скобок
                    while (!ops.isEmpty() && !ops.peek().equals("(")) {
                        applyOperator(values, ops.pop());
                    }
                    ops.pop(); // Убираем "("
                }
                case "and", "or", "not" -> {
                    // Обработка операторов и их приоритетов
                    while (!ops.isEmpty() && precedence(ops.peek()) >= precedence(token)) {
                        applyOperator(values, ops.pop());
                    }
                    ops.push(token); // Добавляем оператор в стек
                }
                default -> {
                    // Это лемма (слово), ищем в индексе
                    Set<String> val = index.getOrDefault(token, new HashSet<>());
                    if (val.isEmpty()) {
                        throw new IllegalArgumentException("No documents found for term: " + token); // Лемма не найдена в индексе
                    }
                    values.push(new HashSet<>(val)); // Добавляем результат в стек
                }
            }
        }

        // Применяем оставшиеся операторы
        while (!ops.isEmpty()) {
            applyOperator(values, ops.pop());
        }

        return values.isEmpty() ? Set.of() : values.pop(); // Возвращаем результат
    }

    // Применение оператора
    private static void applyOperator(Stack<Set<String>> values, String operator) {
        if (operator.equals("not")) {
            if (values.isEmpty()) {
                throw new IllegalArgumentException("Insufficient operands for 'not' operator.");
            }

            // Получаем лемму из стека
            Set<String> lemmaFiles = values.pop();

            // Получаем все файлы в папке lemmatized (по сути, полный список файлов)
            Set<String> allFiles = getAllFilesFromLemmatizedDirectory();

            // Если лемма не найдена, возвращаем все файлы
            if (lemmaFiles.isEmpty()) {
                values.push(allFiles);
            } else {
                // Убираем файлы, содержащие лемму
                Set<String> result = new HashSet<>(allFiles);
                result.removeAll(lemmaFiles);  // Убираем файлы, где встречается лемма
                values.push(result);     // Возвращаем только те файлы, где нет леммы
            }
        } else {
            // Для других операторов (and, or)
            if (values.size() < 2) {
                throw new IllegalArgumentException("Insufficient operands for operator: " + operator);
            }
            Set<String> right = values.pop(); // Правый операнд
            Set<String> left = values.pop(); // Левый операнд
            Set<String> result = new HashSet<>(left); // Инициализация результата
            if (operator.equals("and")) {
                result.retainAll(right); // Пересечение
            } else if (operator.equals("or")) {
                result.addAll(right); // Объединение
            }
            values.push(result);
        }
    }

    // Функция для получения всех файлов из директории lemmatized
    private static Set<String> getAllFilesFromLemmatizedDirectory() {
        Set<String> allFiles = new HashSet<>();
        // Замените путь на реальный путь к директории lemmatized
        File dir = new File("task2-tokenizer/processed_tokens/lemmatized");
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));
            if (files != null) {
                for (File file : files) {
                    allFiles.add(file.getName());
                }
            }
        }
        return allFiles;
    }

    // Разбивка запроса на токены
    private static List<String> tokenize(String query) {
        List<String> tokens = new ArrayList<>();
        // Регулярное выражение для кириллицы и латиницы (только леммы, операторы и скобки)
        Matcher matcher = Pattern.compile("[a-zA-Zа-яА-ЯёЁ]+|\\(|\\)|and|or|not").matcher(query.toLowerCase());

        while (matcher.find()) {
            String token = matcher.group();
            // Если токен является оператором (and, or, not, (, )), то добавляем его в стек операторов
            if (OPERATORS.contains(token)) {
                tokens.add(token); // Операторы добавляем в список токенов
            } else {
                // Если это не оператор, то это лемма
                tokens.add(token);
            }
        }
        return tokens;
    }

    // Возвращение приоритета оператора
    private static int precedence(String op) {
        return switch (op) {
            case "not" -> 3;
            case "and" -> 2;
            case "or" -> 1;
            default -> 0;
        };
    }

    // Получение всех файлов из стека
    private static Set<String> getAllFiles(Stack<Set<String>> stack) {
        Set<String> all = new HashSet<>();
        for (Set<String> s : stack) all.addAll(s); // Добавляем все файлы из стека
        return all;
    }
}
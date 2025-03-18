package crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class WebCrawler {
    private static final String BASE_URL = "https://lenta.ru";
    private static final int MAX_PAGES = 100;
    private static final String OUTPUT_DIR = "task1-crawler/downloaded_pages";

    public static void main(String[] args) {
        new File(OUTPUT_DIR).mkdirs();
        Set<String> visitedUrls = new HashSet<>();
        Queue<String> urlQueue = new LinkedList<>();
        urlQueue.add(BASE_URL);

        try (BufferedWriter indexWriter = new BufferedWriter(new FileWriter(OUTPUT_DIR + "/index.txt"))) {
            int fileCount = 1;

            while (!urlQueue.isEmpty() && fileCount <= MAX_PAGES) {
                String url = urlQueue.poll();
                if (visitedUrls.contains(url)) continue;
                try {
                    Document doc = Jsoup.connect(url).get();

                    // Удаляем все <script> и <link rel="stylesheet"> перед сохранением
                    doc.select("script, link[rel=stylesheet], style").remove();

                    String fileName = String.format("%s/выкачка_%d.html", OUTPUT_DIR, fileCount);

                    // Записываем страницу в текстовый файл (с HTML-разметкой)
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
                        writer.write(doc.html());
                    }

                    // Добавляем запись в index.txt (номер файла + ссылка)
                    indexWriter.write(fileCount + " " + url + "\n");
                    fileCount++;
                    visitedUrls.add(url);
                    System.out.println("Скачано: " + url);

                    // Извлекаем ссылки с текущей страницы и добавляем их в очередь
                    Elements links = doc.select("a[href]");
                    for (Element link : links) {
                        String nextUrl = link.absUrl("href"); // Преобразуем относительную ссылку в абсолютную

                        // Фильтруем ненужные файлы (css, js, изображения, pdf)
                        if (nextUrl.matches(".*\\.(css|js|png|jpg|jpeg|gif|pdf|svg)$")) {
                            continue;
                        }

                        if (nextUrl.startsWith(BASE_URL) && !visitedUrls.contains(nextUrl)) {
                            urlQueue.add(nextUrl); // Добавляем в очередь только HTML-страницы
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка при скачивании: " + url);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

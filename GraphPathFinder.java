package org.machi.javatest;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class GraphPathFinder {

    // Хранение графа в виде смежного списка
    private Map<Integer, List<Integer>> graph = new HashMap<>();

    // Метод для добавления ребра между двумя вершинами
    public void addEdge(int src, int dest) {
        // Добавляем ребро от src к dest
        graph.computeIfAbsent(src, k -> new ArrayList<>()).add(dest);
        // Добавляем обратное ребро для неориентированного графа (двусторонняя связь)
        graph.computeIfAbsent(dest, k -> new ArrayList<>()).add(src);
    }

    // Метод для записи графа в лог-файл
    public void logGraph(String filename) throws IOException {
        // Используем FileWriter для записи в файл
        try (FileWriter writer = new FileWriter(filename)) {
            // Проходим по всем вершинам графа
            for (int node : graph.keySet()) {
                // Записываем вершину и ее соседей в файл
                writer.write(node + " -> " + graph.get(node) + "\n");
            }
        }
    }

    // Метод для поиска кратчайшего пути от start до end
    public List<Integer> findShortestPath(int start, int end) {
        // Очередь для хранения путей, которые нужно исследовать
        Queue<List<Integer>> queue = new LinkedList<>();
        // Множество для отслеживания посещенных вершин
        Set<Integer> visited = new HashSet<>();

        // Добавляем стартовую вершину в очередь как начальный путь
        queue.add(Arrays.asList(start));
        // Помечаем стартовую вершину как посещенную
        visited.add(start);

        // Пока очередь не пуста
        while (!queue.isEmpty()) {
            // Извлекаем первый путь из очереди
            List<Integer> path = queue.poll();
            // Получаем последнюю вершину в текущем пути
            int node = path.get(path.size() - 1);

            // Если достигли конечной вершины, возвращаем путь
            if (node == end) {
                return path;
            }

            // Проходим по соседям текущей вершины
            for (int neighbor : graph.getOrDefault(node, Collections.emptyList())) {
                // Если соседняя вершина еще не была посещена
                if (!visited.contains(neighbor)) {
                    // Помечаем ее как посещенную
                    visited.add(neighbor);
                    // Создаем новый путь, добавляя соседа к текущему пути
                    List<Integer> newPath = new ArrayList<>(path);
                    newPath.add(neighbor);
                    // Добавляем новый путь в очередь
                    queue.add(newPath);
                }
            }
        }

        // Если путь не найден, возвращаем null
        return null;
    }

    public static void main(String[] args) {
        // Создаем экземпляр графа
        GraphPathFinder graph = new GraphPathFinder();

        // Добавляем ребра в граф на основе приведенных данных
        graph.addEdge(1, 2);
        graph.addEdge(2, 3);
        graph.addEdge(2, 4);
        graph.addEdge(2, 6);
        graph.addEdge(3, 4);
        graph.addEdge(3, 5);
        graph.addEdge(4, 5);
        graph.addEdge(4, 6);
        graph.addEdge(5, 6);

        try {
            // Логируем структуру графа в файл graph.log
            graph.logGraph("graph.log");

            // Находим и выводим кратчайший путь от вершины 1 к вершине 6
            List<Integer> path = graph.findShortestPath(1, 6);
            if (path != null) {
                // Если путь найден, выводим его в консоль
                System.out.println("Кратчайший путь: " + path);
            } else {
                // Если пути нет, выводим сообщение о его отсутствии
                System.out.println("Путь не найден.");
            }
        } catch (IOException e) {
            // Обрабатываем возможные ошибки при записи в файл
            System.err.println("Ошибка записи в лог-файл: " + e.getMessage());
        }
    }
}

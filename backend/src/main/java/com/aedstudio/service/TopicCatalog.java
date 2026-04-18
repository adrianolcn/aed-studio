package com.aedstudio.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TopicCatalog {

    public static final int COMPLETION_THRESHOLD_PERCENT = 70;

    public record TrackDefinition(
            String id,
            String name,
            String description,
            int orderIndex
    ) {}

    public record TopicDefinition(
            String id,
            String title,
            String description,
            String trackId,
            String path,
            int orderIndex,
            List<String> prerequisites
    ) {}

    public record ExerciseDefinition(
            String id,
            String topicId,
            String type,
            String prompt,
            List<String> options,
            String correctAnswer,
            String correctFeedback,
            String wrongFeedback,
            boolean required,
            int xp
    ) {}

    private static final List<TrackDefinition> TRACKS = List.of(
            new TrackDefinition("fundamentos", "Fundamentos", "Base conceitual para raciocinar sobre algoritmos, abstração e análise.", 1),
            new TrackDefinition("lineares", "Estruturas Lineares", "Estruturas sequenciais, acesso, memória e operações fundamentais.", 2),
            new TrackDefinition("nao-lineares", "Estruturas Não-Lineares", "Árvores, heaps e tabelas para organizar relações mais complexas.", 3),
            new TrackDefinition("ordenacao", "Algoritmos de Ordenação", "Estratégias de ordenação e comparação de custos.", 4),
            new TrackDefinition("grafos", "Algoritmos em Grafos", "Modelagem de relações, travessias e caminhos mínimos.", 5),
            new TrackDefinition("paradigmas", "Paradigmas", "Padrões de projeto algorítmico para problemas avançados.", 6)
    );

    private static final List<TopicDefinition> TOPICS = List.of(
            topic("algoritmos", "O que é um Algoritmo?", "Entenda algoritmo como procedimento finito, correto e analisável.", "fundamentos", "fundamentos/o-que-e-algoritmo", 1),
            topic("tad", "Tipo Abstrato de Dado", "Separe contrato de implementação e entenda interfaces de dados.", "fundamentos", "fundamentos/tad", 2, "algoritmos"),
            topic("notacao", "Notação Assintótica", "Compare crescimento de funções e custos de execução.", "fundamentos", "fundamentos/notacao", 3, "algoritmos"),
            topic("analise", "Análise de Algoritmos", "Estime custo temporal e espacial em cenários reais.", "fundamentos", "fundamentos/analise", 4, "notacao"),
            topic("arrays", "Arrays e Listas", "Use memória contígua e listas dinâmicas com consciência de custo.", "lineares", "lineares/arrays", 5, "tad"),
            topic("pilhas", "Pilhas e Filas", "Modele ordem de processamento LIFO e FIFO.", "lineares", "lineares/pilhas-filas", 6, "arrays"),
            topic("ll", "Listas Ligadas", "Entenda ponteiros, nós e custo de inserção/remoção.", "lineares", "lineares/listas-ligadas", 7, "pilhas"),
            topic("bst", "Árvores BST", "Organize chaves em hierarquias ordenadas.", "nao-lineares", "nao-lineares/bst", 8, "ll"),
            topic("avl", "Árvores AVL", "Mantenha árvores balanceadas para preservar O(log n).", "nao-lineares", "nao-lineares/avl", 9, "bst"),
            topic("heap", "Heaps", "Priorize elementos com invariantes locais.", "nao-lineares", "nao-lineares/heap", 10, "arrays"),
            topic("hash", "Tabelas Hash", "Mapeie chaves para posições com tratamento de colisões.", "nao-lineares", "nao-lineares/hash-tables", 11, "arrays"),
            topic("sortbase", "Algoritmos O(n²)", "Compare ordenações quadráticas como base conceitual.", "ordenacao", "ordenacao/quadraticos", 12, "analise", "arrays"),
            topic("sort", "Comparativo visual", "Compare estratégias de ordenação na prática.", "ordenacao", "ordenacao/comparativo", 13, "sortbase"),
            topic("mergesort", "Merge Sort", "Use divisão e conquista para ordenar de forma estável.", "ordenacao", "ordenacao/merge-sort", 14, "sort"),
            topic("quicksort", "Quick Sort", "Explore particionamento, pivôs e caso médio eficiente.", "ordenacao", "ordenacao/quick-sort", 15, "sort"),
            topic("grafos", "BFS e DFS", "Modele relações e percorra grafos em largura/profundidade.", "grafos", "grafos/bfs-dfs", 16, "pilhas", "ll"),
            topic("dijkstra", "Dijkstra", "Resolva caminhos mínimos com fila de prioridade.", "grafos", "grafos/dijkstra", 17, "grafos", "heap"),
            topic("dc", "Divisão e Conquista", "Quebre problemas em subproblemas independentes.", "paradigmas", "paradigmas/divisao-conquista", 18, "mergesort", "quicksort"),
            topic("pd", "Programação Dinâmica", "Reaproveite subproblemas sobrepostos com memória.", "paradigmas", "paradigmas/prog-dinamica", 19, "dc"),
            topic("guloso", "Algoritmos Gulosos", "Escolha localmente com prova de propriedade gulosa.", "paradigmas", "paradigmas/guloso", 20, "dijkstra")
    );

    private static final List<String> TOPIC_IDS = TOPICS.stream().map(TopicDefinition::id).toList();
    private static final Set<String> TOPIC_ID_SET = Set.copyOf(TOPIC_IDS);
    private static final Map<String, TopicDefinition> TOPIC_BY_ID = TOPICS.stream()
            .collect(Collectors.toUnmodifiableMap(TopicDefinition::id, Function.identity()));

    private static final List<ExerciseDefinition> EXERCISES = TOPICS.stream()
            .map(topic -> new ExerciseDefinition(
                    topic.id() + "-check",
                    topic.id(),
                    "MULTIPLE_CHOICE",
                    "Qual afirmação melhor representa o objetivo deste tópico: " + topic.title() + "?",
                    List.of(
                            "Memorizar nomes de classes sem analisar custo.",
                            topic.description(),
                            "Ignorar invariantes e focar somente em sintaxe.",
                            "Escolher sempre a estrutura mais complexa disponível."
                    ),
                    "B",
                    "Correto: você identificou o foco conceitual do tópico.",
                    "Revise a ideia central: " + topic.description(),
                    true,
                    10
            ))
            .toList();

    private static final Map<String, ExerciseDefinition> EXERCISE_BY_ID = EXERCISES.stream()
            .collect(Collectors.toUnmodifiableMap(ExerciseDefinition::id, Function.identity()));

    private static TopicDefinition topic(String id, String title, String description, String trackId,
                                         String path, int orderIndex, String... prerequisites) {
        return new TopicDefinition(id, title, description, trackId, path, orderIndex, List.of(prerequisites));
    }

    public boolean contains(String topicId) {
        return topicId != null && TOPIC_ID_SET.contains(topicId);
    }

    public int totalTopics() {
        return TOPIC_IDS.size();
    }

    public List<String> topicIds() {
        return TOPIC_IDS;
    }

    public List<TrackDefinition> tracks() {
        return TRACKS;
    }

    public List<TopicDefinition> topics() {
        return TOPICS;
    }

    public Optional<TopicDefinition> topic(String topicId) {
        return Optional.ofNullable(TOPIC_BY_ID.get(topicId));
    }

    public List<ExerciseDefinition> exercisesForTopic(String topicId) {
        return EXERCISES.stream()
                .filter(ex -> ex.topicId().equals(topicId))
                .toList();
    }

    public Optional<ExerciseDefinition> exercise(String exerciseId) {
        return Optional.ofNullable(EXERCISE_BY_ID.get(exerciseId));
    }

    public int requiredExercisesForTopic(String topicId) {
        return (int) exercisesForTopic(topicId).stream()
                .filter(ExerciseDefinition::required)
                .count();
    }
}

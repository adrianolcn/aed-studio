package com.aedstudio.service;

import com.aedstudio.dto.*;
import com.aedstudio.exception.InvalidTopicException;
import com.aedstudio.exception.LockedTopicException;
import com.aedstudio.model.CodeSubmission;
import com.aedstudio.model.User;
import com.aedstudio.repository.CodeSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CodeExecutionService {

    private static final List<String> FORBIDDEN = List.of(
            "Runtime", "ProcessBuilder", "System.exit", "java.io", "java.nio",
            "Thread", "ClassLoader", "reflect", "Socket", "URL", "while(true)");

    private final TopicCatalog topicCatalog;
    private final ProgressService progressService;
    private final DockerSandboxCommandFactory dockerSandboxCommandFactory;
    private final CodeSubmissionRepository codeSubmissionRepository;

    @Value("${code.sandbox.mode:local}")
    private String sandboxMode;

    @Value("${code.sandbox.timeout-seconds:2}")
    private long timeoutSeconds;

    @Transactional(readOnly = true)
    public List<CodeChallengeDto> challenges(User user, String topicId) {
        if (!topicCatalog.contains(topicId)) {
            throw new InvalidTopicException("topicId não cadastrado: " + topicId);
        }
        ensureTopicAvailable(user, topicId);
        return List.of(challengeFor(topicId));
    }

    @Transactional
    public CodeRunResponse run(User user, CodeRunRequest request) {
        long started = System.nanoTime();
        CodeChallenge challenge = challengeById(request.getChallengeId())
                .orElseThrow(() -> new InvalidTopicException("challengeId não cadastrado: " + request.getChallengeId()));
        ensureTopicAvailable(user, challenge.topicId());
        List<String> failed = validateSource(request.getCode());
        if (!failed.isEmpty()) {
            return persistSubmission(user, challenge, request.getCode(), response(challenge, false, "ERROR", List.of(), failed,
                    "Código rejeitado antes da execução por violar a política do sandbox.",
                    "Remova APIs de sistema e escreva apenas o corpo do método solicitado.",
                    0, elapsedMillis(started), user));
        }

        SandboxResult result = executeInSandbox(challenge, request.getCode());
        int awarded = 0;
        if (result.accepted()) {
            awarded = progressService.awardXp(user,
                    new XpAwardRequest(challenge.topicId(), "code_" + challenge.id(), challenge.xp()));
            progressService.recordCodeSuccess(user, challenge.topicId(), challenge.id());
        }

        return persistSubmission(user, challenge, request.getCode(), response(challenge, result.accepted(), result.status(),
                result.passedChecks(), result.failedChecks(), result.feedback(), result.hint(),
                awarded, elapsedMillis(started), user));
    }

    @Transactional(readOnly = true)
    public List<CodeSubmissionDto> submissions(User user, String topicId, String exerciseId) {
        List<CodeSubmission> submissions;
        if (exerciseId != null && !exerciseId.isBlank()) {
            submissions = codeSubmissionRepository.findTop20ByUserAndExerciseIdOrderByCreatedAtDesc(user, exerciseId);
        } else if (topicId != null && !topicId.isBlank()) {
            submissions = codeSubmissionRepository.findTop20ByUserAndTopicIdOrderByCreatedAtDesc(user, topicId);
        } else {
            submissions = codeSubmissionRepository.findTop20ByUserOrderByCreatedAtDesc(user);
        }
        Optional<Long> bestId = exerciseId == null || exerciseId.isBlank()
                ? Optional.empty()
                : bestSubmission(user, exerciseId).map(CodeSubmission::getId);
        return submissions.stream()
                .map(submission -> toSubmissionDto(submission, bestId.map(id -> id.equals(submission.getId())).orElse(false)))
                .toList();
    }

    @Transactional(readOnly = true)
    public CodeSubmissionDto latestSubmission(User user, String exerciseId) {
        return codeSubmissionRepository.findTopByUserAndExerciseIdOrderByCreatedAtDesc(user, exerciseId)
                .map(submission -> toSubmissionDto(submission, bestSubmission(user, exerciseId)
                        .map(best -> best.getId().equals(submission.getId()))
                        .orElse(false)))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public CodeSubmissionDto bestSubmissionDto(User user, String exerciseId) {
        return bestSubmission(user, exerciseId)
                .map(submission -> toSubmissionDto(submission, true))
                .orElse(null);
    }

    private CodeRunResponse response(
            CodeChallenge challenge,
            boolean accepted,
            String status,
            List<String> passed,
            List<String> failed,
            String feedback,
            String hint,
            int awarded,
            long executionTimeMs,
            User user) {
        return CodeRunResponse.builder()
                .challengeId(challenge.id())
                .accepted(accepted)
                .status(status)
                .passedChecks(passed)
                .failedChecks(failed)
                .passedCount(passed.size())
                .totalChecks(challenge.testCases().size())
                .executionTimeMs(executionTimeMs)
                .feedback(feedback)
                .hint(hint)
                .awarded(awarded)
                .progress(progressService.getProgress(user))
                .build();
    }

    private CodeRunResponse persistSubmission(
            User user,
            CodeChallenge challenge,
            String sourceCode,
            CodeRunResponse response) {
        CodeSubmission submission = codeSubmissionRepository.save(CodeSubmission.builder()
                .user(user)
                .topicId(challenge.topicId())
                .exerciseId(challenge.id())
                .sourceCode(sourceCode)
                .status(response.getStatus())
                .totalTests(response.getTotalChecks())
                .passedTests(response.getPassedCount())
                .executionTimeMs(response.getExecutionTimeMs())
                .passedChecks(serializeLines(response.getPassedChecks()))
                .failedChecks(serializeLines(response.getFailedChecks()))
                .feedback(response.getFeedback())
                .build());
        response.setSubmissionId(submission.getId());
        return response;
    }

    private Optional<CodeSubmission> bestSubmission(User user, String exerciseId) {
        return codeSubmissionRepository
                .findTopByUserAndExerciseIdAndStatusOrderByPassedTestsDescCreatedAtAsc(user, exerciseId, "SUCCESS")
                .or(() -> codeSubmissionRepository.findTopByUserAndExerciseIdOrderByPassedTestsDescCreatedAtAsc(user, exerciseId));
    }

    private CodeSubmissionDto toSubmissionDto(CodeSubmission submission, boolean best) {
        return CodeSubmissionDto.builder()
                .id(submission.getId())
                .topicId(submission.getTopicId())
                .exerciseId(submission.getExerciseId())
                .status(submission.getStatus())
                .totalTests(submission.getTotalTests())
                .passedTests(submission.getPassedTests())
                .executionTimeMs(submission.getExecutionTimeMs())
                .passedChecks(deserializeLines(submission.getPassedChecks()))
                .failedChecks(deserializeLines(submission.getFailedChecks()))
                .feedback(submission.getFeedback())
                .createdAt(submission.getCreatedAt())
                .best(best)
                .build();
    }

    private String serializeLines(List<String> lines) {
        return lines == null || lines.isEmpty() ? "" : String.join("\n", lines);
    }

    private List<String> deserializeLines(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return raw.lines().toList();
    }

    private long elapsedMillis(long startedNanos) {
        return Math.max(1L, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos));
    }

    private CodeChallengeDto challengeFor(String topicId) {
        CodeChallenge challenge = codeChallenge(topicId);
        return CodeChallengeDto.builder()
                .id(challenge.id())
                .topicId(challenge.topicId())
                .title(challenge.title())
                .prompt(challenge.prompt())
                .functionName(challenge.functionName())
                .signature(challenge.signature())
                .returnType(challenge.returnType())
                .examples(challenge.examples())
                .starterCode(challenge.starterCode())
                .expectedConcepts(challenge.expectedConcepts())
                .conceptualHint(challenge.conceptualHint())
                .structuralHint(challenge.structuralHint())
                .pseudoSkeleton(challenge.pseudoSkeleton())
                .difficulty(challenge.difficulty())
                .xp(challenge.xp())
                .build();
    }

    private void ensureTopicAvailable(User user, String topicId) {
        TopicStatusDto status = progressService.getProgress(user).getTopicStates().get(topicId);
        if (status != null && "LOCKED".equals(status.getState())) {
            throw new LockedTopicException("Complete os pré-requisitos antes de acessar desafios de código: " + topicId);
        }
    }

    private Optional<CodeChallenge> challengeById(String challengeId) {
        return topicCatalog.topicIds().stream()
                .map(this::codeChallenge)
                .filter(challenge -> challenge.id().equals(challengeId))
                .findFirst();
    }

    private CodeChallenge codeChallenge(String topicId) {
        return switch (topicId) {
            case "arrays" -> new CodeChallenge(
                    "arrays-code-search",
                    topicId,
                    "Busca em array sentinela",
                    "Implemente o corpo de solve(int[] values, int target). Retorne o índice da primeira ocorrência de target; se não existir, retorne -1.",
                    "solve",
                    "solve(int[] values, int target)",
                    "public static int solve(int[] values, int target)",
                    "int",
                    List.of("solve([3, 7, 9], 7) -> 1", "solve([4, 5, 6], 2) -> -1"),
                    "for (int i = 0; i < values.length; i++) {\n    if (values[i] == target) return i;\n}\nreturn -1;",
                    List.of("índice", "varredura linear", "caso ausente"),
                    "Busca linear compara elemento por elemento até encontrar a chave.",
                    "Use um for com índice; retorne imediatamente quando achar target e só retorne -1 depois do laço.",
                    "para i de 0 até n-1: se values[i] == target, retorne i; ao final, retorne -1",
                    1,
                    35,
                    List.of(
                            test("encontra no meio", "new int[]{3, 7, 9}, 7", 1),
                            test("encontra no começo", "new int[]{5, 2, 5}, 5", 0),
                            test("ausente", "new int[]{1, 2, 3}, 7", -1)));
            case "pilhas" -> new CodeChallenge(
                    "pilhas-code-top",
                    topicId,
                    "Topo após operações LIFO",
                    "Considere values como uma sequência de pushes em uma pilha. Retorne o topo; se a pilha estiver vazia, retorne -1.",
                    "solve",
                    "solve(int[] values)",
                    "public static int solve(int[] values)",
                    "int",
                    List.of("solve([4, 8, 15]) -> 15", "solve([]) -> -1"),
                    "if (values.length == 0) return -1;\nreturn values[values.length - 1];",
                    List.of("LIFO", "topo", "caso vazio"),
                    "Em pilha, o último valor inserido é o primeiro candidato a sair.",
                    "O topo está na última posição da sequência de pushes.",
                    "se não houver valores, retorne -1; senão retorne o último valor",
                    1,
                    30,
                    List.of(
                            test("topo simples", "new int[]{4, 8, 15}", 15),
                            test("um item", "new int[]{9}", 9),
                            test("vazia", "new int[]{}", -1)));
            case "tad" -> new CodeChallenge(
                    "tad-code-contract-length",
                    topicId,
                    "Contrato com entrada textual",
                    "Implemente solve(String input). Retorne o tamanho do texto sem espaços nas extremidades; para null, retorne 0.",
                    "solve",
                    "solve(String input)",
                    "public static int solve(String input)",
                    "int",
                    List.of("solve(\"  pilha \") -> 5", "solve(null) -> 0"),
                    "if (input == null) return 0;\nreturn input.trim().length();",
                    List.of("contrato", "entrada textual", "caso nulo"),
                    "O contrato do método define como tratar entradas especiais, inclusive null.",
                    "Faça a guarda de null antes de chamar métodos de String.",
                    "se input for null, retorne 0; senão retorne input.trim().length()",
                    1,
                    30,
                    List.of(
                            test("remove margens", "\"  pilha \"", 5),
                            test("texto vazio", "\"   \"", 0),
                            test("nulo", "null", 0)));
            case "notacao" -> new CodeChallenge(
                    "notacao-code-halving",
                    topicId,
                    "Passos logarítmicos",
                    "Implemente solve(int n). Retorne quantas divisões inteiras por 2 são feitas até n ficar menor ou igual a 1.",
                    "solve",
                    "solve(int n)",
                    "public static int solve(int n)",
                    "int",
                    List.of("solve(8) -> 3", "solve(1) -> 0"),
                    "int steps = 0;\nwhile (n > 1) {\n    n = n / 2;\n    steps++;\n}\nreturn steps;",
                    List.of("logaritmo", "laço controlado", "complexidade"),
                    "Reduzir pela metade a cada passo é o comportamento típico de O(log n).",
                    "Use uma condição que avance n em direção a 1 para evitar laço infinito.",
                    "steps=0; enquanto n>1: n=n/2; steps++; retorne steps",
                    2,
                    35,
                    List.of(
                            test("oito", "8", 3),
                            test("um", "1", 0),
                            test("dezenove", "19", 4)));
            case "ll" -> new CodeChallenge(
                    "ll-code-count-positive",
                    topicId,
                    "Contagem em lista encadeada simulada",
                    "Considere values como nós percorridos em ordem. Retorne quantos valores positivos existem.",
                    "solve",
                    "solve(int[] values)",
                    "public static int solve(int[] values)",
                    "int",
                    List.of("solve([-1, 3, 0, 5]) -> 2"),
                    "int count = 0;\nfor (int value : values) {\n    if (value > 0) count++;\n}\nreturn count;",
                    List.of("percurso", "estado acumulado", "condição"),
                    "Percorrer uma lista ligada é visitar um nó por vez mantendo um acumulador.",
                    "Inicie count em 0 e incremente apenas quando o valor for maior que zero.",
                    "count = 0; para cada nó: se valor > 0, count++; retorne count",
                    2,
                    35,
                    List.of(
                            test("mistos", "new int[]{-1, 3, 0, 5}", 2),
                            test("todos positivos", "new int[]{1, 2, 3}", 3),
                            test("nenhum positivo", "new int[]{-4, 0, -2}", 0)));
            case "bst" -> new CodeChallenge(
                    "bst-code-left-count",
                    topicId,
                    "Ramo esquerdo da BST",
                    "Considere values[0] como raiz de uma BST. Retorne quantos valores seguintes ficariam no ramo esquerdo da raiz.",
                    "solve",
                    "solve(int[] values)",
                    "public static int solve(int[] values)",
                    "int",
                    List.of("solve([10, 5, 12, 3]) -> 2"),
                    "if (values.length == 0) return 0;\nint root = values[0];\nint count = 0;\nfor (int i = 1; i < values.length; i++) {\n    if (values[i] < root) count++;\n}\nreturn count;",
                    List.of("comparação com raiz", "ramo esquerdo", "invariante"),
                    "Na BST, chaves menores que a raiz seguem para a esquerda.",
                    "Guarde a raiz e conte apenas elementos posteriores menores que ela.",
                    "root = primeiro; conte valores i>0 com values[i] < root",
                    2,
                    40,
                    List.of(
                            test("dois à esquerda", "new int[]{10, 5, 12, 3}", 2),
                            test("nenhum à esquerda", "new int[]{4, 8, 9}", 0),
                            test("sem raiz", "new int[]{}", 0)));
            case "hash" -> new CodeChallenge(
                    "hash-code-collisions",
                    topicId,
                    "Colisões módulo 5",
                    "Use hash(value)=abs(value)%5. Retorne quantas inserções caem em baldes que já estavam ocupados.",
                    "solve",
                    "solve(int[] values)",
                    "public static int solve(int[] values)",
                    "int",
                    List.of("solve([1, 6, 11]) -> 2"),
                    "boolean[] seen = new boolean[5];\nint collisions = 0;\nfor (int value : values) {\n    int bucket = Math.abs(value) % 5;\n    if (seen[bucket]) collisions++;\n    seen[bucket] = true;\n}\nreturn collisions;",
                    List.of("função hash", "balde", "colisão"),
                    "Colisão acontece quando duas chaves apontam para o mesmo balde.",
                    "Use um vetor booleano de 5 posições para lembrar baldes ocupados.",
                    "para cada valor: bucket=abs(valor)%5; se já visto, colisões++; marque visto",
                    2,
                    40,
                    List.of(
                            test("duas colisões", "new int[]{1, 6, 11}", 2),
                            test("sem colisão", "new int[]{1, 2, 3}", 0),
                            test("negativos", "new int[]{-1, 4, 9}", 1)));
            case "grafos" -> new CodeChallenge(
                    "grafos-code-edge-count",
                    topicId,
                    "Contagem de arestas codificadas",
                    "Implemente solve(String[] values). Cada item no formato A-B representa uma aresta; retorne quantas arestas válidas existem.",
                    "solve",
                    "solve(String[] values)",
                    "public static int solve(String[] values)",
                    "int",
                    List.of("solve([\"A-B\", \"B-C\", \"C\"]) -> 2"),
                    "int edges = 0;\nfor (String value : values) {\n    if (value != null && value.contains(\"-\")) edges++;\n}\nreturn edges;",
                    List.of("representação", "strings", "arestas"),
                    "Uma aresta pode ser representada por uma string origem-destino.",
                    "Conte apenas entradas não nulas que contenham o separador '-'.",
                    "edges=0; para cada texto: se contém '-', edges++; retorne edges",
                    1,
                    30,
                    List.of(
                            test("duas arestas", "new String[]{\"A-B\", \"B-C\", \"C\"}", 2),
                            test("com nulo", "new String[]{\"A-B\", null, \"D-E\"}", 2),
                            test("vazio", "new String[]{}", 0)));
            default -> new CodeChallenge(
                    topicId + "-code-sum",
                    topicId,
                    "Soma segura da expedição",
                    "Implemente o corpo de solve(int[] values). Retorne a soma dos elementos.",
                    "solve",
                    "solve(int[] values)",
                    "public static int solve(int[] values)",
                    "int",
                    List.of("solve([1, 2, 3]) -> 6"),
                    "int total = 0;\nfor (int value : values) {\n    total += value;\n}\nreturn total;",
                    List.of("percorrer estrutura", "acumular estado", "retornar resultado"),
                    "Use um acumulador para preservar o estado parcial do percurso.",
                    "Inicialize total em 0, some cada value e retorne total após o laço.",
                    "total = 0; para cada valor: total += valor; retorne total",
                    1,
                    30,
                    List.of(
                            test("casos públicos", "new int[]{1, 2, 3}", 6),
                            test("casos com negativos", "new int[]{4, -2, 8}", 10),
                            test("vetor vazio", "new int[]{}", 0)));
        };
    }

    private List<String> validateSource(String code) {
        List<String> failed = new ArrayList<>();
        if (code.length() > 6000) failed.add("Tamanho máximo excedido.");
        for (String forbidden : FORBIDDEN) {
            if (code.contains(forbidden)) failed.add("Uso proibido: " + forbidden);
        }
        return failed;
    }

    private SandboxResult executeInSandbox(CodeChallenge challenge, String methodBody) {
        if ("docker".equalsIgnoreCase(sandboxMode)) {
            return executeInDockerSandbox(challenge, methodBody);
        }
        return executeInLocalSandbox(challenge, methodBody);
    }

    private SandboxResult executeInLocalSandbox(CodeChallenge challenge, String methodBody) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return new SandboxResult(false, "ERROR", List.of(), List.of("JDK compiler indisponível"),
                    "O servidor precisa rodar com JDK para executar desafios de código.",
                    "Instale/execute com JDK 17+, não apenas JRE.");
        }

        Path dir = null;
        try {
            dir = Files.createTempDirectory("aed-code-sandbox-");
            Path source = dir.resolve("UserSolution.java");
            Files.writeString(source, sourceFor(challenge, methodBody), StandardCharsets.UTF_8);
            int compile = compiler.run(null, null, null, source.toString());
            if (compile != 0) {
                return new SandboxResult(false, "ERROR", List.of(), List.of("compilação"),
                        "O código não compilou dentro do sandbox.",
                        challenge.structuralHint());
            }

            String javaBin = Paths.get(System.getProperty("java.home"), "bin",
                    System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "java.exe" : "java").toString();
            Process process = new ProcessBuilder(javaBin, "-cp", dir.toString(), "UserSolution")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new SandboxResult(false, "ERROR", List.of(), List.of("tempo limite"),
                        "Execução interrompida por exceder o tempo limite.",
                        "Revise laços sem avanço de índice ou condição de parada.");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            return parseSandboxOutput(challenge, process.exitValue(), output, "sandbox local");
        } catch (Exception e) {
            return new SandboxResult(false, "ERROR", List.of(), List.of("execução"),
                    "Falha no sandbox: " + e.getMessage(),
                    challenge.structuralHint());
        } finally {
            cleanup(dir);
        }
    }

    private SandboxResult executeInDockerSandbox(CodeChallenge challenge, String methodBody) {
        Path dir = null;
        try {
            dir = Files.createTempDirectory("aed-code-docker-sandbox-");
            Path source = dir.resolve("UserSolution.java");
            Files.writeString(source, sourceFor(challenge, methodBody), StandardCharsets.UTF_8);

            Process process = new ProcessBuilder(dockerSandboxCommandFactory.build(dir))
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new SandboxResult(false, "ERROR", List.of(), List.of("tempo limite"),
                        "Execução em container interrompida por exceder o tempo limite.",
                        "Revise laços sem avanço de índice ou condição de parada.");
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            return parseSandboxOutput(challenge, process.exitValue(), output, "container");
        } catch (IOException e) {
            return new SandboxResult(false, "ERROR", List.of(), List.of("docker indisponível"),
                    "Não foi possível iniciar o sandbox Docker. Verifique se Docker está instalado e se a imagem está disponível.",
                    "Em desenvolvimento, use CODE_SANDBOX_MODE=local.");
        } catch (Exception e) {
            return new SandboxResult(false, "ERROR", List.of(), List.of("container"),
                    "Falha no sandbox Docker: " + e.getMessage(),
                    challenge.structuralHint());
        } finally {
            cleanup(dir);
        }
    }

    private SandboxResult parseSandboxOutput(CodeChallenge challenge, int exitCode, String output, String mode) {
        List<String> passed = linesWithPrefix(output, "PASS:");
        List<String> failed = linesWithPrefix(output, "FAIL:");
        if (exitCode == 0 && failed.isEmpty() && passed.size() == challenge.testCases().size()) {
            return new SandboxResult(true, "SUCCESS", passed, List.of(),
                    "Todos os " + passed.size() + " cenários passaram no " + mode + ".",
                    "Bom trabalho: a solução preservou o contrato do tópico.");
        }
        if (failed.isEmpty() && output != null && !output.isBlank()) {
            failed = List.of(output);
        }
        return new SandboxResult(false, "FAILURE", passed, failed.isEmpty() ? List.of("testes") : failed,
                "A solução executou, mas ainda não passou em todos os cenários.",
                challenge.structuralHint());
    }

    private List<String> linesWithPrefix(String output, String prefix) {
        if (output == null || output.isBlank()) return List.of();
        return output.lines()
                .filter(line -> line.startsWith(prefix))
                .map(line -> line.substring(prefix.length()).trim())
                .toList();
    }

    private String sourceFor(CodeChallenge challenge, String methodBody) {
        return """
                public class UserSolution {
                    METHOD_DECLARATION {
                """
                .replace("METHOD_DECLARATION", challenge.methodDeclaration())
                + methodBody.indent(8) + """
                    }
                    public static void main(String[] args) {
                """ + testSource(challenge.testCases(), challenge.functionName()).indent(8) + """
                    }
                    private static int failures = 0;
                    private static void check(String name, int actual, int expected) {
                        if (actual == expected) {
                            System.out.println("PASS:" + name);
                        } else {
                            failures++;
                            System.out.println("FAIL:" + name + " esperado=" + expected + " obtido=" + actual);
                        }
                    }
                    private static void finish() {
                        if (failures > 0) throw new IllegalStateException("falhas=" + failures);
                    }
                }
                """;
    }

    private String testSource(List<TestCase> tests, String functionName) {
        StringBuilder builder = new StringBuilder();
        for (TestCase test : tests) {
            builder.append("check(\"")
                    .append(escapeJava(test.name()))
                    .append("\", ")
                    .append(functionName)
                    .append("(")
                    .append(test.arguments())
                    .append("), ")
                    .append(test.expected())
                    .append(");\n");
        }
        builder.append("finish();\n");
        return builder.toString();
    }

    private String escapeJava(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private TestCase test(String name, String arguments, int expected) {
        return new TestCase(name, arguments, expected);
    }

    private void cleanup(Path dir) {
        if (dir == null) return;
        try (var stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            });
        } catch (IOException ignored) { }
    }

    private record CodeChallenge(
            String id,
            String topicId,
            String title,
            String prompt,
            String functionName,
            String signature,
            String methodDeclaration,
            String returnType,
            List<String> examples,
            String starterCode,
            List<String> expectedConcepts,
            String conceptualHint,
            String structuralHint,
            String pseudoSkeleton,
            int difficulty,
            int xp,
            List<TestCase> testCases
    ) {}

    private record TestCase(String name, String arguments, int expected) {}

    private record SandboxResult(
            boolean accepted,
            String status,
            List<String> passedChecks,
            List<String> failedChecks,
            String feedback,
            String hint
    ) {}
}

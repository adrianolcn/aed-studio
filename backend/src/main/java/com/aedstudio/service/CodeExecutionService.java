package com.aedstudio.service;

import com.aedstudio.dto.*;
import com.aedstudio.exception.InvalidTopicException;
import com.aedstudio.model.User;
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

    @Value("${code.sandbox.mode:local}")
    private String sandboxMode;

    @Value("${code.sandbox.timeout-seconds:2}")
    private long timeoutSeconds;

    @Transactional(readOnly = true)
    public List<CodeChallengeDto> challenges(User user, String topicId) {
        if (!topicCatalog.contains(topicId)) {
            throw new InvalidTopicException("topicId não cadastrado: " + topicId);
        }
        return List.of(challengeFor(topicId));
    }

    @Transactional
    public CodeRunResponse run(User user, CodeRunRequest request) {
        CodeChallenge challenge = challengeById(request.getChallengeId())
                .orElseThrow(() -> new InvalidTopicException("challengeId não cadastrado: " + request.getChallengeId()));
        List<String> failed = validateSource(request.getCode());
        if (!failed.isEmpty()) {
            return CodeRunResponse.builder()
                    .challengeId(challenge.id())
                    .accepted(false)
                    .passedChecks(List.of())
                    .failedChecks(failed)
                    .feedback("Código rejeitado antes da execução por violar a política do sandbox.")
                    .awarded(0)
                    .progress(progressService.getProgress(user))
                    .build();
        }

        SandboxResult result = executeInSandbox(challenge, request.getCode());
        int awarded = 0;
        if (result.accepted()) {
            awarded = progressService.awardXp(user,
                    new XpAwardRequest(challenge.topicId(), "code_" + challenge.id(), challenge.xp()));
        }

        return CodeRunResponse.builder()
                .challengeId(challenge.id())
                .accepted(result.accepted())
                .passedChecks(result.passedChecks())
                .failedChecks(result.failedChecks())
                .feedback(result.feedback())
                .awarded(awarded)
                .progress(progressService.getProgress(user))
                .build();
    }

    private CodeChallengeDto challengeFor(String topicId) {
        CodeChallenge challenge = codeChallenge(topicId);
        return CodeChallengeDto.builder()
                .id(challenge.id())
                .topicId(challenge.topicId())
                .title(challenge.title())
                .prompt(challenge.prompt())
                .starterCode(challenge.starterCode())
                .expectedConcepts(challenge.expectedConcepts())
                .xp(challenge.xp())
                .build();
    }

    private Optional<CodeChallenge> challengeById(String challengeId) {
        return topicCatalog.topicIds().stream()
                .map(this::codeChallenge)
                .filter(challenge -> challenge.id().equals(challengeId))
                .findFirst();
    }

    private CodeChallenge codeChallenge(String topicId) {
        return new CodeChallenge(
                topicId + "-code-sum",
                topicId,
                "Soma segura da expedição",
                "Implemente o corpo de um método que recebe int[] values e retorna a soma dos elementos.",
                "int total = 0;\nfor (int value : values) {\n    total += value;\n}\nreturn total;",
                List.of("percorrer estrutura", "acumular estado", "retornar resultado"),
                30);
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
            return executeInDockerSandbox(methodBody);
        }
        return executeInLocalSandbox(methodBody);
    }

    private SandboxResult executeInLocalSandbox(String methodBody) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return new SandboxResult(false, List.of(), List.of("JDK compiler indisponível"),
                    "O servidor precisa rodar com JDK para executar desafios de código.");
        }

        Path dir = null;
        try {
            dir = Files.createTempDirectory("aed-code-sandbox-");
            Path source = dir.resolve("UserSolution.java");
            Files.writeString(source, sourceFor(methodBody), StandardCharsets.UTF_8);
            int compile = compiler.run(null, null, null, source.toString());
            if (compile != 0) {
                return new SandboxResult(false, List.of(), List.of("compilação"),
                        "O código não compilou dentro do sandbox.");
            }

            String javaBin = Paths.get(System.getProperty("java.home"), "bin",
                    System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "java.exe" : "java").toString();
            Process process = new ProcessBuilder(javaBin, "-cp", dir.toString(), "UserSolution")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new SandboxResult(false, List.of(), List.of("tempo limite"),
                        "Execução interrompida por exceder o tempo limite.");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.exitValue() == 0) {
                return new SandboxResult(true, List.of("casos públicos", "casos com negativos", "vetor vazio"), List.of(),
                        "Todos os casos do sandbox passaram.");
            }
            return new SandboxResult(false, List.of(), List.of(output.isBlank() ? "testes" : output),
                    "O código executou, mas não passou nos testes.");
        } catch (Exception e) {
            return new SandboxResult(false, List.of(), List.of("execução"), "Falha no sandbox: " + e.getMessage());
        } finally {
            cleanup(dir);
        }
    }

    private SandboxResult executeInDockerSandbox(String methodBody) {
        Path dir = null;
        try {
            dir = Files.createTempDirectory("aed-code-docker-sandbox-");
            Path source = dir.resolve("UserSolution.java");
            Files.writeString(source, sourceFor(methodBody), StandardCharsets.UTF_8);

            Process process = new ProcessBuilder(dockerSandboxCommandFactory.build(dir))
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new SandboxResult(false, List.of(), List.of("tempo limite"),
                        "Execução em container interrompida por exceder o tempo limite.");
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.exitValue() == 0) {
                return new SandboxResult(true, List.of("casos públicos", "casos com negativos", "vetor vazio"), List.of(),
                        "Todos os casos do sandbox em container passaram.");
            }
            return new SandboxResult(false, List.of(), List.of(output.isBlank() ? "container" : output),
                    "O container executou, mas o código não passou nos testes.");
        } catch (IOException e) {
            return new SandboxResult(false, List.of(), List.of("docker indisponível"),
                    "Não foi possível iniciar o sandbox Docker. Verifique se Docker está instalado e se a imagem está disponível.");
        } catch (Exception e) {
            return new SandboxResult(false, List.of(), List.of("container"), "Falha no sandbox Docker: " + e.getMessage());
        } finally {
            cleanup(dir);
        }
    }

    private String sourceFor(String methodBody) {
        return """
                public class UserSolution {
                    public static int solve(int[] values) {
                """ + methodBody.indent(8) + """
                    }
                    public static void main(String[] args) {
                        check(solve(new int[]{1,2,3}) == 6, "caso simples");
                        check(solve(new int[]{4,-2,8}) == 10, "caso com negativo");
                        check(solve(new int[]{}) == 0, "vetor vazio");
                    }
                    private static void check(boolean ok, String name) {
                        if (!ok) throw new IllegalStateException(name);
                    }
                }
                """;
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
            String starterCode,
            List<String> expectedConcepts,
            int xp
    ) {}

    private record SandboxResult(
            boolean accepted,
            List<String> passedChecks,
            List<String> failedChecks,
            String feedback
    ) {}
}

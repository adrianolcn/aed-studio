package com.aedstudio.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("DockerSandboxCommandFactory")
class DockerSandboxCommandFactoryTest {

    @Test
    @DisplayName("monta comando com isolamento de rede, recursos e privilégios")
    void buildsHardenedDockerCommand() {
        DockerSandboxCommandFactory factory =
                new DockerSandboxCommandFactory("eclipse-temurin:17-jdk", "0.5", "128m", "64");

        List<String> command = factory.build(Path.of("sandbox-workspace"));

        assertTrue(command.contains("--network"));
        assertTrue(command.contains("none"));
        assertTrue(command.contains("--memory"));
        assertTrue(command.contains("128m"));
        assertTrue(command.contains("--pids-limit"));
        assertTrue(command.contains("--read-only"));
        assertTrue(command.contains("no-new-privileges"));
        assertTrue(command.stream().anyMatch(arg -> arg.endsWith(":/workspace:ro")));
    }
}

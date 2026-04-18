package com.aedstudio.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class DockerSandboxCommandFactory {

    private final String image;
    private final String cpus;
    private final String memory;
    private final String pidsLimit;

    public DockerSandboxCommandFactory(
            @Value("${code.sandbox.docker-image:eclipse-temurin:17-jdk}") String image,
            @Value("${code.sandbox.docker-cpus:0.5}") String cpus,
            @Value("${code.sandbox.docker-memory:128m}") String memory,
            @Value("${code.sandbox.docker-pids-limit:64}") String pidsLimit) {
        this.image = image;
        this.cpus = cpus;
        this.memory = memory;
        this.pidsLimit = pidsLimit;
    }

    public List<String> build(Path workspace) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("run");
        command.add("--rm");
        command.add("--network");
        command.add("none");
        command.add("--cpus");
        command.add(cpus);
        command.add("--memory");
        command.add(memory);
        command.add("--pids-limit");
        command.add(pidsLimit);
        command.add("--read-only");
        command.add("--tmpfs");
        command.add("/tmp:rw,noexec,nosuid,size=64m");
        command.add("--security-opt");
        command.add("no-new-privileges");
        command.add("-v");
        command.add(workspace.toAbsolutePath() + ":/workspace:ro");
        command.add("-w");
        command.add("/workspace");
        command.add(image);
        command.add("sh");
        command.add("-c");
        command.add("javac /workspace/UserSolution.java -d /tmp/aed-out && java -cp /tmp/aed-out UserSolution");
        return command;
    }
}

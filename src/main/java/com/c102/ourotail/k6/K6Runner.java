package com.c102.ourotail.k6;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.c102.ourotail.k6.config.K6RunnerProperties;
import com.c102.ourotail.k6.dto.K6OptionDto;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class K6Runner {

    private final DockerClient dockerClient;
    private final K6RunnerProperties properties;
    private static final String K6_IMAGE = "grafana/k6:latest";

    /**
     * Docker 이미지가 로컬에 없으면 Docker Hub에서 받아오는 메서드
     */
    private void pullImageIfNotExists(String imageName) throws InterruptedException {
        try {
            dockerClient.inspectImageCmd(imageName).exec();
            log.info("{} image already exists locally.", imageName);
        } catch (NotFoundException e) {
            log.info("{} image not found locally. Pulling from Docker Hub...", imageName);
            dockerClient.pullImageCmd(imageName).start().awaitCompletion();
            log.info("{} image pulled successfully.", imageName);
        }
    }

    /**
     * k6 테스트를 실행하는 메인 메서드
     */
    public String runK6Test(K6OptionDto options) throws InterruptedException, IOException {
        pullImageIfNotExists(K6_IMAGE);

        String runId = UUID.randomUUID().toString();

        // K6RunnerProperties에서 경로 설정 가져오기
        String baseDir = properties.getPaths().getBaseDir();
        Path tempDirInSpring = Paths.get(baseDir, runId);
        String tempDirOnHost = baseDir + File.separator + runId;

        log.info("Path in Spring's environment (for writing files): {}", tempDirInSpring.toAbsolutePath());
        log.info("Path on Host (for k6 container bind mount): {}", tempDirOnHost);

        String containerId = null;
        try {
            Files.createDirectories(tempDirInSpring);

            Path scriptFile = tempDirInSpring.resolve("script.js");
            Path resultFile = tempDirInSpring.resolve("results.json");

            log.info("k6 target URL: {}", options.getTargetUrl());
            String scriptContent = generateK6Script(options);
            Files.writeString(scriptFile, scriptContent, StandardCharsets.UTF_8);
            log.info("Dynamically generated script file created at: {}", scriptFile.toAbsolutePath());

            Volume volume = new Volume(properties.getPaths().getContainerMountDir());
            Bind bind = new Bind(tempDirOnHost, volume);
            HostConfig hostConfig = new HostConfig()
                    .withExtraHosts("host.docker.internal:host-gateway")
                    .withBinds(bind);

            containerId = dockerClient.createContainerCmd(K6_IMAGE)
                    .withHostConfig(hostConfig)
                    .withUser("root")
                    .withCmd("run", properties.getPaths().getContainerMountDir() + "/script.js", 
                             "--summary-export=" + properties.getPaths().getContainerMountDir() + "/results.json")
                    .exec().getId();

            dockerClient.startContainerCmd(containerId).exec();
            log.info("k6 container started with ID: {}", containerId);

            int statusCode = dockerClient.waitContainerCmd(containerId).start().awaitStatusCode();
            log.info("k6 container finished with status code: {}", statusCode);

            if (statusCode != 0) {
                String logs = getContainerLogs(containerId);
                throw new RuntimeException("k6 test failed with status code " + statusCode + ". Logs:\n" + logs);
            }

            if (Files.exists(resultFile)) {
                return Files.readString(resultFile, StandardCharsets.UTF_8);
            } else {
                String logs = getContainerLogs(containerId);
                return "{\"message\": \"Test completed successfully, but no result file was generated.\", \"logs\": \"" + logs.replace("\"", "\\\"") + "\"}";
            }

        } finally {
            if (containerId != null) {
                try {
                    dockerClient.removeContainerCmd(containerId).withForce(true).exec();
                    log.info("Container {} removed.", containerId);
                } catch (NotFoundException e) {
                    log.warn("Container {} was already removed.", containerId);
                }
            }
            if (Files.exists(tempDirInSpring)) {
                // 참고: 프로덕션 환경에서는 임시 파일/디렉토리 정리 로직을 활성화하는 것이 좋습니다.
//                try (var stream = Files.walk(tempDirInSpring)) {
//                    stream.sorted(Comparator.reverseOrder())
//                            .forEach(path -> {
//                                try {
//                                    Files.delete(path);
//                                } catch (IOException e) {
//                                    log.error("Failed to delete temp file {}", path, e);
//                                }
//                            });
//                }
//                log.info("Temporary directory {} deleted.", tempDirInSpring);
            }
        }
    }

    /**
     * Generates the k6 script dynamically based on user options.
     */
    private String generateK6Script(K6OptionDto options) {
        String method = options.getMethod().toUpperCase();
        String body = options.getBody();
        boolean hasBody = (method.equals("POST") || method.equals("PUT")) && body != null && !body.trim().isEmpty();
        String payload = hasBody ? body : "null";

        String originalUrl = options.getTargetUrl();
        String targetUrlForK6 = originalUrl;
        if (targetUrlForK6 != null && (targetUrlForK6.contains("://localhost") || targetUrlForK6.contains("://127.0.0.1"))) {
            targetUrlForK6 = targetUrlForK6.replace("localhost", "host.docker.internal")
                    .replace("127.0.0.1", "host.docker.internal");
            log.info("Replaced localhost with host.docker.internal for k6 container. New URL: {}", targetUrlForK6);
        }

        return String.format("""
            import http from 'k6/http';
            import { check, sleep } from 'k6';

            export const options = {
              vus: %d,
              duration: '%ds',
            };

            export default function () {
              const url = '%s';
              const method = '%s';
              const payload = %s;
              const params = {
                headers: { 'Content-Type': 'application/json' },
              };
              const bodyToSend = (method === 'POST' || method === 'PUT') ? JSON.stringify(payload) : null;
              const res = http.request(method, url, bodyToSend, params);
              check(res, { 'status is 200': (r) => r.status == 200 });
              sleep(1);
            }
            """,
                options.getVirtualUsers(),
                options.getDuration(),
                targetUrlForK6,
                method,
                payload
        );
    }

    /**
     * 컨테이너의 로그를 가져오는 헬퍼 메서드
     */
    private String getContainerLogs(String containerId) {
        final ByteArrayOutputStream logStream = new ByteArrayOutputStream();
        final CountDownLatch latch = new CountDownLatch(1);

        ResultCallback.Adapter<Frame> callback = new ResultCallback.Adapter<Frame>() {
            @Override
            public void onNext(Frame item) {
                try {
                    logStream.write(item.getPayload());
                } catch (IOException e) {
                    onError(e);
                }
            }
            @Override
            public void onComplete() {
                latch.countDown();
                super.onComplete();
            }
            @Override
            public void onError(Throwable throwable) {
                log.error("Error receiving container logs for {}", containerId, throwable);
                latch.countDown();
                super.onError(throwable);
            }
        };

        try {
            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .exec(callback);
            boolean completed = latch.await(1, TimeUnit.MINUTES);
            if (!completed) {
                log.warn("Log collection timed out for container {}", containerId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Log collection interrupted for container {}", containerId, e);
            return "Failed to retrieve container logs due to interruption.";
        } catch (Exception e) {
            log.warn("Could not retrieve logs for container {}. It might have been already removed.", containerId, e);
            return "Failed to retrieve container logs. The container might have been removed prematurely.";
        }
        return logStream.toString(StandardCharsets.UTF_8);
    }
}
package com.c102.ourotail.k6.config;

import com.c102.ourotail.k6.K6Runner;
import com.c102.ourotail.k6.controller.K6Controller;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * K6Runner specific auto-configuration
 * Provides DockerClient bean for K6Runner functionality
 */
@Configuration
@EnableConfigurationProperties(K6RunnerProperties.class)
public class K6RunnerAutoConfiguration {

    /**
     * Register DockerClient bean for K6Runner
     * @param properties K6Runner configuration properties
     * @return DockerClient instance
     */
    @Bean
    @ConditionalOnMissingBean
    public DockerClient dockerClient(K6RunnerProperties properties) {
        DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder();

        // 설정 프로퍼티에서 docker.host 값을 읽어와 설정합니다.
        if (properties.getDockerHost() != null && !properties.getDockerHost().isEmpty()) {
            configBuilder.withDockerHost(properties.getDockerHost());
        }

        DefaultDockerClientConfig config = configBuilder.build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        return DockerClientImpl.getInstance(config, httpClient);
    }

    
    /**
     * Register K6Runner bean (requires DockerClient)
     * @param dockerClient Docker client instance
     * @return K6Runner instance
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(DockerClient.class)
    public K6Runner k6Runner(DockerClient dockerClient, K6RunnerProperties properties) {
        return new K6Runner(dockerClient, properties);
    }

    /**
     * Register K6Controller bean (optional, controlled by property)
     * @param k6Runner K6Runner service instance
     * @return K6Controller instance
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "k6.runner.controller.enabled", havingValue = "true", matchIfMissing = true)
    public K6Controller k6Controller(K6Runner k6Runner) {
        return new K6Controller(k6Runner);
    }
}

package com.c102.ourotail.k6.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "k6.runner")
public class K6RunnerProperties {

    /**
     * Docker 데몬에 연결하기 위한 URI. (예: tcp://localhost:2375, unix:///var/run/docker.sock)
     * 비워두면, 시스템 환경 변수(DOCKER_HOST)나 기본값을 사용합니다.
     */
    private String dockerHost;

    /**
     * k6 스크립트 및 결과 파일 경로 설정
     */
    private final Paths paths = new Paths();

    public String getDockerHost() {
        return dockerHost;
    }

    public void setDockerHost(String dockerHost) {
        this.dockerHost = dockerHost;
    }

    public Paths getPaths() {
        return paths;
    }

    public static class Paths {
        private String baseDir = System.getProperty("user.dir") + "/k6-runs";

        /**
         * k6 스크립트가 생성되고 결과 파일이 저장될 호스트 머신의 기본 디렉토리.
         */
        public String getBaseDir() {
            return baseDir;
        }

        public void setBaseDir(String baseDir) {
            this.baseDir = baseDir;
        }

        /**
         * 스크립트 파일이 마운트될 컨테이너 내부의 디렉토리.
         */
        private String containerMountDir = "/k6";

        public String getContainerMountDir() {
            return containerMountDir;
        }

        public void setContainerMountDir(String containerMountDir) {
            this.containerMountDir = containerMountDir;
        }
    }
}

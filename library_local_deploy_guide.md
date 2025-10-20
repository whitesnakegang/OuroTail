# 라이브러리 로컬 배포 가이드

## 개요

이 문서는 Spring Boot 기반의 라이브러리(SDK)를 로컬 Maven 저장소에 배포하는 방법을 단계별로 설명합니다. 우리가 작성한 `ourotail` SDK를 예시로 사용하여 실제 코드와 함께 설명합니다.

## 1. 프로젝트 구조 이해

### 1.1 현재 프로젝트 구조
```
ourotail/
├── build.gradle                    # Gradle 빌드 설정
├── settings.gradle                 # 프로젝트 설정
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/c102/ourotail/
│   │   │       ├── config/
│   │   │       │   └── OurotailAutoConfiguration.java
│   │   │       └── controller/
│   │   │           └── TestController.java
│   │   └── resources/
│   │       └── META-INF/spring/
│   │           └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│   └── test/
│       └── java/
│           └── com/c102/ourotail/
│               └── config/
│                   └── OurotailAutoConfigurationTest.java
└── build/                          # 빌드 결과물
```

### 1.2 핵심 컴포넌트 설명

#### OurotailAutoConfiguration.java
```java
@AutoConfiguration
@ConditionalOnClass({TestController.class, K6Runner.class})
@EnableConfigurationProperties(K6RunnerProperties.class)
public class OurotailAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TestController testController() {
        return new TestController();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(DockerClient.class)
    public K6Runner k6Runner(DockerClient dockerClient) {
        return new K6Runner(dockerClient);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "k6.runner.controller.enabled", havingValue = "true", matchIfMissing = true)
    public K6Controller k6Controller(K6Runner k6Runner) {
        return new K6Controller(k6Runner);
    }
}
```

- `@AutoConfiguration`: Spring Boot 자동 설정 클래스임을 나타냄
- `@ConditionalOnClass`: TestController와 K6Runner 클래스가 클래스패스에 있을 때만 활성화
- `@ConditionalOnMissingBean`: 해당 빈이 이미 존재하지 않을 때만 생성
- `@EnableConfigurationProperties`: K6RunnerProperties 설정 프로퍼티 활성화

#### TestController.java
```java
@Component
@RestController
@RequestMapping("/api/test")
public class TestController {
    
    @GetMapping("/hello")
    public Map<String, Object> hello() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello from Ourotail SDK!");
        response.put("status", "success");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
    
    // ... 다른 메서드들
}
```

#### K6Runner.java (K6 성능 테스트 실행기)
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class K6Runner {

    private final DockerClient dockerClient;
    private final K6RunnerProperties properties;
    private static final String K6_IMAGE = "grafana/k6:latest";

    public String runK6Test(K6OptionDto options) throws InterruptedException, IOException {
        // Docker 컨테이너를 사용하여 K6 성능 테스트 실행
        // 동적으로 K6 스크립트 생성 및 실행
        // 결과를 JSON 형태로 반환
    }
}
```

#### K6Controller.java (K6 REST API)
```java
@RestController
@RequiredArgsConstructor
public class K6Controller {

    private final K6Runner k6Runner;
    
    @PostMapping("/k6/run")
    public ResponseEntity<String> runK6Test(@RequestBody K6OptionDto options) {
        try {
            String result = k6Runner.runK6Test(options);
            return ResponseEntity.ok().header("Content-Type", "text/plain").body(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error running k6 test: " + e.getMessage());
        }
    }
}
```

#### K6RunnerProperties.java (설정 프로퍼티)
```java
@ConfigurationProperties(prefix = "k6.runner")
public class K6RunnerProperties {
    
    private String dockerHost;
    private final Paths paths = new Paths();

    public static class Paths {
        private String baseDir = System.getProperty("user.dir") + "/k6-runs";
        private String containerMountDir = "/k6";
    }
}
```

## 2. Gradle 빌드 설정

### 2.1 build.gradle 주요 설정

```gradle
plugins {
    id 'java-library'                    // 라이브러리용 플러그인
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'com.c102'                       // 그룹 ID
version = '0.0.1-SNAPSHOT'              // 버전
description = 'K6 SDK for Spring Boot'  // 설명

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    withSourcesJar()                     // 소스 JAR 생성
    withJavadocJar()                     // JavaDoc JAR 생성
}
```

### 2.2 핵심 의존성

```gradle
dependencies {
    // 자동 설정을 위한 핵심 의존성
    implementation 'org.springframework.boot:spring-boot-autoconfigure'
    
    // 웹 컨트롤러 사용을 위한 의존성 (Tomcat 없음)
    implementation 'org.springframework:spring-web'
    
    // 설정 프로퍼티 처리
    implementation 'org.springframework.boot:spring-boot-configuration-processor'
    annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'
    
    // 로깅 인터페이스 (구현체는 사용자가 선택)
    compileOnly 'org.slf4j:slf4j-api'
    
    // Docker Java API for K6 integration
    implementation 'com.github.docker-java:docker-java:3.3.6'
    implementation 'com.github.docker-java:docker-java-transport-httpclient5:3.3.6'
    
    // Lombok for reducing boilerplate code
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    
    // 테스트 의존성
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

### 2.3 Maven 배포 설정

```gradle
apply plugin: 'maven-publish'

publishing {
    publications {
        maven(MavenPublication) {
            from components.java
            
            groupId = 'com.c102'
            artifactId = 'ourotail-sdk'
            version = '0.0.1-SNAPSHOT'
            
            pom {
                name = 'Ourotail SDK'
                description = 'K6 SDK for Spring Boot'
                url = 'https://github.com/your-org/ourotail-sdk'
                
                licenses {
                    license {
                        name = 'The Apache License, Version 2.0'
                        url = 'http://www.apache.org/licenses/LICENSE-2.0.txt'
                    }
                }
            }
        }
    }
    
    repositories {
        mavenLocal()  // 로컬 Maven 저장소에 배포
    }
}
```

## 3. 자동 설정 등록

### 3.1 AutoConfiguration.imports 파일

`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 파일에 자동 설정 클래스를 등록:

```
com.c102.ourotail.config.OurotailAutoConfiguration
com.c102.ourotail.k6.config.K6RunnerAutoConfiguration
```

이 파일은 Spring Boot가 자동으로 스캔하여 자동 설정을 활성화합니다. K6RunnerAutoConfiguration은 DockerClient 빈을 제공하고, OurotailAutoConfiguration은 K6Runner와 K6Controller 빈을 등록합니다.

## 4. 로컬 배포 과정

### 4.1 빌드 및 테스트

```bash
# 프로젝트 루트 디렉토리에서 실행
./gradlew clean build
```

실행 결과:
- `build/libs/ourotail-0.0.1-SNAPSHOT.jar` - 메인 JAR 파일
- `build/libs/ourotail-0.0.1-SNAPSHOT-sources.jar` - 소스 JAR 파일
- `build/libs/ourotail-0.0.1-SNAPSHOT-javadoc.jar` - JavaDoc JAR 파일

### 4.2 로컬 Maven 저장소에 배포

```bash
./gradlew publishToMavenLocal
```

이 명령어는 다음 위치에 라이브러리를 배포합니다:
- Windows: `C:\Users\{사용자명}\.m2\repository\com\c102\ourotail-sdk\0.0.1-SNAPSHOT\`
- macOS/Linux: `~/.m2/repository/com/c102/ourotail-sdk/0.0.1-SNAPSHOT/`

### 4.3 배포 확인

로컬 Maven 저장소에 다음 파일들이 생성되었는지 확인:

```
~/.m2/repository/com/c102/ourotail-sdk/0.0.1-SNAPSHOT/
├── ourotail-sdk-0.0.1-SNAPSHOT.jar
├── ourotail-sdk-0.0.1-SNAPSHOT-sources.jar
├── ourotail-sdk-0.0.1-SNAPSHOT-javadoc.jar
├── ourotail-sdk-0.0.1-SNAPSHOT.pom
└── maven-metadata-local.xml
```

## 5. 다른 프로젝트에서 사용하기

### 5.1 의존성 추가

다른 Spring Boot 프로젝트의 `build.gradle`에 의존성 추가:

```gradle
dependencies {
    implementation 'com.c102:ourotail-sdk:0.0.1-SNAPSHOT'
    
    // Spring Boot 애플리케이션 의존성
    implementation 'org.springframework.boot:spring-boot-starter-web'
}
```

### 5.2 자동 설정 활성화

Spring Boot 애플리케이션에서 자동 설정이 활성화되면, 다음 컴포넌트들이 자동으로 등록됩니다:

- `TestController`: 기본 테스트 API 제공
- `DockerClient`: Docker API 클라이언트 (K6Runner에서 사용)
- `K6Runner`: K6 성능 테스트 실행 서비스
- `K6Controller`: K6 성능 테스트 REST API (설정에 따라 활성화)

### 5.3 API 테스트

애플리케이션 실행 후 다음 엔드포인트들을 테스트할 수 있습니다:

#### 기본 테스트 API
```bash
# GET 요청 테스트
curl http://localhost:8080/api/test/hello

# 응답 예시
{
  "message": "Hello from Ourotail SDK!",
  "status": "success",
  "timestamp": 1703123456789
}

# POST 요청 테스트
curl -X POST http://localhost:8080/api/test/echo \
  -H "Content-Type: application/json" \
  -d '{"test": "data"}'

# SDK 정보 조회
curl http://localhost:8080/api/test/info
```

#### K6 성능 테스트 API
```bash
# K6 성능 테스트 실행
curl -X POST http://localhost:8080/k6/run \
  -H "Content-Type: application/json" \
  -d '{
    "targetUrl": "http://localhost:8080/api/test/hello",
    "virtualUsers": 10,
    "duration": 30,
    "method": "GET",
    "body": null
  }'

# 응답 예시 (K6 테스트 결과)
{
  "metrics": {
    "http_reqs": 300,
    "http_req_duration": {
      "avg": 15.2,
      "min": 8.1,
      "max": 45.3
    }
  },
  "summary": "Test completed successfully"
}
```

### 5.4 K6 설정 프로퍼티

`application.properties` 또는 `application.yml`에서 K6 관련 설정을 할 수 있습니다:

```properties
# K6 Runner 설정
k6.runner.docker-host=unix:///var/run/docker.sock
k6.runner.paths.base-dir=/tmp/k6-runs
k6.runner.paths.container-mount-dir=/k6
k6.runner.controller.enabled=true

# K6 경로 설정 (K6Runner에서 사용)
k6.path.internal=/tmp/k6-runs
k6.path.host=/tmp/k6-runs
```

## 6. 테스트 작성

### 6.1 자동 설정 테스트

```java
@SpringBootTest(classes = {OurotailAutoConfigurationTest.TestConfig.class, OurotailAutoConfiguration.class})
class OurotailAutoConfigurationTest {

    @Configuration
    static class TestConfig {
        // 테스트용 설정
    }

    @Test
    void contextLoads() {
        // 자동 설정이 정상적으로 로드되는지 확인
    }
}
```

### 6.2 통합 테스트

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TestControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testHelloEndpoint() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/test/hello", Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("message")).isEqualTo("Hello from Ourotail SDK!");
    }
}
```

### 6.3 K6 관련 테스트

```java
@SpringBootTest
class K6RunnerTest {

    @Autowired
    private K6Runner k6Runner;

    @Test
    @Disabled("Docker 환경이 필요한 테스트")
    void testK6Runner() throws Exception {
        K6OptionDto options = new K6OptionDto();
        options.setTargetUrl("http://localhost:8080/api/test/hello");
        options.setVirtualUsers(5);
        options.setDuration(10);
        options.setMethod("GET");

        String result = k6Runner.runK6Test(options);
        
        assertThat(result).isNotNull();
        assertThat(result).contains("metrics");
    }
}

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class K6ControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @Disabled("Docker 환경이 필요한 테스트")
    void testK6RunEndpoint() {
        K6OptionDto options = new K6OptionDto();
        options.setTargetUrl("http://localhost:8080/api/test/hello");
        options.setVirtualUsers(5);
        options.setDuration(10);
        options.setMethod("GET");

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/k6/run", options, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }
}
```

## 7. 버전 관리

### 7.1 버전 변경

`build.gradle`에서 버전을 변경:

```gradle
version = '0.0.2-SNAPSHOT'  // 새 버전으로 변경
```

### 7.2 재배포

```bash
./gradlew clean publishToMavenLocal
```

## 8. 문제 해결

### 8.1 자동 설정이 작동하지 않는 경우

1. `AutoConfiguration.imports` 파일이 올바른 위치에 있는지 확인
2. 클래스패스에 필요한 의존성이 있는지 확인
3. `@ConditionalOnClass` 조건이 만족되는지 확인

### 8.2 K6 관련 문제 해결

#### Docker 연결 문제
```bash
# Docker 데몬이 실행 중인지 확인
docker ps

# Docker 소켓 권한 확인 (Linux/macOS)
ls -la /var/run/docker.sock

# Windows의 경우 Docker Desktop이 실행 중인지 확인
```

#### K6 컨테이너 실행 실패
```properties
# application.properties에서 Docker 호스트 설정
k6.runner.docker-host=unix:///var/run/docker.sock
# 또는 Windows의 경우
k6.runner.docker-host=tcp://localhost:2375
```

#### K6 스크립트 생성 실패
- `k6.runner.paths.base-dir` 경로에 쓰기 권한이 있는지 확인
- 디스크 공간이 충분한지 확인

### 8.3 의존성 충돌

```gradle
// 특정 버전 강제 지정
configurations.all {
    resolutionStrategy {
        force 'org.springframework:spring-web:6.0.13'
    }
}
```

### 8.4 빌드 실패

```bash
# 상세한 빌드 정보 확인
./gradlew build --info

# 의존성 트리 확인
./gradlew dependencies
```

## 9. 고급 설정

### 9.1 프로파일별 설정

```gradle
publishing {
    publications {
        maven(MavenPublication) {
            from components.java
            
            // 프로파일별 설정
            if (project.hasProperty('release')) {
                version = '1.0.0'
            } else {
                version = '1.0.0-SNAPSHOT'
            }
        }
    }
}
```

### 9.2 서명 설정

```gradle
apply plugin: 'signing'

signing {
    sign publishing.publications.maven
}
```

## 10. 배포 체크리스트

### 기본 SDK 설정
- [ ] `build.gradle` 설정 완료 (Docker Java 의존성 포함)
- [ ] 자동 설정 클래스 작성 (OurotailAutoConfiguration, K6RunnerAutoConfiguration)
- [ ] `AutoConfiguration.imports` 파일 등록 (두 자동 설정 클래스 모두)
- [ ] K6RunnerProperties 설정 프로퍼티 클래스 작성
- [ ] 테스트 코드 작성 및 통과
- [ ] 로컬 빌드 성공
- [ ] 로컬 Maven 저장소 배포 성공

### K6 기능 테스트
- [ ] Docker 환경 준비 (Docker Desktop 실행)
- [ ] K6 컨트롤러 API 테스트 성공
- [ ] K6 성능 테스트 실행 성공
- [ ] 설정 프로퍼티 동작 확인

### 통합 테스트
- [ ] 다른 프로젝트에서 의존성 추가 및 테스트 성공
- [ ] 자동 설정이 정상적으로 활성화되는지 확인
- [ ] K6 기능이 정상적으로 작동하는지 확인

## 결론

이 가이드를 따라하면 Spring Boot 기반의 라이브러리를 로컬 Maven 저장소에 성공적으로 배포할 수 있습니다. 실제 프로덕션 환경에서는 Maven Central이나 사설 저장소를 사용하는 것을 권장합니다.

라이브러리 개발 시에는 항상 테스트를 작성하고, 문서화를 철저히 하며, 버전 관리를 체계적으로 하는 것이 중요합니다.

## K6 기능 추가 요약

새로 추가된 K6 기능은 다음과 같습니다:

### 🔧 **새로운 컴포넌트들:**
- `K6Runner`: Docker를 사용하여 K6 성능 테스트를 실행하는 핵심 서비스
- `K6Controller`: K6 성능 테스트를 위한 REST API 엔드포인트
- `K6RunnerProperties`: K6 관련 설정 프로퍼티 관리
- `K6OptionDto`: K6 테스트 옵션을 담는 데이터 전송 객체

### 📦 **추가된 의존성:**
- `docker-java`: Docker API 클라이언트
- `docker-java-transport-httpclient5`: HTTP 클라이언트 전송 계층
- `lombok`: 보일러플레이트 코드 감소

### ⚙️ **자동 설정:**
- `K6RunnerAutoConfiguration`: DockerClient 빈 제공
- `OurotailAutoConfiguration`: K6Runner, K6Controller 빈 등록

### 🚀 **새로운 API 엔드포인트:**
- `POST /k6/run`: K6 성능 테스트 실행

이제 SDK 사용자는 간단한 REST API 호출로 K6 성능 테스트를 실행할 수 있습니다!

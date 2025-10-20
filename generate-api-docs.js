#!/usr/bin/env node

/**
 * OpenAPI 정적 문서 생성 스크립트
 * 
 * 이 스크립트는 다음 작업을 수행합니다:
 * 1. Spring Boot 애플리케이션에서 OpenAPI JSON 스펙을 가져옵니다
 * 2. React Swagger UI를 사용하여 정적 HTML 문서를 생성합니다
 * 3. Try it out과 Test 기능이 포함된 완전한 API 문서를 만듭니다
 */

const fs = require('fs');
const path = require('path');
const https = require('https');
const http = require('http');

// 설정
const CONFIG = {
    // Spring Boot 애플리케이션 URL (실행 중이어야 함)
    apiUrl: 'http://localhost:8080',
    openApiEndpoint: '/v3/api-docs',
    
    // 출력 디렉토리
    outputDir: './docs',
    
    // Swagger UI 설정 - 실시간 API 호출 방식
    swaggerUiConfig: {
        url: 'http://localhost:8080/v3/api-docs',  // 실시간으로 API 스펙 가져오기
        dom_id: '#swagger-ui',
        deepLinking: true,
        presets: [
            'SwaggerUIBundle.presets.apis',
            'SwaggerUIStandalonePreset'
        ],
        plugins: [
            'SwaggerUIBundle.plugins.DownloadUrl'
        ],
        layout: 'StandaloneLayout',
        tryItOutEnabled: true,
        requestInterceptor: (req) => {
            // CORS 문제 해결을 위한 헤더 추가
            req.headers['Access-Control-Allow-Origin'] = '*';
            return req;
        },
        responseInterceptor: (res) => {
            // 응답 처리
            return res;
        }
    }
};

/**
 * HTTP 요청을 수행하는 헬퍼 함수
 */
function makeRequest(url) {
    return new Promise((resolve, reject) => {
        const client = url.startsWith('https') ? https : http;
        
        client.get(url, (res) => {
            let data = '';
            
            res.on('data', (chunk) => {
                data += chunk;
            });
            
            res.on('end', () => {
                try {
                    const jsonData = JSON.parse(data);
                    resolve(jsonData);
                } catch (error) {
                    reject(new Error(`JSON 파싱 오류: ${error.message}`));
                }
            });
        }).on('error', (error) => {
            reject(new Error(`HTTP 요청 오류: ${error.message}`));
        });
    });
}

/**
 * OpenAPI JSON 스펙을 가져옵니다
 */
async function fetchOpenApiSpec() {
    const url = `${CONFIG.apiUrl}${CONFIG.openApiEndpoint}`;
    console.log(`OpenAPI 스펙을 가져오는 중: ${url}`);
    
    try {
        const spec = await makeRequest(url);
        console.log('✅ OpenAPI 스펙을 성공적으로 가져왔습니다');
        return spec;
    } catch (error) {
        console.error('❌ OpenAPI 스펙을 가져오는데 실패했습니다:', error.message);
        console.log('\n💡 해결 방법:');
        console.log('1. Spring Boot 애플리케이션이 실행 중인지 확인하세요');
        console.log('2. 애플리케이션이 http://localhost:8080에서 실행 중인지 확인하세요');
        console.log('3. SpringDoc OpenAPI 의존성이 추가되어 있는지 확인하세요');
        process.exit(1);
    }
}

/**
 * 출력 디렉토리를 생성합니다
 */
function createOutputDirectory() {
    if (!fs.existsSync(CONFIG.outputDir)) {
        fs.mkdirSync(CONFIG.outputDir, { recursive: true });
        console.log(`📁 출력 디렉토리를 생성했습니다: ${CONFIG.outputDir}`);
    }
}

/**
 * OpenAPI JSON 파일을 저장합니다
 */
function saveOpenApiSpec(spec) {
    const filePath = path.join(CONFIG.outputDir, 'api-docs.json');
    fs.writeFileSync(filePath, JSON.stringify(spec, null, 2));
    console.log(`💾 OpenAPI 스펙을 저장했습니다: ${filePath}`);
}

/**
 * React Swagger UI HTML 템플릿을 생성합니다 (실시간 API 호출 방식)
 */
function generateSwaggerUiHtml() {
    const htmlTemplate = `<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Ourotail SDK API Documentation</title>
    
    <!-- Swagger UI CSS -->
    <link rel="stylesheet" type="text/css" href="https://unpkg.com/swagger-ui-dist@5.9.0/swagger-ui.css" />
    
    <!-- Custom CSS -->
    <style>
        body {
            margin: 0;
            background-color: #fafafa;
        }
        
        .swagger-ui .topbar {
            background-color: #2c3e50;
        }
        
        .swagger-ui .topbar .download-url-wrapper {
            display: none;
        }
        
        .swagger-ui .info {
            margin: 20px 0;
        }
        
        .swagger-ui .info .title {
            color: #2c3e50;
        }
        
        .swagger-ui .scheme-container {
            background: #fff;
            border: 1px solid #e0e0e0;
            border-radius: 4px;
            padding: 10px;
            margin: 20px 0;
        }
        
        .custom-header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 20px;
            text-align: center;
            margin-bottom: 20px;
        }
        
        .custom-header h1 {
            margin: 0;
            font-size: 2.5em;
            font-weight: 300;
        }
        
        .custom-header p {
            margin: 10px 0 0 0;
            font-size: 1.2em;
            opacity: 0.9;
        }
        
        .api-status {
            background: #e8f5e8;
            border: 1px solid #4caf50;
            border-radius: 4px;
            padding: 10px;
            margin: 10px 0;
            text-align: center;
        }
        
        .api-status.error {
            background: #ffebee;
            border-color: #f44336;
            color: #c62828;
        }
        
        .api-status::before {
            content: "🔄 ";
            font-weight: bold;
        }
        
        .api-status.error::before {
            content: "❌ ";
        }
        
        .loading {
            text-align: center;
            padding: 40px;
            font-size: 1.2em;
            color: #666;
        }
        
        .loading::before {
            content: "⏳ ";
            animation: spin 1s linear infinite;
        }
        
        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
    </style>
</head>
<body>
    <div class="custom-header">
        <h1>Ourotail SDK API</h1>
        <p>K6 성능 테스트 및 기본 테스트 API 문서</p>
    </div>
    
    <div id="api-status" class="api-status">
        Spring Boot 애플리케이션에서 실시간으로 API 스펙을 가져오는 중...
    </div>
    
    <div id="swagger-ui">
        <div class="loading">API 스펙을 로딩 중입니다...</div>
    </div>
    
    <!-- Swagger UI JavaScript -->
    <script src="https://unpkg.com/swagger-ui-dist@5.9.0/swagger-ui-bundle.js"></script>
    <script src="https://unpkg.com/swagger-ui-dist@5.9.0/swagger-ui-standalone-preset.js"></script>
    
    <script>
        window.onload = function() {
            // Swagger UI 초기화
            const ui = SwaggerUIBundle({
                ${JSON.stringify(CONFIG.swaggerUiConfig, null, 16).replace(/"/g, '')},
                onComplete: function() {
                    console.log('Swagger UI가 로드되었습니다');
                    
                    // Try it out 버튼 활성화
                    setTimeout(() => {
                        const tryButtons = document.querySelectorAll('.try-out__btn');
                        tryButtons.forEach(btn => {
                            if (btn.textContent.includes('Try it out')) {
                                btn.click();
                            }
                        });
                    }, 1000);
                }
            });
            
            // 전역 객체로 설정 (디버깅용)
            window.ui = ui;
        };
        
        // CORS 문제 해결을 위한 전역 설정
        window.addEventListener('beforeunload', function() {
            // 페이지를 떠날 때 정리 작업
        });
    </script>
</body>
</html>`;

    const filePath = path.join(CONFIG.outputDir, 'index.html');
    fs.writeFileSync(filePath, htmlTemplate);
    console.log(`📄 실시간 API 호출 방식 Swagger UI HTML을 생성했습니다: ${filePath}`);
}

/**
 * README 파일을 생성합니다
 */
function generateReadme() {
    const readmeContent = `# Ourotail SDK API Documentation

이 디렉토리는 Ourotail SDK의 **실시간 API 호출 방식** 문서를 포함합니다.

## 🚀 실시간 API 호출 방식

이 문서는 정적 파일이 아닌 **실시간으로 Spring Boot 애플리케이션에서 API 스펙을 가져와서** 화면을 생성합니다.

### 작동 원리
1. 브라우저에서 \`index.html\` 로드
2. JavaScript가 \`http://localhost:8080/v3/api-docs\`를 호출
3. 실시간으로 OpenAPI 스펙을 가져와서 Swagger UI 렌더링
4. API 스펙이 변경되면 자동으로 반영됨

## 파일 구조

- \`index.html\` - 실시간 API 호출 방식 Swagger UI 문서
- \`api-docs.json\` - 백업용 OpenAPI 3.0 스펙 파일
- \`README.md\` - 이 파일

## 사용 방법

### 1. 사전 준비

**중요**: Spring Boot 애플리케이션이 실행 중이어야 합니다!

\`\`\`bash
# Spring Boot 애플리케이션 실행
./gradlew bootRun

# 또는 다른 터미널에서
java -jar build/libs/ourotail-0.0.1-SNAPSHOT.jar
\`\`\`

### 2. 문서 서빙

\`\`\`bash
# Python 3을 사용한 간단한 HTTP 서버
python -m http.server 8000

# 또는 Node.js를 사용한 HTTP 서버
npx http-server -p 8000
\`\`\`

그 후 브라우저에서 \`http://localhost:8000\`에 접속하세요.

### 3. API 테스트

Swagger UI에서 다음 기능을 사용할 수 있습니다:

- **Try it out**: 각 API 엔드포인트를 직접 테스트
- **Execute**: 실제 API 호출 실행
- **Response**: API 응답 결과 확인

### 4. API 엔드포인트

#### 기본 테스트 API
- \`GET /api/test/hello\` - Hello API
- \`POST /api/test/echo\` - Echo API  
- \`GET /api/test/info\` - SDK 정보 조회

#### K6 성능 테스트 API
- \`POST /k6/run\` - K6 성능 테스트 실행

## 주의사항

- API를 테스트하려면 Spring Boot 애플리케이션이 실행 중이어야 합니다
- K6 테스트를 실행하려면 Docker가 설치되어 있어야 합니다
- CORS 문제가 발생할 수 있으므로, 필요시 브라우저의 CORS 정책을 비활성화하세요

## 문제 해결

### CORS 오류
브라우저에서 CORS 오류가 발생하는 경우:
1. Chrome에서 \`--disable-web-security\` 플래그로 실행
2. 또는 Firefox에서 \`about:config\`에서 \`security.fileuri.strict_origin_policy\`를 \`false\`로 설정

### API 연결 실패
1. Spring Boot 애플리케이션이 실행 중인지 확인
2. 포트 번호가 올바른지 확인 (기본값: 8080)
3. 방화벽 설정 확인

## 문서 재생성

API 스펙이 변경된 경우 다음 명령어로 문서를 재생성하세요:

\`\`\`bash
node generate-api-docs.js
\`\`\`
`;

    const filePath = path.join(CONFIG.outputDir, 'README.md');
    fs.writeFileSync(filePath, readmeContent);
    console.log(`📖 README 파일을 생성했습니다: ${filePath}`);
}

/**
 * 메인 실행 함수
 */
async function main() {
    console.log('🚀 OpenAPI 정적 문서 생성을 시작합니다...\n');
    
    try {
        // 1. OpenAPI 스펙 가져오기
        const spec = await fetchOpenApiSpec();
        
        // 2. 출력 디렉토리 생성
        createOutputDirectory();
        
        // 3. OpenAPI JSON 저장
        saveOpenApiSpec(spec);
        
        // 4. Swagger UI HTML 생성
        generateSwaggerUiHtml();
        
        // 5. README 생성
        generateReadme();
        
        console.log('\n✅ 정적 문서 생성이 완료되었습니다!');
        console.log(`\n📂 생성된 파일들:`);
        console.log(`   - ${path.join(CONFIG.outputDir, 'index.html')}`);
        console.log(`   - ${path.join(CONFIG.outputDir, 'api-docs.json')}`);
        console.log(`   - ${path.join(CONFIG.outputDir, 'README.md')}`);
        
        console.log(`\n🌐 문서 보기:`);
        console.log(`   1. 터미널에서 다음 명령어 실행:`);
        console.log(`      cd ${CONFIG.outputDir}`);
        console.log(`      python -m http.server 8000`);
        console.log(`   2. 브라우저에서 http://localhost:8000 접속`);
        
    } catch (error) {
        console.error('\n❌ 문서 생성 중 오류가 발생했습니다:', error.message);
        process.exit(1);
    }
}

// 스크립트 실행
if (require.main === module) {
    main();
}

module.exports = {
    fetchOpenApiSpec,
    generateSwaggerUiHtml,
    CONFIG
};

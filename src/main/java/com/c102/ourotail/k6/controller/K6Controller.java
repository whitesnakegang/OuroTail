package com.c102.ourotail.k6.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.c102.ourotail.k6.K6Runner;
import com.c102.ourotail.k6.dto.K6OptionDto;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "K6 Performance Test", description = "K6 성능 테스트 실행 API")
public class K6Controller {

    private final K6Runner k6Runner;
    
    @Operation(summary = "K6 성능 테스트 실행", description = "Docker를 사용하여 K6 성능 테스트를 실행합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성능 테스트가 성공적으로 완료됨",
                    content = @Content(mediaType = "text/plain",
                            examples = @ExampleObject(value = """
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
                                    """))),
            @ApiResponse(responseCode = "500", description = "테스트 실행 중 오류 발생",
                    content = @Content(mediaType = "text/plain",
                            examples = @ExampleObject(value = "Error running k6 test: Docker connection failed")))
    })
    @PostMapping("/k6/run")
    public ResponseEntity<String> runK6Test(@RequestBody @Schema(description = "K6 테스트 옵션") K6OptionDto options) {
        try {
            // The service method will now accept options
            String result = k6Runner.runK6Test(options);
            return ResponseEntity.ok().header("Content-Type", "text/plain").body(result);
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(500).body("Error running k6 test: " + e.getMessage());
        }
    }
}

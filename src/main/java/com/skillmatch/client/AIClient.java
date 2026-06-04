package com.skillmatch.client;

import com.skillmatch.domain.dto.AIMatchRequest;
import com.skillmatch.domain.dto.AIMatchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;

/**
 * AI 引擎 HTTP 客户端 — 调用 Python FastAPI 服务
 */
@Slf4j
@Component
public class AIClient {

    private final RestTemplate rest;

    @Value("${ai.engine.url:http://localhost:8000}")
    private String baseUrl;

    public AIClient() {
        this.rest = new RestTemplate();
        // 显式注册 Jackson 转换器，确保 Java 对象能序列化为 JSON
        this.rest.getMessageConverters().add(0, new MappingJackson2HttpMessageConverter());
    }

    /**
     * 批量语义匹配 — 同步调用，超时/异常返回 null（上游降级）
     */
    public AIMatchResponse batchMatch(AIMatchRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<AIMatchRequest> entity = new HttpEntity<>(request, headers);

            log.info("AI 请求体: {}", new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request));

            ResponseEntity<AIMatchResponse> resp = rest.exchange(
                    baseUrl + "/api/ai/match",
                    HttpMethod.POST,
                    entity,
                    AIMatchResponse.class
            );
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                log.info("AI match: candidates={}, cost={}ms",
                        request.getCandidates().size(), resp.getBody().getCostMs());
                return resp.getBody();
            }
        } catch (Exception e) {
            log.warn("AI 匹配失败，降级使用规则排序: {}", e.getMessage());
            if (e instanceof org.springframework.web.client.HttpClientErrorException) {
                log.warn("AI 响应体: {}", ((org.springframework.web.client.HttpClientErrorException) e).getResponseBodyAsString());
            }
        }
        return null;
    }

    /**
     * 异步批量匹配 — 不阻塞主线程
     */
    public CompletableFuture<AIMatchResponse> batchMatchAsync(AIMatchRequest request) {
        return CompletableFuture.supplyAsync(() -> batchMatch(request));
    }
}

package com.skillmatch.client;

import com.skillmatch.domain.dto.AIMatchRequest;
import com.skillmatch.domain.dto.AIMatchResponse;
import com.skillmatch.domain.dto.MatchExplainRequest;
import com.skillmatch.domain.dto.MatchExplainResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.concurrent.CompletableFuture;

/**
 * AI 引擎 HTTP 客户端 — 调用 Python FastAPI 服务
 */
@Slf4j
@Component
public class AIClient {

    private final RestTemplate rest;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${ai.engine.url:http://localhost:8000}")
    private String baseUrl;

    @Value("${ai.llm.base-url:https://api.deepseek.com/v1}")
    private String llmBaseUrl;

    @Value("${ai.llm.model:deepseek-chat}")
    private String llmModel;

    public AIClient() {
        // 配置连接和读取超时，防止AI引擎挂起时无限阻塞
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);  // 连接超时3秒
        factory.setReadTimeout(10000);    // 读取超时10秒
        this.rest = new RestTemplate(factory);
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

            log.info("AI 请求体: {}", MAPPER.writeValueAsString(request));

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

    /**
     * LLM 匹配解释 — 生成自然语言匹配原因，超时/异常返回 null（降级）
     */
    public MatchExplainResponse explainMatch(MatchExplainRequest request) {
        // 注入 YAML 中的 LLM 配置到请求体（API Key 在 AI Engine 侧，不经过 HTTP）
        request.setLlmBaseUrl(llmBaseUrl);
        request.setLlmModel(llmModel);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<MatchExplainRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<MatchExplainResponse> resp = rest.exchange(
                    baseUrl + "/api/ai/match/explain",
                    HttpMethod.POST,
                    entity,
                    MatchExplainResponse.class
            );
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                log.info("AI explain: {} ↔ {}, reason={}",
                        request.getSourceName(), request.getTargetName(),
                        resp.getBody().getReason());
                return resp.getBody();
            }
        } catch (Exception e) {
            log.warn("AI 匹配解释失败，降级不显示: {}", e.getMessage());
        }
        return null;
    }
}

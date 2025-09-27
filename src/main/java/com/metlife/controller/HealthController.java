package com.metlife.controller;


import com.metlife.DTO.HealthForm;
import com.metlife.repository.HealthFormRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final HealthFormRepository repository;
    private final WebClient webClient;

    @Value("${azure.openai.endpoint}")
    private String azureEndpoint;

    @Value("${azure.openai.key}")
    private String azureKey;

    @Value("${azure.openai.deployment}")
    private String deployment;

    public HealthController(HealthFormRepository repository) {
        this.repository = repository;
        this.webClient = WebClient.builder()
                .baseUrl("") // we’ll set full URL dynamically
                .build();
    }

    // 1️⃣ Submit form
    @PostMapping("/submit")
    public HealthForm submitForm(@RequestBody HealthForm form) {
        return repository.save(form);
    }

    // 2️⃣ Dashboard data
    @GetMapping("/dashboard")
    public List<HealthForm> getDashboardData() {
        return repository.findAll();
    }

    // 3️⃣ Generate insights from Azure OpenAI
    @PostMapping(value="/generate-insights", produces=MediaType.APPLICATION_JSON_VALUE)
    public Mono<String> generateInsights(@RequestBody HealthForm form) {

        repository.save(form);

        String prompt = buildPrompt(form);

        String url = azureEndpoint + "/openai/deployments/" + deployment + "/chat/completions?api-version=2025-01-01-preview";

        return webClient.post()
                .uri(url)
                .header("api-key", azureKey)
                .header("Content-Type", "application/json")
                .bodyValue(Map.of(
                        "messages", List.of(
                                Map.of("role","system","content","You are a helpful health assistant."),
                                Map.of("role","user","content", prompt)
                        ),
                        "max_tokens", 1000,
                        "temperature", 0.3
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    // Extract GPT content
                    List choices = (List) response.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map choice0 = (Map) choices.get(0);
                        Map message = (Map) choice0.get("message");
                        return (String) message.get("content");
                    }
                    return "No response from AI";
                });
    }

    private String buildPrompt(HealthForm f) {
        return String.format(
                "You are a health assistant. Analyze the following patient data and give a JSON output with risk score (0-1), risk category (low/moderate/high), predicted conditions, and lifestyle recommendations.\n\n" +
                        "{\n" +
                        " \"name\": \"%s\",\n" +
                        " \"age\": %d,\n" +
                        " \"gender\": \"%s\",\n" +
                        " \"height\": %.1f,\n" +
                        " \"weight\": %.1f,\n" +
                        " \"systolic\": %d,\n" +
                        " \"diastolic\": %d,\n" +
                        " \"heart_rate\": %d,\n" +
                        " \"smoking_status\": \"%s\",\n" +
                        " \"alcohol_consumption\": \"%s\",\n" +
                        " \"major_illness\": \"%s\",\n" +
                        " \"current_medications\": \"%s\",\n" +
                        " \"weekly_steps\": %d,\n" +
                        " \"weekly_sleep_hours\": %.1f\n" +
                        "}",
                f.getName(), f.getAge(), f.getGender(), f.getHeight(), f.getWeight(),
                f.getSystolicBP(), f.getDiastolicBP(), f.getHeartRate(),
                f.getSmokingStatus(), f.getAlcoholConsumption(), f.getMajorIllness(),
                f.getCurrentMedications(), f.getWeeklySteps(), f.getWeeklySleepHours()
        );
    }
}

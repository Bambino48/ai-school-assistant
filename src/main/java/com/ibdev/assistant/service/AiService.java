package com.ibdev.assistant.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    public String askAi(String message) {

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Corps de la requête Gemini
        Map<String, Object> request = new HashMap<>();

        List<Map<String, Object>> contents = new ArrayList<>();

        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");

        List<Map<String, String>> parts = new ArrayList<>();
        Map<String, String> textPart = new HashMap<>();

        // Prompt métier
        textPart.put("text", "Tu es un assistant d'orientation scolaire. Réponds simplement.\n\nQuestion: " + message);

        parts.add(textPart);
        userMessage.put("parts", parts);

        contents.add(userMessage);

        request.put("contents", contents);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        // URL avec API KEY
        String fullUrl = apiUrl + "?key=" + apiKey;

        ResponseEntity<Map> response = restTemplate.postForEntity(fullUrl, entity, Map.class);

        // Extraction réponse
        try {
            List candidates = (List) response.getBody().get("candidates");
            Map candidate = (Map) candidates.get(0);

            Map content = (Map) candidate.get("content");
            List partsResponse = (List) content.get("parts");

            Map textMap = (Map) partsResponse.get(0);

            return textMap.get("text").toString();

        } catch (Exception e) {
            return "Erreur lors de la réponse IA";
        }
    }
}
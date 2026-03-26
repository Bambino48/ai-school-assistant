package com.ibdev.assistant.service;

import com.ibdev.assistant.entity.Message;
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
        return askAiWithContext(Collections.emptyList(), message);
    }

    public String askAiWithContext(List<Message> history, String newMessage) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = new HashMap<>();
            List<Map<String, Object>> contents = new ArrayList<>();

            StringBuilder context = new StringBuilder();
            context.append("Tu es un assistant d'orientation scolaire.\n");
            context.append("Réponds clairement, simplement et de manière structurée.\n");
            context.append("Donne des conseils pratiques adaptés au profil de l'utilisateur.\n\n");

            if (history != null && !history.isEmpty()) {
                context.append("Voici l'historique de la conversation :\n\n");

                for (Message msg : history) {
                    String role = "USER".equalsIgnoreCase(msg.getRole()) ? "Utilisateur" : "Assistant";
                    context.append(role)
                            .append(" : ")
                            .append(msg.getContent())
                            .append("\n");
                }

                context.append("\n");
            }

            context.append("Nouvelle question de l'utilisateur : ")
                    .append(newMessage);

            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");

            List<Map<String, String>> parts = new ArrayList<>();
            Map<String, String> textPart = new HashMap<>();
            textPart.put("text", context.toString());

            parts.add(textPart);
            userMessage.put("parts", parts);

            contents.add(userMessage);
            request.put("contents", contents);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            String fullUrl = apiUrl + "?key=" + apiKey;

            ResponseEntity<Map> response = restTemplate.postForEntity(fullUrl, entity, Map.class);

            if (response.getBody() == null) {
                return "Aucune réponse reçue depuis Gemini.";
            }

            List candidates = (List) response.getBody().get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                return "Aucune réponse générée par Gemini.";
            }

            Map candidate = (Map) candidates.get(0);
            Map content = (Map) candidate.get("content");
            if (content == null) {
                return "Réponse Gemini invalide : contenu manquant.";
            }

            List partsResponse = (List) content.get("parts");
            if (partsResponse == null || partsResponse.isEmpty()) {
                return "Réponse Gemini invalide : texte manquant.";
            }

            Map textMap = (Map) partsResponse.get(0);
            Object text = textMap.get("text");

            return text != null ? text.toString() : "Réponse vide générée par Gemini.";

        } catch (Exception e) {
            e.printStackTrace();
            return "Erreur lors de la communication avec Gemini.";
        }
    }
}
package com.ibdev.assistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibdev.assistant.entity.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class AiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public String askAiWithContext(List<Message> history, String newMessage) {
        StringBuilder finalText = new StringBuilder();

        try {
            consumeGeminiStream(history, newMessage, finalText::append);
            if (finalText.isEmpty()) {
                return "Aucune réponse générée par Gemini.";
            }
            return finalText.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "Erreur lors de la communication avec Gemini.";
        }
    }

    public StreamingResponseBody streamAiWithContext(
            List<Message> history,
            String newMessage,
            Consumer<String> onComplete
    ) {
        return outputStream -> {
            StringBuilder fullText = new StringBuilder();

            try {
                consumeGeminiStream(history, newMessage, delta -> {
                    if (delta == null || delta.isBlank()) {
                        return;
                    }

                    fullText.append(delta);
                    try {
                        writeChunkGradually(outputStream, delta);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

                if (fullText.isEmpty()) {
                    String fallback = "Aucune réponse générée par Gemini.";
                    fullText.append(fallback);
                    writeChunk(outputStream, fallback);
                }

                writeDone(outputStream);
                onComplete.accept(fullText.toString());

            } catch (Exception e) {
                e.printStackTrace();
                String errorMessage = "Erreur lors de la communication avec Gemini.";

                try {
                    if (fullText.isEmpty()) {
                        writeChunk(outputStream, errorMessage);
                    }
                    writeDone(outputStream);
                } catch (Exception ignored) {
                }

                onComplete.accept(errorMessage);
            }
        };
    }

    private void consumeGeminiStream(
            List<Message> history,
            String newMessage,
            Consumer<String> onDelta
    ) throws Exception {
        String requestBody = buildRequestBody(history, newMessage);
        String streamUrl = buildStreamUrl();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(streamUrl))
                .timeout(Duration.ofMinutes(2))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<java.io.InputStream> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofInputStream()
        );

        if (response.statusCode() >= 400) {
            throw new RuntimeException("Erreur Gemini HTTP " + response.statusCode());
        }

        StringBuilder cumulativeText = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8)
        )) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith(":")) {
                    continue;
                }

                if (!line.startsWith("data:")) {
                    continue;
                }

                String data = line.substring(5).trim();

                if ("[DONE]".equals(data)) {
                    break;
                }

                String incomingChunk = extractTextChunk(data);
                String delta = resolveDelta(cumulativeText.toString(), incomingChunk);

                if (delta.isBlank()) {
                    continue;
                }

                cumulativeText.append(delta);
                onDelta.accept(delta);
            }
        }
    }

    private String buildRequestBody(List<Message> history, String newMessage) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();

        Map<String, Object> systemInstruction = new LinkedHashMap<>();
        systemInstruction.put("parts", List.of(
                Map.of("text", """
                        Tu es un assistant d'orientation scolaire.
                        Réponds clairement, simplement et de manière structurée.
                        Donne des conseils pratiques adaptés au profil de l'utilisateur.
                        Si c'est pertinent, propose des pistes concrètes, des formations et des débouchés.
                        """)
        ));
        body.put("system_instruction", systemInstruction);

        List<Map<String, Object>> contents = new ArrayList<>();

        if (history != null) {
            for (Message message : history) {
                contents.add(Map.of(
                        "role", mapRole(message.getRole()),
                        "parts", List.of(Map.of("text", message.getContent()))
                ));
            }
        }

        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", newMessage))
        ));

        body.put("contents", contents);

        return objectMapper.writeValueAsString(body);
    }

    private String buildStreamUrl() {
        String baseUrl = apiUrl;

        if (baseUrl.contains(":generateContent")) {
            baseUrl = baseUrl.replace(":generateContent", ":streamGenerateContent");
        } else if (!baseUrl.contains(":streamGenerateContent")) {
            baseUrl = baseUrl + ":streamGenerateContent";
        }

        if (baseUrl.contains("?")) {
            return baseUrl + "&alt=sse&key=" + apiKey;
        }

        return baseUrl + "?alt=sse&key=" + apiKey;
    }

    private String mapRole(String role) {
        if ("AI".equalsIgnoreCase(role) || "MODEL".equalsIgnoreCase(role)) {
            return "model";
        }
        return "user";
    }

    private String extractTextChunk(String jsonData) {
        try {
            JsonNode root = objectMapper.readTree(jsonData);
            JsonNode candidates = root.path("candidates");

            if (!candidates.isArray() || candidates.isEmpty()) {
                return "";
            }

            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                return "";
            }

            StringBuilder chunk = new StringBuilder();

            for (JsonNode part : parts) {
                JsonNode textNode = part.get("text");
                if (textNode != null && !textNode.isNull()) {
                    chunk.append(textNode.asText());
                }
            }

            return chunk.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String resolveDelta(String currentText, String incomingChunk) {
        if (incomingChunk == null || incomingChunk.isBlank()) {
            return "";
        }

        if (incomingChunk.startsWith(currentText)) {
            return incomingChunk.substring(currentText.length());
        }

        return incomingChunk;
    }

    private void writeChunkGradually(OutputStream outputStream, String text) throws Exception {
        List<String> pieces = splitForStreaming(text);

        for (String piece : pieces) {
            writeChunk(outputStream, piece);
            Thread.sleep(18);
        }
    }

    private List<String> splitForStreaming(String text) {
        List<String> pieces = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return pieces;
        }

        String[] tokens = text.split("(?<=\\s)|(?=\\s)");
        StringBuilder current = new StringBuilder();

        for (String token : tokens) {
            current.append(token);

            if (current.length() >= 12 || token.contains("\n")) {
                pieces.add(current.toString());
                current.setLength(0);
            }
        }

        if (!current.isEmpty()) {
            pieces.add(current.toString());
        }

        return pieces;
    }

    private void writeChunk(OutputStream outputStream, String chunk) throws Exception {
        String sseData = "data: " + chunk.replace("\n", "\\n") + "\n\n";
        outputStream.write(sseData.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    private void writeDone(OutputStream outputStream) throws Exception {
        String doneEvent = "data: [DONE]\n\n";
        outputStream.write(doneEvent.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }
}
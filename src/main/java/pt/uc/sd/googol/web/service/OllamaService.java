package pt.uc.sd.googol.web.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.HashMap;
import java.util.Map;

@Service
public class OllamaService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // URL da API local do Ollama
    private final String OLLAMA_API_URL = "http://127.0.0.1:11434/api/generate";
    // O modelo que tens instalado (confirma se tens o gemma2, llama3, etc.)
    private final String MODEL_NAME = "gemma2:2b"; 

    /**
     * Gera texto e devolve como String única (para usar no HTML)
     */
    public String generateText(String prompt) {
        try {
            // 1. Preparar o JSON do pedido
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", MODEL_NAME);
            requestBody.put("prompt", prompt);
            requestBody.put("stream", false); // Importante: false para receber tudo de uma vez

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            // 2. Enviar pedido POST
            String response = restTemplate.postForObject(OLLAMA_API_URL, requestEntity, String.class);

            // 3. Ler a resposta JSON
            JsonNode root = objectMapper.readTree(response);
            if (root.has("response")) {
                return root.get("response").asText();
            }
            
            return "Sem resposta do modelo.";

        } catch (Exception e) {
            System.err.println("Erro no Ollama: " + e.getMessage());
            return "Erro ao gerar análise: Verifique se o Ollama está a correr.";
        }
    }
}
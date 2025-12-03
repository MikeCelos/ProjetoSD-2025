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

/**
 * Serviço responsável pela integração com o modelo de Inteligência Artificial local (Ollama).
 * <p>
 * Esta classe permite gerar análises e resumos contextualizados sobre as pesquisas dos utilizadores,
 * comunicando via REST com uma instância local do Ollama.
 * <p>
 * Vantagens desta abordagem:
 * <ul>
 * <li><b>Privacidade:</b> Os dados não saem da máquina local.</li>
 * <li><b>Custo:</b> Não requer chaves de API pagas (como a OpenAI).</li>
 * <li><b>Conformidade:</b> Cumpre o requisito de "alternativa equivalente REST" do projeto.</li>
 * </ul>
 *
 * @author André Ramos 2023227306
 */
@Service
public class OllamaService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /** URL da API local do Ollama (endpoint de geração de texto). */
    private final String OLLAMA_API_URL = "http://127.0.0.1:11434/api/generate";
    
    /** * O modelo de linguagem a utilizar.
     * Deve estar instalado no sistema (comando: ollama pull gemma3:270m).
     */
    private final String MODEL_NAME = "gemma3:270m"; 

    /**
     * Gera uma resposta de texto baseada num prompt enviado para o modelo de IA.
     * <p>
     * O método constrói um pedido JSON, envia-o via POST para a API do Ollama
     * e processa a resposta para extrair apenas o texto gerado.
     * A opção "stream" é definida como false para receber a resposta completa de uma só vez.
     *
     * @param prompt A instrução ou texto a enviar para a IA (ex: "Resume o conceito de X").
     * @return Uma String com a resposta gerada pelo modelo, ou uma mensagem de erro em caso de falha.
     */
    public String generateText(String prompt) {
        try {
            // 1. Preparar o JSON do pedido
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", MODEL_NAME);
            requestBody.put("prompt", prompt);
            requestBody.put("stream", false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            // 2. Enviar pedido POST para a API REST do Ollama
            String response = restTemplate.postForObject(OLLAMA_API_URL, requestEntity, String.class);

            // 3. Ler a resposta JSON e extrair o campo "response"
            JsonNode root = objectMapper.readTree(response);
            if (root.has("response")) {
                return root.get("response").asText();
            }
            
            return "Sem resposta do modelo.";

        } catch (Exception e) {
            System.err.println("Erro no Ollama: " + e.getMessage());
            return "Erro ao gerar análise: " + e.getMessage();
        }
    }
}
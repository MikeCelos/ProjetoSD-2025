/**
 * ===============================================================
 *  Projeto GOOGOL — Meta 2
 *  Ficheiro: OllamaService.java
 * ===============================================================
 *
 *  @Resumo:
 *  Serviço responsável pela integração do sistema GOOGOL com um
 *  modelo de Inteligência Artificial executado localmente através
 *  da plataforma Ollama.
 *
 *  Este serviço permite gerar análises, explicações e resumos
 *  contextuais sobre pesquisas efetuadas pelos utilizadores,
 *  utilizando comunicação REST com uma instância local do Ollama.
 *
 *  @Papel na arquitetura:
 *  - Executa na camada Web (Spring Boot).
 *  - Atua como consumidor de um serviço REST de IA local.
 *  - Fornece capacidades de análise textual ao frontend.
 *  - Não participa diretamente na indexação ou pesquisa.
 *
 *  @Motivação para uso de IA local:
 *  <ul>
 *    <li><b>Privacidade:</b> Os dados das pesquisas não saem da máquina.</li>
 *    <li><b>Custo:</b> Não requer APIs comerciais ou chaves pagas.</li>
 *    <li><b>Autonomia:</b> O sistema funciona offline após instalação.</li>
 *    <li><b>Conformidade:</b> Cumpre o requisito de alternativa REST
 *        equivalente a serviços externos (ex.: OpenAI).</li>
 *  </ul>
 *
 *  @Tecnologias utilizadas:
 *  - Spring {@link Service} para gestão do ciclo de vida.
 *  - {@link RestTemplate} para comunicação HTTP.
 *  - Jackson ({@link ObjectMapper}) para processamento de JSON.
 *  - Ollama como backend de execução do modelo de linguagem.
 *
 *  @Modelo de linguagem:
 *  - O modelo utilizado é configurável através da constante {@code MODEL_NAME}.
 *  - Deve estar previamente instalado no sistema local:
 *      {@code ollama pull gemma3:270m}
 *
 *  @Autor:
 *   Elemento 1: André Ramos — 2023227306
 */

package pt.uc.sd.googol.web.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
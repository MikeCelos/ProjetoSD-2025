package pt.uc.sd.googol.web.controller;

import pt.uc.sd.googol.web.service.HackerNewsService;
import pt.uc.sd.googol.web.service.OllamaService;
import pt.uc.sd.googol.web.service.StatsListenerImpl;
import pt.uc.sd.googol.gateway.GatewayInterface;
import pt.uc.sd.googol.gateway.SearchResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Controlador principal da aplicação Web (Frontend Spring Boot).
 * <p>
 * Implementa o padrão MVC, processando os pedidos HTTP dos utilizadores e devolvendo
 * as vistas HTML apropriadas (Thymeleaf).
 * <p>
 * Responsabilidades:
 * <ul>
 * <li>Gerir a página inicial e os resultados de pesquisa.</li>
 * <li>Integrar com o Gateway RMI para obter dados do backend.</li>
 * <li>Coordenar os serviços de Hacker News (REST) e Ollama (IA).</li>
 * </ul>
 *
 * @author André Ramos 2023227306
 */
@Controller
public class WebController {

    // Dependências (Serviços + Gateway da Meta 1)
    private final HackerNewsService hnService;
    private final OllamaService ollamaService;
    private final GatewayInterface gateway; // Injetado automaticamente pelo RmiConfig

    /**
     * Construtor com injeção de dependências.
     * O Spring Boot injeta automaticamente os serviços e o Gateway RMI.
     *
     * @param hnService Serviço de integração com a API do Hacker News.
     * @param ollamaService Serviço de integração com o modelo de IA local (Ollama).
     * @param gateway Interface RMI para comunicação com o backend distribuído (pode ser null se offline).
     */
    @Autowired 
    public WebController(HackerNewsService hnService, 
                         OllamaService ollamaService, 
                         @Autowired(required = false) GatewayInterface gateway) {
        this.hnService = hnService;
        this.ollamaService = ollamaService;
        this.gateway = gateway;
    }

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Quando o JavaScript envia para "/app/request-stats"
     * Respondemos imediatamente para "/topic/stats"
     */
    @MessageMapping("/request-stats")
    public void sendInitialStats() {
        
        Map<String, Object> lastStats = StatsListenerImpl.getLastStats();
        
        // Envia diretamente para o tópico (todos os conectados recebem)
        messagingTemplate.convertAndSend("/topic/stats", lastStats);
    }

    /**
     * Renderiza a página inicial da aplicação.
     *
     * @return O nome do template Thymeleaf ("index").
     */
    @GetMapping("/")
    public String index() {
        return "index"; // templates/index.html
    }

    /**
     * Processa os pedidos de pesquisa.
     * <p>
     * Executa as seguintes operações:
     * <ol>
     * <li>Obtém os resultados do índice via RMI (Gateway).</li>
     * <li>Gera automaticamente um resumo inteligente sobre o termo pesquisado usando o Ollama.</li>
     * <li>Prepara os dados de paginação para a interface.</li>
     * </ol>
     *
     * @param query O termo de pesquisa inserido pelo utilizador (parâmetro 'q').
     * @param page O número da página de resultados (parâmetro 'page', default 0).
     * @param model O modelo para passar dados para a vista HTML.
     * @return O nome do template a renderizar.
     */
    @GetMapping("/search")
    public String search(@RequestParam(value = "q", required = false) String query, 
                        @RequestParam(value = "page", defaultValue = "0") int page, 
                        Model model) {

        // 1. Proteção: Se query for null (acesso direto), transformamos em string vazia
        if (query == null) {
            query = "";
        }

        List<SearchResult> results = Collections.emptyList();
        
        // 2. Só chamamos o Gateway se houver texto para pesquisar
        try {
            if (gateway != null && !query.isBlank()) {
                // A. Pesquisa Normal no Índice Distribuído (RMI)
                List<String> terms = List.of(query.split("\\s+"));
                results = gateway.search(terms, page);
                
                // B. Geração de Resumo com IA (Automático)
                try {
                    String prompt = "Resume numa frase o conceito de: " + query;
                    String analysis = ollamaService.generateText(prompt);
                    model.addAttribute("aiAnalysis", analysis);
                } catch (Exception e) {
                    // Se a IA falhar (ex: Ollama desligado), não bloqueia a pesquisa principal
                    System.err.println("Erro na IA Automática: " + e.getMessage());
                }
            }
        } catch (Exception e)  {
            model.addAttribute("error", "Erro na pesquisa: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Preencher o modelo para o Thymeleaf
        model.addAttribute("query", query);
        model.addAttribute("results", results);
        
        // Lógica de paginação para os botões "Anterior/Seguinte" funcionarem
        model.addAttribute("totalResults", results.isEmpty() ? 0 : "10+");
        model.addAttribute("currentPage", page);
        // Se a página estiver cheia (10 itens), assumimos que há mais resultados
        int totalPages = (results.size() >= 10) ? page + 2 : page + 1;
        model.addAttribute("totalPages", totalPages);

        return "index";
    }

    /**
     * Processa a submissão manual de um URL para indexação.
     * Envia o URL para a fila prioritária do backend via RMI.
     *
     * @param url O URL a indexar.
     * @param redirectAttributes Atributos para passar mensagens flash (sucesso/erro) após o redirect.
     * @return Redirecionamento para a página de pesquisa.
     */
    @PostMapping("/index")
    public String addUrl(@RequestParam("url") String url, RedirectAttributes redirectAttributes) {
        try {
            if (gateway != null) {
                boolean ok = gateway.indexUrl(url);
                if (ok) {
                    redirectAttributes.addFlashAttribute("successMessage", "URL enviado para a fila: " + url);
                } else {
                    redirectAttributes.addFlashAttribute("errorMessage", "Gateway recusou o URL.");
                }
                System.out.println(" [WEB] Pedido de indexação de " + url + " -> " + ok);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Gateway indisponível (RMI Offline).");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro: " + e.getMessage());
            e.printStackTrace();
        }

        return "redirect:/search"; // Redireciona para evitar reenvio do formulário
    }

    /**
     * Aciona a integração com a API REST do Hacker News.
     * Pesquisa as "top stories", filtra as que contêm os termos da pesquisa e envia os URLs
     * para indexação no backend.
     *
     * @param q O termo de pesquisa atual.
     * @param redirectAttributes Atributos para feedback visual.
     * @return Redirecionamento para a página de pesquisa (mantendo o termo 'q').
     */
    @PostMapping("/search/hacker-news")
    public String indexHackerNews(@RequestParam(required = false) String q, 
                                  RedirectAttributes redirectAttributes) {
        
        // Delega a lógica complexa para o serviço dedicado
        int count = hnService.processTopStories(q);

        if (count > 0) {
            redirectAttributes.addFlashAttribute("successMessage", 
                count + " histórias do Hacker News enviadas para indexação!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Nenhuma história relevante encontrada no Hacker News.");
        }

        return "redirect:/search?q=" + (q != null ? q : "");
    }
}
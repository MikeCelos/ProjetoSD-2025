package pt.uc.sd.googol.web.controller;

import pt.uc.sd.googol.web.service.HackerNewsService;
import pt.uc.sd.googol.web.service.OllamaService;
import pt.uc.sd.googol.gateway.GatewayInterface;
import pt.uc.sd.googol.gateway.SearchResult;

import org.springframework.beans.factory.annotation.Autowired; // Importante para injeção
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;

@Controller
public class WebController {

    // Dependências (Serviços + Gateway da Meta 1)
    private final HackerNewsService hnService;
    private final OllamaService ollamaService;
    private final GatewayInterface gateway; // Injetado automaticamente pelo RmiConfig

    // Construtor: O Spring Boot vai preencher isto sozinho (Autowired)
    @Autowired 
    public WebController(HackerNewsService hnService, 
                         OllamaService ollamaService, 
                         @Autowired(required = false) GatewayInterface gateway) {
        this.hnService = hnService;
        this.ollamaService = ollamaService;
        this.gateway = gateway;
    }

    // --- PÁGINA INICIAL ---
    @GetMapping("/")
    public String index() {
        return "index"; // templates/index.html
    }

    // --- PESQUISAR ---
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
                List<String> terms = List.of(query.split("\\s+"));
                results = gateway.search(terms, page);
                try {
                    String prompt = "Resume numa frase o conceito de: " + query;
                    String analysis = ollamaService.generateText(prompt);
                    model.addAttribute("aiAnalysis", analysis);
                } catch (Exception e) {
                    // Se a IA falhar, não faz mal, a pesquisa continua
                    System.err.println("Erro na IA Automática: " + e.getMessage());
                }
            }
        } catch (Exception e)  {
            model.addAttribute("error", "Erro na pesquisa: " + e.getMessage());
            e.printStackTrace();
        }
        model.addAttribute("query", query);
        model.addAttribute("results", results);
        
        // Lógica de paginação para os botões "Anterior/Seguinte" funcionarem
        model.addAttribute("totalResults", results.isEmpty() ? 0 : "10+");
        model.addAttribute("currentPage", page);
        int totalPages = (results.size() >= 10) ? page + 2 : page + 1;
        model.addAttribute("totalPages", totalPages);

        return "index";
    }

    // --- INDEXAR URL MANUAL (Melhorado com Feedback visual) ---
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
                redirectAttributes.addFlashAttribute("errorMessage", "Gateway indisponível.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro: " + e.getMessage());
            e.printStackTrace();
        }

        return "redirect:/search"; // Redireciona para evitar reenvio do form
    }

    // --- NOVO: HACKER NEWS (Requisito Meta 2) ---
    @PostMapping("/search/hacker-news")
    public String indexHackerNews(@RequestParam(required = false) String q, 
                                  RedirectAttributes redirectAttributes) {
        
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
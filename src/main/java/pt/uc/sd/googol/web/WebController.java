package pt.uc.sd.googol.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import pt.uc.sd.googol.gateway.GatewayInterface;
import pt.uc.sd.googol.gateway.SearchResult;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Collections;
import java.util.List;

@Controller
public class WebController {

    private GatewayInterface gateway;  // pode ser null se o RMI falhar

    public WebController() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1100);
            this.gateway = (GatewayInterface) registry.lookup("gateway");
            System.out.println("Ligado ao Gateway via RMI!");
        } catch (Exception e) {
            System.out.println("⚠️ Não consegui ligar ao Gateway (ainda).");
            this.gateway = null;
        }
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/search")
public String search(
        @RequestParam("q") String query,
        Model model) {

    List<SearchResult> results;

    try {
        if (gateway != null) {
            // página 0 por agora (primeira página de resultados)
            results = gateway.search(Collections.singletonList(query), 0);
        } else {
            results = Collections.emptyList();
        }
    } catch (Exception e) {
        e.printStackTrace();
        results = Collections.emptyList();
    }

    model.addAttribute("query", query);
    model.addAttribute("results", results);
    model.addAttribute("totalResults", results.size());
    model.addAttribute("currentPage", 0);
    model.addAttribute("totalPages", 1);

    return "index";
}

}

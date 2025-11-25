package pt.uc.sd.googol.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pt.uc.sd.googol.gateway.GatewayInterface;
import pt.uc.sd.googol.gateway.SearchResult;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Collections;
import java.util.List;

@Controller
public class WebController {

    private GatewayInterface gateway;

    // Tenta ligar ao Gateway (lazy, só quando for preciso)
    private GatewayInterface getGateway() {
        if (gateway != null) {
            return gateway;
        }
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1100);
            gateway = (GatewayInterface) registry.lookup("gateway");
            System.out.println("WebController ligado ao Gateway via RMI!");
        } catch (Exception e) {
            System.out.println("WebController: não consegui ligar ao Gateway: " + e.getMessage());
            gateway = null;
        }
        return gateway;
    }

    @GetMapping("/")
    public String index() {
        return "index"; // templates/index.html
    }

    @GetMapping("/search")
    public String search(@RequestParam("q") String query, Model model) {

        List<SearchResult> results = Collections.emptyList();

        try {
            GatewayInterface gw = getGateway();
            if (gw != null) {
                // página 0 por enquanto
                results = gw.search(Collections.singletonList(query), 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        model.addAttribute("query", query);
        model.addAttribute("results", results);
        model.addAttribute("totalResults", results.size());
        model.addAttribute("currentPage", 0);
        model.addAttribute("totalPages", 1);

        return "index";
    }

    @PostMapping("/index")
    public String addUrl(@RequestParam("url") String url) {

        try {
            GatewayInterface gw = getGateway();
            if (gw != null) {
                boolean ok = gw.indexUrl(url);
                System.out.println(" [WEB] Pedido de indexação de " + url + " -> " + ok);
            } else {
                System.out.println(" [WEB] Gateway indisponível, não foi possível indexar " + url);
            }
        } catch (Exception e) {
            System.out.println(" [WEB] Erro ao indexar URL: " + e.getMessage());
            e.printStackTrace();
        }

        // Voltar à página inicial (padrão POST-Redirect-GET)
        return "redirect:/";
    }
}

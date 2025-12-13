package pt.uc.sd.googol.web;

import org.springframework.boot.CommandLineRunner; // Importante
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean; // Importante
import org.springframework.messaging.simp.SimpMessagingTemplate; // Importante

import pt.uc.sd.googol.gateway.GatewayInterface;
import pt.uc.sd.googol.gateway.StatsListener;
import pt.uc.sd.googol.web.service.StatsListenerImpl; // Certifica-te que criaste esta classe (passo 1)

/**
 * Ponto de entrada principal da aplicação Web (Frontend) do sistema Googol.
 * <p>
 * Esta classe é responsável por inicializar o contexto do Spring Boot,
 * arrancar o servidor Web embutido (Tomcat) na porta 8080 e desencadear
 * a configuração automática e injeção de dependências.
 *
 * @author André Ramos 2023227306
 */
@SpringBootApplication
public class GoogolWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(GoogolWebApplication.class, args);
    }

    /**
     * BEAN NOVO: Regista o WebServer como ouvinte no Gateway assim que a aplicação arranca.
     * * @param gateway A interface RMI injetada (pode ser null se o backend estiver desligado)
     * @param template O gestor de WebSockets para enviar mensagens para o HTML
     */
    @Bean
    public CommandLineRunner registerListener(GatewayInterface gateway, SimpMessagingTemplate template) {
        return args -> {
            if (gateway != null) {
                try {
                    // 1. Instancia o nosso Listener (Callback)
                    StatsListener listener = new StatsListenerImpl(template);
                    
                    // 2. Envia a referência do listener para o Gateway remoto
                    gateway.registerListener(listener);
                    
                    System.out.println(" [Reatividade] WebServer registado no Gateway com sucesso!");
                } catch (Exception e) {
                    System.err.println(" [Reatividade] Erro ao registar callback: " + e.getMessage());
                }
            } else {
                System.err.println(" [Reatividade] Gateway não encontrado. Dashboard em tempo real desligado.");
            }
        };
    }
}
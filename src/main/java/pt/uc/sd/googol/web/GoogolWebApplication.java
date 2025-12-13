/**
 * ===============================================================
 *  Projeto GOOGOL — Meta 2
 *  Ficheiro: GoogolWebApplication.java
 * ===============================================================
 *
 *  @Resumo:
 *  Classe principal (entrypoint) da aplicação Web do sistema distribuído GOOGOL.
 *  É responsável por inicializar o ecossistema Spring Boot e arrancar o Frontend
 *  Web que permite a interação dos utilizadores com o motor de pesquisa.
 *
 *  @Função no sistema:
 *  <ul>
 *    <li>Arranca o servidor Web embutido (Apache Tomcat).</li>
 *    <li>Inicializa o contexto do Spring Framework.</li>
 *    <li>Ativa a injeção de dependências e o auto-config do Spring Boot.</li>
 *    <li>Permite o funcionamento dos módulos Web, WebSocket e RMI.</li>
 *  </ul>
 *
 *  @Arquitetura:
 *  <p>
 *  Esta classe encontra-se no topo da hierarquia da camada Web e
 *  desencadeia automaticamente:
 *  </p>
 *  <ul>
 *    <li>Controllers MVC (pesquisa, indexação, análise com IA).</li>
 *    <li>Serviços de integração (Gateway RMI, Hacker News, Ollama).</li>
 *    <li>Configuração de WebSockets (STOMP).</li>
 *    <li>Serviços agendados (monitorização e estatísticas).</li>
 *  </ul>
 *
 *  @Spring Boot:
 *  A anotação {@link SpringBootApplication} agrega implicitamente:
 *  <ul>
 *    <li>{@code @Configuration} — define esta classe como fonte de configuração.</li>
 *    <li>{@code @EnableAutoConfiguration} — ativa a configuração automática.</li>
 *    <li>{@code @ComponentScan} — procura componentes neste pacote e sub-pacotes.</li>
 *  </ul>
 *
 *  @Execução:
 *  <pre>
 *  mvn spring-boot:run
 *  </pre>
 *
 *  Por omissão, a aplicação Web fica disponível em:
 *  <pre>
 *  http://localhost:8080
 *  </pre>
 *
 *  @Notas:
 *  <ul>
 *    <li>A aplicação Web é independente do backend distribuído.</li>
 *    <li>Pode iniciar mesmo que o Gateway RMI esteja offline.</li>
 *    <li>Os serviços RMI e WebSocket ligam-se dinamicamente após o arranque.</li>
 *  </ul>
 *
 *  @Autor:
 * 
 *   Elemento 1: André Ramos — 2023227306
 *   Elemento 2: Francisco Vasconcelos e Sá Pires da Silva (2023220012)
 */

package pt.uc.sd.googol.web;

import org.springframework.boot.CommandLineRunner; // Importante
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean; // Importante
import org.springframework.messaging.simp.SimpMessagingTemplate; // Importante

import pt.uc.sd.googol.gateway.GatewayInterface;
import pt.uc.sd.googol.gateway.StatsListener;
import pt.uc.sd.googol.web.service.StatsListenerImpl; // Certifica-te que criaste esta classe (passo 1)

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
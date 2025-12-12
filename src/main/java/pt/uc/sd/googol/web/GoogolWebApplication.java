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
 *   Elemento 1: André Ramos — 2023227306
 */

package pt.uc.sd.googol.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada principal da aplicação Web (Frontend) do sistema Googol.
 * <p>
 * Esta classe é responsável por inicializar o contexto do Spring Boot,
 * arrancar o servidor Web embutido (Tomcat) na porta 8080 e desencadear
 * a configuração automática e injeção de dependências de todos os componentes
 * (Controladores, Serviços, WebSocket e ligação RMI).
 * <p>
 * A anotação {@link SpringBootApplication} instrui o Spring a fazer o varrimento
 * de componentes (Component Scan) neste pacote e nos sub-pacotes, encontrando
 * automaticamente os Beans de configuração.
 *
 * @author André Ramos 2023227306
 */
@SpringBootApplication
public class GoogolWebApplication {

    /**
     * Método principal que inicia a execução da aplicação Spring Boot.
     *
     * @param args Argumentos da linha de comando passados ao iniciar o programa.
     */
    public static void main(String[] args) {
        SpringApplication.run(GoogolWebApplication.class, args);
    }
}
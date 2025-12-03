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
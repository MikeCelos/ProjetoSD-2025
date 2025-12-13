/**
 * Classe de configuração do Spring Boot responsável pela integração com o sistema RMI (Backend).
 * <p>
 * Esta classe define a criação do Bean {@link GatewayInterface}, permitindo que os controladores
 * e serviços da aplicação Web comuniquem com o Gateway do motor de busca desenvolvido na Meta 1.
 * Funciona como a "ponte" entre o Frontend (Spring Boot) e o Backend (Java RMI).
 *
 * @author Elemento 1: André Ramos (2023227306)
 */


package pt.uc.sd.googol.web.config;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import org.springframework.context.annotation.Bean; // Importa a tua interface da Meta 1
import org.springframework.context.annotation.Configuration;

import pt.uc.sd.googol.gateway.GatewayInterface;

@Configuration
public class RmiConfig {

    /**
     * Cria e disponibiliza o proxy para o objeto remoto Gateway.
     * <p>
     * O método tenta localizar o RMI Registry na porta 1100 (localhost) e efetuar o lookup
     * do serviço registado como "gateway".
     * <p>
     * <b>Tolerância a Falhas no Arranque:</b> Se a conexão falhar (ex: o Gateway da Meta 1
     * não estiver a correr), o método captura a exceção e retorna {@code null}.
     * Isto permite que a aplicação Web arranque e mostre a interface gráfica, mesmo que as
     * funcionalidades de pesquisa estejam temporariamente indisponíveis.
     *
     * @return A interface remota do Gateway, ou {@code null} se a conexão falhar.
     */
    @Bean
    public GatewayInterface gatewayInterface() {
        try {
            // Conecta ao Registry na porta 1100 (onde corre o Gateway)
            Registry registry = LocateRegistry.getRegistry("localhost", 1100);
            return (GatewayInterface) registry.lookup("gateway");
        } catch (Exception e) {
            System.err.println(" Erro ao conectar ao Gateway RMI: " + e.getMessage());
            return null; // A aplicação Web arranca, mas sem RMI
        }
    }
}
package pt.uc.sd.googol.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pt.uc.sd.googol.gateway.GatewayInterface; // Importa a tua interface da Meta 1

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

@Configuration
public class RmiConfig {

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

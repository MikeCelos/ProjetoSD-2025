/**
 * Classe principal responsável por iniciar o servidor da Fila de URLs (QueueServer).
 * <p>
 * Este componente é fundamental para a arquitetura distribuída, pois centraliza
 * a gestão de URLs a visitar. Ao correr num processo independente, permite que:
 * <ul>
 * <li>Múltiplos <b>Downloaders</b> peçam trabalho (URLs) de forma concorrente e coordenada.</li>
 * <li>O <b>Gateway</b> possa adicionar novos URLs submetidos pelos clientes (funcionalidade "Indexar URL").</li>
 * <li>O estado da fila (URLs pendentes, visitados) seja mantido num único local acessível remotamente.</li>
 * </ul>
 * <p>
 * O servidor exporta o objeto {@link URLQueue} via RMI na porta 1098 (distinta da porta dos Barrels
 * para evitar conflitos se correrem na mesma máquina).
 *
 * @author Elemento 2: Francisco Vasconcelos e Sá Pires da Silva (2023220012)
 */

package pt.uc.sd.googol.queue;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


public class QueueServer {
    
    /**
     * Ponto de entrada da aplicação QueueServer.
     * <p>
     * O método realiza as seguintes operações:
     * <ol>
     * <li>Tenta criar um RMI Registry na porta 1098.</li>
     * <li>Se a porta já estiver em uso, tenta obter o Registry existente.</li>
     * <li>Instancia a classe {@link URLQueue}.</li>
     * <li>Regista (rebind) a instância no RMI com o nome "queue".</li>
     * <li>Mantém o servidor ativo à espera de conexões de clientes RMI (Downloaders/Gateway).</li>
     * </ol>
     *
     * @param args Argumentos da linha de comando (não utilizados nesta versão).
     */
    public static void main(String[] args) {
        try {
            int port = 1098; // Porta diferente dos Barrels (1099) para evitar conflitos locais
            
            // Cria o Registry
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(port);
                System.out.println("RMI Registry criado na porta " + port);
            } catch (Exception e) {
                registry = LocateRegistry.getRegistry(port);
                System.out.println("Usando RMI Registry existente na porta " + port);
            }
            
            // Instancia e regista a Queue
            URLQueue queue = new URLQueue();
            registry.rebind("queue", queue);
            
            System.out.println("=== URL Queue Server Pronto ===");
            System.out.println("À espera de Downloaders...");
            
            
        } catch (Exception e) {
            System.err.println("Erro no QueueServer: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
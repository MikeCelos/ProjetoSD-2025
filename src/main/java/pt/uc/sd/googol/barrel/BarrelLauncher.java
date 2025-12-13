/**
 * ===============================================================
 *  Projeto GOOGOL — Meta 1
 *  Elemento 2: Francisco Vasconcelos e Sá Pires da Silva (2023220012)
 *  Ficheiro: BarrelLauncher.java
 * ===============================================================
 *
 *  @Resumo:
 *  Classe auxiliar para lançar instâncias do IndexStorageBarrel
 *  de forma independente, com IDs e portas personalizadas.
 *  Permite executar múltiplos barrels (replicação horizontal)
 *  sem alterar o código principal.
 *
 *  @Arquitetura:
 *  - Lê os argumentos de linha de comando:
 *      arg0 → barrelId
 *      arg1 (opcional) → porto RMI
 *  - Garante que existe um RMI Registry ativo na porta indicada.
 *  - Cria uma instância de IndexStorageBarrel(barrelId).
 *  - Regista o objeto remoto com o nome "barrel<id>" no registry.
 *
 *  @Execução:
 *  Exemplos:
 *      mvn exec:java -Dexec.mainClass="pt.uc.sd.googol.barrel.BarrelLauncher" -Dexec.args="0 1099"
 *      mvn exec:java -Dexec.mainClass="pt.uc.sd.googol.barrel.BarrelLauncher" -Dexec.args="1 1100"
 *
 *  @Comunicação:
 *  O Launcher não comunica diretamente com outros módulos;
 *  apenas cria o ambiente RMI e mantém o processo vivo para
 *  aceitar chamadas remotas.
 *
 *  @Plano futuro:
 *  Automatizar o arranque de múltiplos barrels via Makefile e
 *  adicionar monitorização de estado (heartbeats).
 */

package pt.uc.sd.googol.barrel;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class BarrelLauncher {

    /**
     * Ponto de entrada da aplicação Barrel.
     * Inicializa o Registry RMI e regista o objeto remoto do Barrel.
     *
     * @param args Argumentos da linha de comando. Requer o ID do barrel como primeiro argumento (ex: 0, 1).
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Uso: java BarrelLauncher <barrelId>");
            System.exit(1);
        }
        
        int barrelId = Integer.parseInt(args[0]);
        
        try {
            int port = 1099;
            
            // Obter ou criar registry
            Registry registry;
            try {
                registry = LocateRegistry.getRegistry(port);
                registry.list(); // Testar se existe
                System.out.println(" Usando registry existente na porta " + port);
            } catch (Exception e) {
                registry = LocateRegistry.createRegistry(port);
                System.out.println(" Registry criado na porta " + port);
            }
            
            // Criar e registrar este barrel
            SimpleBarrel barrel = new SimpleBarrel(barrelId);
            registry.rebind("barrel" + barrelId, barrel);
            System.out.println(" Barrel" + barrelId + " rodando");
            
            // Manter vivo
            while (true) {
                Thread.sleep(1000);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
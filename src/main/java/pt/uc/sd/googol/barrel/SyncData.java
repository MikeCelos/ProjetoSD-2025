/**
 * Objeto de Transferência de Dados (DTO) usado para a sincronização entre Barrels.
 * <p>
 * Esta classe encapsula todo o estado interno de um Barrel (páginas, índice invertido e backlinks)
 * num único objeto serializável, permitindo que seja enviado via RMI para um novo Barrel
 * que se esteja a juntar à rede ou a recuperar de uma falha (State Transfer).
 *
 * @author Elemento 1:André Ramos 2023227306
 */

package pt.uc.sd.googol.barrel;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import pt.uc.sd.googol.common.PageInfo;


public class SyncData implements Serializable {
    
    /** Mapa de URLs para objetos PageInfo com metadados (título, citação, etc.). */
    public Map<String, PageInfo> pages;
    
    /** Índice invertido: mapeia termos (palavras) para conjuntos de URLs onde aparecem. */
    public Map<String, Set<String>> index;
    
    /** Registo de backlinks: mapeia URLs para o conjunto de páginas que apontam para eles. */
    public Map<String, Set<String>> backlinks;

    /**
     * Cria um novo pacote de dados de sincronização com o estado atual do Barrel.
     *
     * @param pages Mapa atual de páginas indexadas.
     * @param index Mapa atual do índice invertido.
     * @param backlinks Mapa atual de backlinks.
     */
    public SyncData(Map<String, PageInfo> pages, 
                   Map<String, Set<String>> index, 
                   Map<String, Set<String>> backlinks) {
        this.pages = pages;
        this.index = index;
        this.backlinks = backlinks;
    }
}
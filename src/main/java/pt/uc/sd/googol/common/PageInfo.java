/**
 * Classe que representa a informação extraída de uma página Web.
 * <p>
 * Funciona como um Objeto de Transferência de Dados (DTO) que é criado pelos
 * Downloaders após o processamento de um URL e enviado para os Barrels
 * via Multicast para ser armazenado e indexado.
 * Implementa {@link Serializable} para poder viajar através da rede (RMI).
 *
 * @author Elemento 1: André Ramos (2023227306)
 */

package pt.uc.sd.googol.common;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

public class PageInfo implements Serializable {
    
    /** Versão de serialização para garantir compatibilidade entre versões da classe. */
    private static final long serialVersionUID = 1L; 
    
    private final String url;
    private final String title;
    private final String citation;
    private final Set<String> words;
    private final List<String> links;
        
    /**
     * Constrói um novo objeto com a informação extraída de uma página.
     *
     * @param url O URL completo da página visitada.
     * @param title O título da página (extraído da tag HTML title).
     * @param citation Um excerto curto do texto da página para apresentação nos resultados de pesquisa.
     * @param words Conjunto de palavras únicas encontradas na página (usado para construir o índice invertido).
     * @param links Lista de hiperligações encontradas na página (usado para crawling recursivo e cálculo de relevância).
     */
    public PageInfo(String url, String title, String citation, 
                    Set<String> words, List<String> links) {
        this.url = url;
        this.title = title;
        this.citation = citation;
        this.words = words;
        this.links = links;
    }
    
    /**
     * Obtém o URL da página.
     * @return String contendo o URL.
     */
    public String getUrl() { return url; }

    /**
     * Obtém o título da página.
     * @return String com o título.
     */
    public String getTitle() { return title; }

    /**
     * Obtém a citação (snippet) da página.
     * @return String com um breve excerto do texto.
     */
    public String getCitation() { return citation; }

    /**
     * Obtém o conjunto de palavras indexáveis encontradas na página.
     * @return Set de Strings com as palavras.
     */
    public Set<String> getWords() { return words; }

    /**
     * Obtém a lista de links para outras páginas encontrados neste documento.
     * @return Lista de Strings com os URLs de destino.
     */
    public List<String> getLinks() { return links; }
    
    @Override
    public String toString() {
        return "PageInfo{" +
                "url='" + url + '\'' +
                ", title='" + title + '\'' +
                ", words=" + words.size() +
                ", links=" + links.size() +
                '}';
    }
}
package pt.uc.sd.googol.gateway;

import java.io.Serializable;

/**
 * Objeto de Transferência de Dados (DTO) que representa um resultado de pesquisa.
 * <p>
 * Esta classe é utilizada para transportar a informação de uma página encontrada nos Barrels
 * para o Gateway e, posteriormente, para o Cliente (Seja ele a consola ou a interface Web).
 * Implementa {@link Serializable} para poder ser transmitida através de RMI.
 *
 * @author André Ramos 2023227306
 */
public class SearchResult implements Serializable {
    
    /** Versão de serialização para garantir compatibilidade RMI. */
    private static final long serialVersionUID = 1L;
    
    private final String url;
    private final String title;
    private final String snippet;
    private final int relevance;
    
    /**
     * Constrói um novo resultado de pesquisa.
     *
     * @param url O endereço Web da página encontrada.
     * @param title O título da página.
     * @param snippet Uma breve citação ou excerto do texto da página.
     * @param relevance A pontuação de relevância (geralmente baseada no número de backlinks).
     */
    public SearchResult(String url, String title, String snippet, int relevance) {
        this.url = url;
        this.title = title;
        this.snippet = snippet;
        this.relevance = relevance;
    }
    
    /**
     * Obtém o URL do resultado.
     * @return String com o endereço Web.
     */
    public String getUrl() { return url; }

    /**
     * Obtém o título da página.
     * @return String com o título.
     */
    public String getTitle() { return title; }

    /**
     * Obtém o excerto de texto (citação) da página.
     * @return String com o snippet.
     */
    public String getSnippet() { return snippet; }

    /**
     * Obtém o valor de relevância do resultado.
     * @return Inteiro representando a importância da página (ex: contagem de backlinks).
     */
    public int getRelevance() { return relevance; }
    
    /**
     * Retorna uma representação formatada do resultado para exibição na consola.
     * Formato: Título, URL, Relevância e Snippet.
     *
     * @return String formatada.
     */
    @Override
    public String toString() {
        return String.format(" %s\n   %s\n   Relevância: %d\n   %s\n",
            title, url, relevance, snippet);
    }
}
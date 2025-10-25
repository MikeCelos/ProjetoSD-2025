package pt.uc.sd.googol.common;

import java.io.Serializable;

/**
 * Objeto de transferência de resultados de pesquisa.
 * Enviado do Barrel → Gateway → Cliente via RMI.
 */
public class SearchResult implements Serializable {
    private final String url;
    private final String title;
    private final String snippet;
    private final int rank;  // nº de backlinks (in-degree)

    public SearchResult(String url, String title, String snippet, int rank) {
        this.url = url;
        this.title = title;
        this.snippet = snippet;
        this.rank = rank;
    }

    // Getters
    public String getUrl() { return url; }
    public String getTitle() { return title; }
    public String getSnippet() { return snippet; }
    public int getRank() { return rank; }

    @Override
    public String toString() {
        return String.format("[%d] %s (%s)\n   %s",
                rank, title, url, snippet);
    }
}

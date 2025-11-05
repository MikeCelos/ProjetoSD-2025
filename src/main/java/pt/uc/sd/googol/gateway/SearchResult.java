package pt.uc.sd.googol.gateway;

import java.io.Serializable;

public class SearchResult implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String url;
    private final String title;
    private final String snippet;
    private final int relevance;
    
    public SearchResult(String url, String title, String snippet, int relevance) {
        this.url = url;
        this.title = title;
        this.snippet = snippet;
        this.relevance = relevance;
    }
    
    public String getUrl() { return url; }
    public String getTitle() { return title; }
    public String getSnippet() { return snippet; }
    public int getRelevance() { return relevance; }
    
    @Override
    public String toString() {
        return String.format(" %s\n   %s\n   Relevância: %d\n   %s\n",
            title, url, relevance, snippet);
    }
}
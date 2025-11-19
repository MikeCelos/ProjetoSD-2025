package pt.uc.sd.googol.common;

import java.io.Serializable;
import java.util.*;

public class PageInfo implements Serializable {
    private static final long serialVersionUID = 1L; // ← ADICIONAR
    
    private final String url;
    private final String title;
    private final String citation;
    private final Set<String> words;
    private final List<String> links;
        
    public PageInfo(String url, String title, String citation, 
                    Set<String> words, List<String> links) {
        this.url = url;
        this.title = title;
        this.citation = citation;
        this.words = words;
        this.links = links;
    }
    
    // Getters
    public String getUrl() { return url; }
    public String getTitle() { return title; }
    public String getCitation() { return citation; }
    public Set<String> getWords() { return words; }
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
package pt.uc.sd.googol.gateway; //

import java.io.Serializable;

public class Search implements Serializable, Comparable<Search> {
    
    private String term;
    private int count;

    public Search() {}

    public Search(String term, int count) {
        this.term = term;
        this.count = count;
    }

    // Getters usados pelo Gateway
    public String getSearch() { return term; }
    public int getAccesses() { return count; }

    // Setters
    public void setSearch(String term) { this.term = term; }
    public void setAccesses(int count) { this.count = count; }

    // Para ordenar o Top 10
    @Override
    public int compareTo(Search o) {
        // Ordem decrescente (maior count primeiro)
        return Integer.compare(o.count, this.count);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Search search = (Search) o;
        return count == search.count && term.equals(search.term);
    }
}
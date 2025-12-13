/**
 * ===============================================================
 *  Projeto GOOGOL — Meta 1
 *  Componente: Gateway
 *  Classe: Search
 * ===============================================================
 *
 * <p>
 * A classe {@code Search} representa um termo de pesquisa e o respetivo
 * número de acessos/pesquisas realizadas pelos utilizadores.
 * </p>
 *
 * <p>
 * É utilizada pelo {@code Gateway} para manter estatísticas de pesquisa,
 * nomeadamente para calcular e apresentar o <b>Top 10 termos mais
 * pesquisados</b>.
 * </p>
 *
 * <p>
 * Esta classe:
 * <ul>
 *   <li>Implementa {@link Serializable} para poder ser transmitida
 *       entre componentes distribuídos via RMI;</li>
 *   <li>Implementa {@link Comparable} para permitir ordenação direta
 *       por número de acessos (ordem decrescente);</li>
 *   <li>É uma estrutura simples de dados (DTO) sem lógica de negócio.</li>
 * </ul>
 * </p>
 *
 * @author
 * Elemento 1: André Ramos
 * Elemento 2: Francisco Vasconcelos e Sá Pires da Silva
 */

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
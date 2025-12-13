/**
 * ===============================================================
 *  Projeto GOOGOL — Meta 1 / Meta 2
 *  Ficheiro: SearchResult.java
 * ===============================================================
 *
 *  @Resumo:
 *  Objeto de Transferência de Dados (DTO) que representa um resultado
 *  individual de uma pesquisa efetuada no motor de busca distribuído GOOGOL.
 *
 *  Cada instância desta classe corresponde a uma página indexada que
 *  satisfaz uma determinada query, contendo informação suficiente para
 *  apresentação ao utilizador final.
 *
 *  @Papel na arquitetura:
 *  - Criado pelos Barrels após a execução de uma pesquisa.
 *  - Transmitido ao Gateway através de RMI.
 *  - Encaminhado pelo Gateway para os clientes (CLI ou Web).
 *
 *  @Características:
 *  - Imutável (todos os campos são {@code final}).
 *  - Implementa {@link Serializable}, permitindo transporte via RMI.
 *  - Contém apenas dados (sem lógica de negócio).
 *
 *  @Campos representados:
 *  - URL da página encontrada.
 *  - Título da página.
 *  - Excerto textual (snippet) relevante para a query.
 *  - Pontuação de relevância (ex.: baseada em backlinks).
 *
 *  @Notas de desenho:
 *  - Esta classe segue o padrão DTO (Data Transfer Object).
 *  - Não contém setters para garantir integridade dos dados.
 *  - A relevância é calculada nos Barrels e não no Gateway.
 *
 *  @Autor:
 *   André Ramos — 2023227306
 *   Francisco Vasconcelos e Sá Pires da Silva — 2023220012
 */

package pt.uc.sd.googol.gateway;

import java.io.Serializable;

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
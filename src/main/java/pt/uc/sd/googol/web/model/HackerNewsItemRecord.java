/**
 * ===============================================================
 *  Projeto GOOGOL — Meta 2
 *  Ficheiro: HackerNewsItemRecord.java
 * ===============================================================
 *
 *  @Resumo:
 *  Modelo de dados (DTO) que representa um item devolvido pela
 *  API pública do Hacker News (Y Combinator).
 *
 *  Esta classe é utilizada para mapear automaticamente respostas
 *  JSON obtidas a partir do endpoint:
 *      https://hacker-news.firebaseio.com/v0/item/{id}.json
 *  para objetos Java, recorrendo à biblioteca Jackson.
 *
 *  @Papel no projeto:
 *  - Utilizada pelo módulo Web / Gateway para consumir dados externos.
 *  - Permite integrar fontes externas de conteúdo (Hacker News)
 *    no motor de pesquisa GOOGOL.
 *  - Os URLs extraídos desta classe são enviados para indexação
 *    através do Gateway e Downloader.
 *
 *  @Integração com Jackson:
 *  - A anotação {@link JsonIgnoreProperties} com {@code ignoreUnknown = true}
 *    garante compatibilidade futura com alterações na API do Hacker News.
 *  - Campos adicionais introduzidos pela API são ignorados automaticamente.
 *
 *  @Campos relevantes para o GOOGOL:
 *  - {@code url}: endereço externo da notícia (campo crítico para indexação).
 *  - {@code title}: título da história, usado para filtragem e apresentação.
 *  - {@code score}: pontuação da notícia no Hacker News.
 *
 *  @Notas de desenho:
 *  - Esta classe segue o padrão DTO (Data Transfer Object).
 *  - Não contém lógica de negócio.
 *  - Inclui construtor vazio obrigatório para deserialização JSON.
 *
 *  @Autor:
 *   Elemento 1: André Ramos — 2023227306
 *   Elemento 2: Francisco Vasconcelos e Sá Pires da Silva (2023220012)
 */

package pt.uc.sd.googol.web.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Modelo de dados (DTO) que representa um item (notícia, comentário, etc.) da API do Hacker News.
 * <p>
 * Esta classe é utilizada para mapear automaticamente as respostas JSON provenientes da API externa
 * (https://hacker-news.firebaseio.com/v0/item/{id}.json) para objetos Java.
 * <p>
 * A anotação {@link JsonIgnoreProperties} garante que, se a API do Hacker News adicionar
 * novos campos no futuro, a aplicação não falha ao tentar processá-los.
 *
 * @author André Ramos 2023227306
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HackerNewsItemRecord {
    
    /** O identificador único do item no Hacker News. */
    private Integer id;
    
    /** Indica se o item foi apagado. */
    private Boolean deleted;
    
    /** O tipo de item (ex: "story", "comment", "job", "poll"). */
    private String type;
    
    /** O nome de utilizador do autor. */
    private String by;
    
    /** Data de criação em formato Unix Time. */
    private Long time;
    
    /** O texto do comentário ou história (pode conter HTML). */
    private String text;
    
    /** Indica se o item está "morto" (dead). */
    private Boolean dead;
    
    /** O ID do item pai (para comentários). */
    private String parent;
    
    /** O ID da sondagem associada. */
    private Integer poll;
    
    /** Lista de IDs dos comentários filhos. */
    private List<Integer> kids;
    
    /** O URL da história. Campo CRÍTICO para o projeto Googol (é este que indexamos). */
    private String url;        
    
    /** A pontuação (karma) da história. */
    private Integer score;
    
    /** O título da história. Usado para filtrar pesquisas. */
    private String title;
    
    /** Lista de partes relacionadas (para sondagens). */
    private List<Integer> parts;
    
    /** O número total de comentários descendentes. */
    private Integer descendants;

    /**
     * Construtor vazio necessário para a deserialização do Jackson (JSON -> Objeto).
     */
    public HackerNewsItemRecord() {
    }

    /**
     * Construtor completo para instanciar objetos manualmente, se necessário.
     *
     * @param id O ID do item.
     * @param deleted Se foi apagado.
     * @param type O tipo de item.
     * @param by O autor.
     * @param time O timestamp.
     * @param text O texto.
     * @param dead Se está morto.
     * @param parent O pai.
     * @param poll A sondagem.
     * @param kids Os filhos.
     * @param url O URL externo.
     * @param score A pontuação.
     * @param title O título.
     * @param parts As partes.
     * @param descendants O número de descendentes.
     */
    public HackerNewsItemRecord(Integer id, Boolean deleted, String type, String by, Long time, String text,
                                Boolean dead, String parent, Integer poll, List<Integer> kids, String url,
                                Integer score, String title, List<Integer> parts, Integer descendants) {
        this.id = id;
        this.deleted = deleted;
        this.type = type;
        this.by = by;
        this.time = time;
        this.text = text;
        this.dead = dead;
        this.parent = parent;
        this.poll = poll;
        this.kids = kids;
        this.url = url;
        this.score = score;
        this.title = title;
        this.parts = parts;
        this.descendants = descendants;
    }

    public Integer getId() {
        return id;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public String getType() {
        return type;
    }

    public String getBy() {
        return by;
    }

    public Long getTime() {
        return time;
    }

    public String getText() {
        return text;
    }

    public Boolean getDead() {
        return dead;
    }

    public String getParent() {
        return parent;
    }

    public Integer getPoll() {
        return poll;
    }

    public List<Integer> getKids() {
        return kids;
    }

    /**
     * Obtém o URL externo associado à história.
     * @return O endereço Web para indexação.
     */
    public String getUrl() {
        return url;
    }

    public Integer getScore() {
        return score;
    }

    /**
     * Obtém o título da história.
     * @return O título para filtragem por palavras-chave.
     */
    public String getTitle() {
        return title;
    }

    public List<Integer> getParts() {
        return parts;
    }

    public Integer getDescendants() {
        return descendants;
    }
}
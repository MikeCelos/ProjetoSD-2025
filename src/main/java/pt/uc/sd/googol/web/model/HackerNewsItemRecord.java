package pt.uc.sd.googol.web.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HackerNewsItemRecord {
    private Integer id;
    private Boolean deleted;
    private String type;
    private String by;
    private Long time;
    private String text;
    private Boolean dead;
    private String parent;
    private Integer poll;
    private List<Integer> kids;
    private String url;        
    private Integer score;
    private String title;
    private List<Integer> parts;
    private Integer descendants;

    public HackerNewsItemRecord() {
    }

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

    public String getUrl() {
        return url;
    }

    public Integer getScore() {
        return score;
    }

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
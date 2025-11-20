package pt.uc.sd.googol.barrel;

import pt.uc.sd.googol.common.PageInfo;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public class SyncData implements Serializable {
    public Map<String, PageInfo> pages;
    public Map<String, Set<String>> index;
    public Map<String, Set<String>> backlinks;

    public SyncData(Map<String, PageInfo> pages, 
                   Map<String, Set<String>> index, 
                   Map<String, Set<String>> backlinks) {
        this.pages = pages;
        this.index = index;
        this.backlinks = backlinks;
    }
}

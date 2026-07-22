package com.flyemu.share.dto;

import lombok.Data;

@Data
public class ChaptersDto  {
    private Integer index;
    private Integer id;
    private String name;
    private String type;
    private String videoUrl;
    private Integer sort;
    private Integer chaptersId;
    private Annex videoAnnex;

}
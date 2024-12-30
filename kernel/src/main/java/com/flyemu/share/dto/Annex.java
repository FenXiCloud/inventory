package com.flyemu.share.dto;

import lombok.Data;

@Data
public class Annex {
    private String name; //文件原始名称
    private String size; //文件大小
    private String type; //类型
    private String url; //文件存储路径
}

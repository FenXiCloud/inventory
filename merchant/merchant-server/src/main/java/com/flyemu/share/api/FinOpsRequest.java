package com.flyemu.share.api;

import lombok.Data;

import java.io.Serializable;

@Data
public class FinOpsRequest implements Serializable {

    private String cookie;

    private String account;

    private String password;

    private String params;

    private String baseUrl;

    private String suffixUri;

    private FinOpsCallback callback;
}

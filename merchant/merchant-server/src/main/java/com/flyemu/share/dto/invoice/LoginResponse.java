package com.flyemu.share.dto.invoice;

import lombok.Data;

@Data
public class LoginResponse {
    private boolean success;
    private String token;
    private String taskId;
    private String message;
}

package com.flyemu.share.controller;

import lombok.Data;

import java.util.TreeMap;

@Data
public class JsonResult {

    private boolean success = true;

    private Integer code = 200;

    private String msg = "";

    private Object data;

    public static JsonResult instance(boolean success) {
        JsonResult result = new JsonResult();
        result.setSuccess(success);
        return result;
    }

    public static JsonResult successful() {
        return new JsonResult();
    }

    public static JsonResult successful(Object data) {
        JsonResult result = new JsonResult();
        result.setData(data);
        return result;
    }

    public static JsonResult successful(Integer code, Object data) {
        JsonResult result = new JsonResult();
        result.setCode(code);
        result.setData(data);
        return result;
    }

    public static JsonResult successful(String msg, Integer code) {
        JsonResult result = new JsonResult();
        result.setMsg(msg);
        result.setCode(code);
        return result;
    }

    public static JsonResult failure() {
        JsonResult result = new JsonResult();
        result.setSuccess(false);
        result.setCode(-1);
        return result;
    }

    public static JsonResult failure(String msg) {
        JsonResult result = new JsonResult();
        result.setSuccess(false);
        result.setMsg(msg);
        result.setCode(-1);
        return result;
    }

    public static JsonResult failure(String msg, Integer code) {
        JsonResult result = new JsonResult();
        result.setSuccess(false);
        result.setMsg(msg);
        result.setCode(code);
        return result;
    }

    public JsonResult setMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public JsonResult setData(Object data) {
        this.data = data;
        return this;
    }

    public JsonResult setCode(Integer code) {
        this.code = code;
        return this;
    }

    public JsonResult data(String key, Object value) {
        if (this.data == null) {
            this.data = new TreeMap<String, Object>();
            ((TreeMap) this.data).put(key, value);
        } else if (data instanceof TreeMap) {
            ((TreeMap) this.data).put(key, value);
        }
        return this;
    }
}

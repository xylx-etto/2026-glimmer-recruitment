package com.test;

import com.alibaba.fastjson2.JSON;

public class QueryResponse {
    private String pick_code;
    private String msg;
    public QueryResponse(String pick_code,String msg){

        this.pick_code=pick_code;
        this.msg=msg;
    }
    public QueryResponse(){

    }

    public String getMsg() {
        return msg;
    }

    public String getPick_code() {
        return pick_code;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public void setPick_code(String pick_code) {
        this.pick_code = pick_code;
    }
}

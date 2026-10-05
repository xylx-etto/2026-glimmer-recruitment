package com.test;

public class QueryRequest {
    //String json = JSON.toJSONString(obj);                  // 对象 → JSON 字符串
    //User user = JSON.parseObject(json, User.class);        // JSON 字符串 → 对象
    private String trackingNumber;
    private String phone;
    public QueryRequest(String trackingNumber,String phone){
        this.trackingNumber=trackingNumber;
        this.phone=phone;
    }
    public QueryRequest(){

    }
    public String getTrackingNumber(){
        return trackingNumber;
    }
    public String getPhone(){
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }
}

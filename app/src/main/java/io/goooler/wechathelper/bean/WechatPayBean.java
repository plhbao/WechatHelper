package io.goooler.wechathelper.bean;


import java.io.Serializable;

/**
 * 微信支付服务器回传字段
 *
 * @author Goooler
 * @version 1.0
 * @date 2019-08-21
 */
public class WechatPayBean implements Serializable {
    /**
     * 应用 ID
     */
    private String appid;
    /**
     * 商户号
     */
    private String partnerid;
    /**
     * 预支付交易会话 ID
     */
    private String prepayid;
    /**
     * 随机字符串
     */
    private String noncestr;
    /**
     * 时间戳
     */
    private String timestamp;
    /**
     * 扩展字段
     */
    private String packages;
    /**
     * 签名
     */
    private String sign;

    public String getAppid() {
        return appid;
    }

    public void setAppid(String appid) {
        this.appid = appid;
    }

    public String getPartnerid() {
        return partnerid;
    }

    public void setPartnerid(String partnerid) {
        this.partnerid = partnerid;
    }

    public String getPrepayid() {
        return prepayid;
    }

    public void setPrepayid(String prepayid) {
        this.prepayid = prepayid;
    }

    public String getNoncestr() {
        return noncestr;
    }

    public void setNoncestr(String noncestr) {
        this.noncestr = noncestr;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getPackages() {
        return packages;
    }

    public void setPackages(String packages) {
        this.packages = packages;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }
}

package io.goooler.wechathelper.bean;

/**
 * 微信登录和支付的回调状态码
 *
 * @author Goooler
 * @version 1.0
 * @date 2019-08-21
 */
public interface ErrCode {
    /**
     * （登录、支付）成功
     */
    int SUCCESS = 0;
    /**
     * （登录、支付）通用错误
     */
    int COMMON = -1;
    /**
     * （登录、支付）用户取消
     */
    int CANCEL = -2;
    /**
     * （登录）发送失败
     */
    int SENT_FAILED = -3;
    /**
     * （登录）认证被否决
     */
    int AUTH_DENIED = -4;
    /**
     * （登录）不支持错误
     */
    int UNSUPPORTED = -5;
    /**
     * （登录）被禁止
     */
    int BAN = -6;
}
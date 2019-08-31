package io.goooler.wechathelper.callback;

/**
 * 微信登录回调
 *
 * @author Goooler
 * @version 1.0
 * @date 2019-08-21
 */
public interface WxLoginListener {

    /**
     * 成功回调
     *
     * @param code 授权码（登录）
     */
    void onSucceed(String code);

    /**
     * 失败回调
     *
     * @param errorCode 错误码，错误类型有多种，详见 ErrCode
     */
    void onFailed(int errorCode);
}
package io.goooler.wechathelper.callback;

/**
 * 微信支付回调
 *
 * @author Goooler
 * @version 1.0
 * @date 2019-08-21
 */
public interface WxPayListener {
    /**
     * 成功回调
     *
     * @param extData 订单信息（支付）
     */
    void onSucceed(String extData);

    /**
     * 支付失败回调
     *
     * @param errorCode 错误码，错误类型有多种，详见 ErrCode
     * @param extData   返回的订单信息等
     */
    void onFailed(int errorCode, String extData);
}

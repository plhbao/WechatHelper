package io.goooler.wechathelper.wxapi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelpay.PayResp;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;

import java.lang.ref.WeakReference;

import io.goooler.wechathelper.WxHelper;
import io.goooler.wechathelper.bean.ErrCode;
import io.goooler.wechathelper.callback.WxPayListener;

/**
 * 接收微信支付的回调（微信调用）
 *
 * @author Goooler
 * @version 1.0
 * @date 2019-08-20
 */
public class WXPayEntryActivity extends Activity implements IWXAPIEventHandler {

    /**
     * 监听使用弱引用防止微信不回调时无法销毁造成内存泄露
     */
    private static WeakReference<WxPayListener> wxPayListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WxHelper.getInstance().handleResponse(getIntent(), this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        WxHelper.getInstance().handleResponse(getIntent(), this);
    }

    /**
     * 第三方应用发送到微信的请求处理后的响应结果，会回调到该方法
     */
    @Override
    public void onResp(BaseResp baseResp) {
        boolean hasWxLoginListener = wxPayListener != null && wxPayListener.get() != null;

        if (baseResp != null) {
            PayResp payResp = (PayResp) baseResp;
            switch (payResp.errCode) {
                case PayResp.ErrCode.ERR_OK:
                    if (hasWxLoginListener) {
                        wxPayListener.get().onSucceed(payResp.extData);
                    }
                    break;
                case PayResp.ErrCode.ERR_COMM:
                    if (hasWxLoginListener) {
                        wxPayListener.get().onFailed(ErrCode.COMMON, payResp.extData);
                    }
                    break;
                case PayResp.ErrCode.ERR_USER_CANCEL:
                    if (hasWxLoginListener) {
                        wxPayListener.get().onFailed(ErrCode.CANCEL, payResp.extData);
                    }
                default:
                    break;
            }
        }
        finish();
    }

    /**
     * 微信发送请求到第三方应用时，会回调到该方法
     */
    @Override
    public void onReq(BaseReq baseReq) {
    }

    public static void setWxPayListener(@Nullable WxPayListener wxPayListener) {
        WXPayEntryActivity.wxPayListener = new WeakReference<>(wxPayListener);
    }

    @Override
    protected void onDestroy() {
        wxPayListener = null;
        super.onDestroy();
    }
}

package io.goooler.wechathelper.wxapi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;

import java.lang.ref.WeakReference;

import io.goooler.wechathelper.WxHelper;
import io.goooler.wechathelper.bean.ErrCode;
import io.goooler.wechathelper.callback.WxLoginListener;
import io.goooler.wechathelper.callback.WxShareListener;

/**
 * 接收微信登录的回调（微信调用）
 *
 * @author Goooler
 * @version 1.0
 * @date 2019-08-20
 */
public class WXEntryActivity extends Activity implements IWXAPIEventHandler {

    /**
     * 监听使用弱引用防止微信不回调时无法销毁造成内存泄露
     */
    private static WeakReference<WxLoginListener> wxLoginListener;
    private static WeakReference<WxShareListener> wxShareListener;

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
     * 分享到朋友圈的结果无法回调，要注意
     */
    @Override
    public void onResp(BaseResp resp) {
        switch (resp.errCode) {
            case BaseResp.ErrCode.ERR_OK:
                if (resp instanceof SendAuth.Resp) {
                    String code = ((SendAuth.Resp) resp).code;
                    if (wxLoginListener != null) {
                        wxLoginListener.get().onSucceed(code);
                    }
                    if (wxShareListener != null) {
                        wxShareListener.get().onSucceed();
                    }
                }
                break;
            case BaseResp.ErrCode.ERR_USER_CANCEL:
                if (wxLoginListener != null) {
                    wxLoginListener.get().onFailed(ErrCode.CANCEL);
                }
                if (wxShareListener != null) {
                    wxShareListener.get().onFailed(ErrCode.CANCEL);
                }
                break;
            case BaseResp.ErrCode.ERR_AUTH_DENIED:
                if (wxLoginListener != null) {
                    wxLoginListener.get().onFailed(ErrCode.AUTH_DENIED);
                }
                if (wxShareListener != null) {
                    wxShareListener.get().onFailed(ErrCode.AUTH_DENIED);
                }
                break;
            case BaseResp.ErrCode.ERR_SENT_FAILED:
                if (wxLoginListener != null) {
                    wxLoginListener.get().onFailed(ErrCode.SENT_FAILED);
                }
                break;
            case BaseResp.ErrCode.ERR_UNSUPPORT:
                if (wxLoginListener != null) {
                    wxLoginListener.get().onFailed(ErrCode.UNSUPPORTED);
                }
                break;
            case BaseResp.ErrCode.ERR_COMM:
                if (wxLoginListener != null) {
                    wxLoginListener.get().onFailed(ErrCode.COMMON);
                }
                break;
            case BaseResp.ErrCode.ERR_BAN:
                if (wxLoginListener != null) {
                    wxLoginListener.get().onFailed(ErrCode.BAN);
                }
            default:
                break;
        }
        finish();
    }

    /**
     * 微信发送请求到第三方应用时，会回调到该方法
     */
    @Override
    public void onReq(BaseReq req) {
    }

    public static void setWxLoginListener(WxLoginListener wxLoginListener) {
        WXEntryActivity.wxLoginListener = new WeakReference<>(wxLoginListener);
    }

    public static void setWxShareListener(WxShareListener wxShareListener) {
        WXEntryActivity.wxShareListener = new WeakReference<>(wxShareListener);
    }

    @Override
    protected void onDestroy() {
        wxLoginListener = null;
        super.onDestroy();
    }
}

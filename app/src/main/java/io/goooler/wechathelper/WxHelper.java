package io.goooler.wechathelper;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.tencent.mm.opensdk.modelbiz.WXLaunchMiniProgram;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXImageObject;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXMiniProgramObject;
import com.tencent.mm.opensdk.modelmsg.WXTextObject;
import com.tencent.mm.opensdk.modelmsg.WXVideoObject;
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject;
import com.tencent.mm.opensdk.modelpay.PayReq;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import io.goooler.wechathelper.bean.WechatPayBean;
import io.goooler.wechathelper.callback.WxLoginListener;
import io.goooler.wechathelper.callback.WxPayListener;
import io.goooler.wechathelper.callback.WxShareListener;
import io.goooler.wechathelper.wxapi.WXEntryActivity;
import io.goooler.wechathelper.wxapi.WXPayEntryActivity;

/**
 * @author Goooler
 * @version 1.0
 * @date 2019-08-20
 */
public class WxHelper {

    private static final int TIMELINE_SUPPORTED_VERSION = 0x21020001;
    private static final int THUMB_SIZE = 150;
    // 缩略图最大32KB
    private static final int MAX_BYTES = 32 * 1024;
    // 图片的二进制数据:内容大小不超过 10MB
    private static final int MAX_IMAGE_DATA_SIZE = 10 * 1024 * 1024;
    // 小程序消息封面图片，小于128k
    private static final int MAX_MINI_PROGRAM_THUMB_SIZE = 128 * 1024;
    private static final String WECHAT_PACKAGE_NAME = "com.tencent.mm";
    private static final String WECHAT_UI_NAME = "com.tencent.mm.ui.LauncherUI";
    /**
     * 微信 6.7.3 版本号
     */
    private static final int WECHAT_673 = 1360;

    private static IWXAPI mWXApi;
    private Context appContext;
    private volatile static WxHelper instance;

    private WxHelper() {
    }

    public static WxHelper getInstance() {
        if (instance == null) {
            synchronized (WxHelper.class) {
                if (instance == null) {
                    instance = new WxHelper();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化，一般在 application 里进行
     *
     * @param appContext 上下文，一般为 app
     * @param appId      微信 appId
     */
    @MainThread
    public void initData(Context appContext, String appId) {
        this.appContext = appContext;
        if (mWXApi == null) {
            mWXApi = WXAPIFactory.createWXAPI(appContext, appId, true);
        }
        if (!mWXApi.isWXAppInstalled()) {
            showToast(R.string.not_installed);
            return;
        }
        if (!mWXApi.registerApp(appId)) {
            showToast(R.string.register_failed);
        }
    }

    /**
     * 微信登录
     *
     * @param listener 登录结果的监听
     * @return true/false
     */
    @MainThread
    public boolean login(@NonNull WxLoginListener listener) {
        if (!mWXApi.isWXAppInstalled()) {
            showToast(R.string.please_install_to_auth);
            return false;
        }

        SendAuth.Req req = new SendAuth.Req();
        req.scope = "snsapi_userinfo";
        req.state = String.valueOf(System.currentTimeMillis());
        WXEntryActivity.setWxLoginListener(listener);

        return mWXApi.sendReq(req);
    }

    /**
     * 微信支付
     *
     * @param wechatPayBean 提供微信支付的一些信息
     * @param extData       额外的一些信息，可通过微信原样返回
     * @param listener      支付结果的监听，再某些场景下无法得到回调，支付结果的判断依赖此回调不可靠，
     *                      建议在发起支付的页面的 onResume 做处理
     * @return true/false
     */
    public boolean pay(@NonNull WechatPayBean wechatPayBean,
                       @Nullable String extData,
                       @Nullable WxPayListener listener) {
        boolean ret;
        PayReq req = new PayReq();
        req.appId = wechatPayBean.getAppid();
        req.partnerId = wechatPayBean.getPartnerid();
        req.prepayId = wechatPayBean.getPrepayid();
        req.nonceStr = wechatPayBean.getNoncestr();
        req.timeStamp = wechatPayBean.getTimestamp();
        req.packageValue = "Sign=WXPay";
        req.sign = wechatPayBean.getSign();
        req.extData = extData;
        ret = mWXApi.sendReq(req);
        WXPayEntryActivity.setWxPayListener(listener);
        return ret;
    }

    /**
     * 通过系统分享文本
     */
    public void shareTextBySystem(Context context,
                                  String title,
                                  String content) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        if (title != null) {
            intent.putExtra(Intent.EXTRA_SUBJECT, title);
        }
        intent.putExtra(Intent.EXTRA_TEXT, content);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_to)));
    }

    /**
     * 通过系统分享单图
     */
    public void shareSinglePictureBySystem(Context context,
                                           String title,
                                           String content,
                                           String imagePath) {
        Uri imageUri;
        File file = new File(imagePath);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            imageUri = Uri.fromFile(file);
        } else {
            imageUri = getImageContentUri(context, file);
        }
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("image/*");
        if (title != null) {
            intent.putExtra(Intent.EXTRA_SUBJECT, title);
        }
        if (content != null) {
            intent.putExtra(Intent.EXTRA_TEXT, content);
        }
        intent.putExtra(Intent.EXTRA_STREAM, imageUri);
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_to)));
    }

    /**
     * 通过系统分享视频
     */
    public void shareVideoBySystem(Context context,
                                   File videoFile) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        Uri videoUri;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            videoUri = Uri.fromFile(videoFile);
        } else {
            videoUri = getVideoContentUri(context, videoFile);
        }
        intent.putExtra(Intent.EXTRA_STREAM, videoUri);
        intent.setType("video/*");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_to)));
    }

    /**
     * 通过微信分享，发送文本给好友
     *
     * @param text 文本
     * @return 成功与否
     */
    public boolean shareTextToFriends(String text,
                                      WxShareListener listener,
                                      String appId) {
        return shareTextByWechat(text, SendMessageToWX.Req.WXSceneSession, listener, appId);
    }

    /**
     * 通过微信分享，发送文本到朋友圈
     *
     * @param text 文本
     * @return 成功与否
     */
    public boolean shareTextToTimeline(String text,
                                       WxShareListener listener,
                                       String appId) {
        return shareTextByWechat(text, SendMessageToWX.Req.WXSceneTimeline, listener, appId);
    }

    /**
     * 通过微信分享，发送图片给好友
     *
     * @param path 图片路径
     * @return 成功与否
     */
    public boolean sharePictureToFriends(String path,
                                         WxShareListener listener,
                                         String appId) {
        return sharePictureByWechat(path, SendMessageToWX.Req.WXSceneSession, listener, appId);
    }

    /**
     * 通过微信分享，发送图片到朋友圈
     *
     * @param path 图片路径
     * @return 成功与否
     */
    public boolean sharePictureToTimeline(String path,
                                          WxShareListener listener,
                                          String appId) {
        return sharePictureByWechat(path, SendMessageToWX.Req.WXSceneTimeline, listener, appId);
    }

    /**
     * 通过微信分享，发送 url 给好友
     *
     * @param url         链接
     * @param title       分享标题
     * @param thumbOrigin 缩略图
     * @param description 描述文字
     * @return 成功与否
     */
    public boolean shareUrlToFriends(String url,
                                     String title,
                                     Bitmap thumbOrigin,
                                     String description,
                                     WxShareListener listener,
                                     String appId) {
        return shareUrlByWechat(url, title, thumbOrigin, description,
                SendMessageToWX.Req.WXSceneSession, listener, appId);
    }

    /**
     * 通过微信分享，发送url到朋友圈
     *
     * @param url         链接
     * @param title       分享标题
     * @param thumbOrigin 缩略图
     * @param description 描述文字
     * @return 成功与否
     */
    public boolean shareUrlToTimeline(String url,
                                      String title,
                                      Bitmap thumbOrigin,
                                      String description,
                                      WxShareListener listener,
                                      String appId) {
        return shareUrlByWechat(url, title, thumbOrigin, description,
                SendMessageToWX.Req.WXSceneTimeline, listener, appId);
    }

    /**
     * 通过微信分享视频给朋友
     */
    public boolean shareVideoToFriends(String url,
                                       String title,
                                       Bitmap thumbOrigin,
                                       String description,
                                       WxShareListener listener,
                                       String appId) {
        return shareVideoByWechat(url, title, thumbOrigin, description,
                SendMessageToWX.Req.WXSceneSession, listener, appId);
    }

    /**
     * 通过微信分享视频到朋友圈
     */
    public boolean shareVideoToTimeline(String url,
                                        String title,
                                        Bitmap thumbOrigin,
                                        String description,
                                        WxShareListener listener,
                                        String appId) {
        return shareVideoByWechat(url, title, thumbOrigin, description,
                SendMessageToWX.Req.WXSceneTimeline, listener, appId);
    }

    /**
     * 通过系统分享多张图片给微信好友
     *
     * @param context context
     * @param content content
     * @param imgList 图片文件列表
     */
    public void shareImagesToSession(Context context, String content, List<File> imgList) {
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_TEXT, content);
        intent.setComponent(new ComponentName("com.tencent.mm", "com.tencent.mm.ui.tools.ShareImgUI"));
        intent.setAction(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("image/*");
        ArrayList<Uri> imgUris = new ArrayList<>();
        for (File img : imgList) {
            Uri imageUri;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                imageUri = Uri.fromFile(img);
            } else {
                imageUri = getImageContentUri(context, img);
            }
            imgUris.add(imageUri);
        }
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imgUris);
        context.startActivity(intent);
    }

    /**
     * 真正实现分享文本的方法
     */
    private boolean shareTextByWechat(String text,
                                      int scene,
                                      WxShareListener listener,
                                      String appId) {
        WXTextObject textObj = new WXTextObject();
        textObj.text = text;
        return shareByWechat(textObj, null, null, text, scene, listener, appId);
    }

    /**
     * 真正实现分享图片的方法
     */
    private boolean sharePictureByWechat(String path,
                                         int scene,
                                         WxShareListener listener,
                                         String appId) {
        WXImageObject imageObject;
        Bitmap thumbBmp;
        Bitmap bmp = BitmapFactory.decodeFile(path);
        Bitmap sendBitmap;
        if (bmp.getByteCount() > MAX_IMAGE_DATA_SIZE) {
            byte[] compressedBytes = bitmap2Bytes(bmp, MAX_IMAGE_DATA_SIZE);
            sendBitmap = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.length);
        } else {
            sendBitmap = bmp;
        }
        imageObject = new WXImageObject(sendBitmap);
        thumbBmp = Bitmap.createScaledBitmap(bmp, THUMB_SIZE, THUMB_SIZE, true);
        return shareByWechat(imageObject, null, thumbBmp, null, scene, listener, appId);
    }

    /**
     * 真正实现分享链接的方法
     */
    private boolean shareUrlByWechat(String url,
                                     String title,
                                     Bitmap bmp,
                                     String description,
                                     int scene,
                                     WxShareListener listener,
                                     String appId) {
        WXWebpageObject webPage = new WXWebpageObject();
        webPage.webpageUrl = url;
        Bitmap thumbBmp;
        thumbBmp = Bitmap.createScaledBitmap(bmp, THUMB_SIZE, THUMB_SIZE, true);
        return shareByWechat(webPage, title, thumbBmp, description, scene, listener, appId);
    }

    /**
     * 真正实现分享视频的方法
     */
    private boolean shareVideoByWechat(String url,
                                       String title,
                                       Bitmap bmp,
                                       String description,
                                       int scene,
                                       WxShareListener listener,
                                       String appId) {
        WXVideoObject videoObj = new WXVideoObject();
        videoObj.videoUrl = url;

        Bitmap thumbBmp;
        thumbBmp = Bitmap.createScaledBitmap(bmp, THUMB_SIZE, THUMB_SIZE, true);
        return shareByWechat(videoObj, title, thumbBmp, description, scene, listener, appId);
    }

    /**
     * 所有分享的重载的私有方法，可区分分享的不同类型
     *
     * @param mediaObject 要分享的媒体对象
     * @param title       分享标题
     * @param thumb       缩略图
     * @param description 分享的描述文字
     * @param scene       分享到的场景，会话列表、朋友圈等
     * @param listener    分享结果的监听
     * @param appId       部分场景下这里需要的 appId 和登录、支付的不同，需要重载，为空代表使用默认的
     * @return true/false
     */
    private boolean shareByWechat(@NonNull WXMediaMessage.IMediaObject mediaObject,
                                  @Nullable String title,
                                  @Nullable Bitmap thumb,
                                  @Nullable String description,
                                  int scene,
                                  @Nullable WxShareListener listener,
                                  @Nullable String appId) {
        WXMediaMessage msg = new WXMediaMessage(mediaObject);
        if (title != null) {
            msg.title = title;
        }

        if (description != null) {
            msg.description = description;
        }

        if (thumb != null) {
            msg.thumbData = bitmap2Bytes(thumb, MAX_BYTES);
        }

        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = String.valueOf(System.currentTimeMillis());
        req.message = msg;
        req.scene = scene;
        WXEntryActivity.setWxShareListener(listener);
        if (!TextUtils.isEmpty(appId)) {
            return WXAPIFactory.createWXAPI(appContext, appId, true).sendReq(req);
        }
        return mWXApi.sendReq(req);
    }

    /**
     * 小程序分享
     *
     * @param webPageUrl  兼容低版本的网页链接
     * @param userName    小程序的原始id
     * @param path        小程序页面路径
     * @param title       小程序消息title
     * @param description 小程序消息desc
     * @param authority   fileProvider 的 authority
     * @param imageFile   小程序消息封面图片，小于128k
     * @param appId       部分场景下这里需要的 appId 和登录、支付的不同，需要重载，为空代表使用默认的
     * @param type        小程序的类型，默认正式版
     * @param listener    监听
     */
    public void miniAppShare(String webPageUrl,
                             String userName,
                             String path,
                             String title,
                             String description,
                             final File imageFile,
                             @NonNull String authority,
                             @Nullable Integer type,
                             @Nullable String appId,
                             @Nullable WxShareListener listener) {
        WXMiniProgramObject miniProgramObj = new WXMiniProgramObject();
        webPageUrl = TextUtils.isEmpty(webPageUrl) ? " " : webPageUrl;
        // 兼容低版本的网页链接
        miniProgramObj.webpageUrl = webPageUrl;
        // 小程序原始id
        miniProgramObj.userName = userName;
        //小程序页面路径
        miniProgramObj.path = path;
        miniProgramObj.withShareTicket = true;
        if (type == null) {
            miniProgramObj.miniprogramType = WXMiniProgramObject.MINIPTOGRAM_TYPE_RELEASE;
        } else {
            switch (type) {
                //正式版
                case 0:
                    miniProgramObj.miniprogramType = WXMiniProgramObject.MINIPTOGRAM_TYPE_RELEASE;
                    break;
                //开发版
                case 1:
                    miniProgramObj.miniprogramType = WXMiniProgramObject.MINIPROGRAM_TYPE_TEST;
                    break;
                //体验版
                case 2:
                    miniProgramObj.miniprogramType = WXMiniProgramObject.MINIPROGRAM_TYPE_PREVIEW;
                    break;
                default:
                    break;
            }
        }
        WXMediaMessage msg = new WXMediaMessage(miniProgramObj);
        // 小程序消息title
        msg.title = title;
        // 小程序消息desc
        msg.description = description;
        if (imageFile != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            msg.thumbData = getThumb(bitmap);
        }
        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = String.valueOf(System.currentTimeMillis());
        req.message = msg;
        req.scene = SendMessageToWX.Req.WXSceneSession;
        WXEntryActivity.setWxShareListener(listener);
        if (!TextUtils.isEmpty(appId)) {
            WXAPIFactory.createWXAPI(appContext, appId, true).sendReq(req);
        } else {
            mWXApi.sendReq(req);
        }
    }

    /**
     * 处理微信登录、支付、分享等回调结果
     *
     * @param intent 微信传递过来的 intent
     * @param h      微信定义的接收回调结果的接口
     */
    public void handleResponse(Intent intent, IWXAPIEventHandler h) {
        mWXApi.handleIntent(intent, h);
    }

    /**
     * 调起微信
     *
     * @param context 上下文
     */
    public void openWechat(Context context) {
        Intent intent = new Intent();
        ComponentName cmp = new ComponentName(WECHAT_PACKAGE_NAME, WECHAT_UI_NAME);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setComponent(cmp);
        context.startActivity(intent);
    }

    /**
     * 跳转到小程序
     *
     * @param miniAppId       小程序原始id
     * @param path            拉起小程序页面的可带参路径，不填默认拉起小程序首页
     * @param appId           部分场景下这里需要的 appId 和登录、支付的不同，需要重载，
     * @param miniprogramType 小程序版本类型 发布（0）| 测试（1） | 体验（2）
     */
    public void gotoMiniApp(String miniAppId, String path, @Nullable String appId, int miniprogramType) {
        WXLaunchMiniProgram.Req req = new WXLaunchMiniProgram.Req();
        req.userName = miniAppId;
        req.path = path;
        // 可选打开 开发版，体验版和正式版
        req.miniprogramType = miniprogramType;
        if (!TextUtils.isEmpty(appId)) {
            WXAPIFactory.createWXAPI(appContext, appId, true).sendReq(req);
        } else {
            mWXApi.sendReq(req);
        }
    }

    /**
     * 检测手机上是否已经安装微信
     *
     * @return 已经安装返回true, 否则返回false
     */
    public boolean isWechatInstalled(String appId) {
        return mWXApi.isWXAppInstalled();
    }

    /**
     * 判断是否支持转发到朋友圈
     * 微信4.2以上支持，如果需要检查微信版本支持API的情况，可调用 IWXAPI 的 getWXAppSupportAPI 方法，
     * 0x21020001 及以上支持发送朋友圈
     *
     * @return boolean
     */
    public boolean isSupportWechat() {
        return mWXApi.getWXAppSupportAPI() >= TIMELINE_SUPPORTED_VERSION;
    }

    /**
     * 微信 6.7.3 版本的判断，1360 是微信 6.7.3 的版本号
     * 6.7.3 开始不再支持多图分享，但允许手动添加
     *
     * @return 是否
     */
    @SuppressLint("NewApi")
    public boolean isWechat673Version() {
        boolean ret = false;
        try {
            PackageInfo packageInfo = appContext.getPackageManager().getPackageInfo(WECHAT_PACKAGE_NAME, 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    && packageInfo != null
                    && packageInfo.getLongVersionCode() >= WECHAT_673) {
                ret = true;
            } else if (packageInfo != null && packageInfo.getLongVersionCode() >= WECHAT_673) {
                ret = true;
            }
        } catch (Exception e) {
            // do nothing
        }
        return ret;
    }

    /**
     * @param type text/image/webpage/music/video
     * @return 时间戳
     */
    private String buildTransaction(String type) {
        return TextUtils.isEmpty(type) ? String.valueOf(System.currentTimeMillis()) : (type + System.currentTimeMillis());
    }

    /**
     * Bitmap 转换成 byte[] 并且进行压缩, 压缩到不大于给定上限
     */
    private byte[] bitmap2Bytes(Bitmap bitmap, int maxBytes) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
        int options = 100;
        while (output.toByteArray().length > maxBytes && options != 10) {
            output.reset();
            bitmap.compress(Bitmap.CompressFormat.JPEG, options, output);
            options -= 10;
        }

        return output.toByteArray();
    }

    private void showToast(@StringRes int stringId) {
        if (appContext != null) {
            Toast.makeText(appContext, stringId, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 通过ContentResolver获取图片Uri(FileProvider.getUriForFile拿到的Uri微信识别不了，分享报错"分享失败 多文件分享只支持照片格式")
     *
     * @param context   context
     * @param imageFile imageFile
     */
    private static Uri getImageContentUri(Context context, File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID}, MediaStore.Images.Media.DATA + "=? ",
                new String[]{filePath}, null);
        Uri uri = null;

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
                Uri baseUri = Uri.parse("content://media/external/images/media");
                uri = Uri.withAppendedPath(baseUri, "" + id);
            }

            cursor.close();
        }

        if (uri == null) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DATA, filePath);
            uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        }

        return uri;
    }

    /**
     * 通过ContentResolver获取视频Uri(FileProvider.getUriForFile拿到的Uri微信识别不了，分享报错"分享失败 多文件分享只支持照片格式")
     *
     * @param context   context
     * @param videoFile videoFile
     */
    private static Uri getVideoContentUri(Context context, File videoFile) {
        Uri uri = null;
        String filePath = videoFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Video.Media._ID}, MediaStore.Video.Media.DATA + "=? ",
                new String[]{filePath}, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
                Uri baseUri = Uri.parse("content://media/external/video/media");
                uri = Uri.withAppendedPath(baseUri, "" + id);
            }

            cursor.close();
        }

        if (uri == null) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Video.Media.DATA, filePath);
            uri = context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        }

        return uri;
    }

    /**
     * 获取小程序封面图片字节数组，不超过128KB
     *
     * @param bitmap 原始bitmap
     * @return byte[]
     */
    private byte[] getThumb(Bitmap bitmap) {
        int byteCount = bitmap.getByteCount();
        ByteBuffer buf = ByteBuffer.allocate(byteCount);
        bitmap.copyPixelsToBuffer(buf);
        byte[] byteArray = buf.array();
        if (byteCount > MAX_MINI_PROGRAM_THUMB_SIZE) {
            return bitmap2Bytes(bitmap, MAX_MINI_PROGRAM_THUMB_SIZE);
        } else {
            return byteArray;
        }
    }
}

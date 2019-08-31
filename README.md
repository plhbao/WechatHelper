# JlWechat

## 介绍：
### 对微信支付、登录、分享等一系列事件的封装，方便开辟新工程时快速部署微信相关

## 摘要：
### 封装过程中使用了一个 activity-alias 方式将 WXEntryActivity 和 WXPayEntryActivity 这两个本来需要写入工程下特定路径的类重定向到库的路径下面，详细介绍见 [activity-alias](https://developer.android.com/guide/topics/manifest/activity-alias-element)


```
<application
    android:allowBackup="true"
    android:largeHeap="true"
    android:supportsRtl="true">
    <activity
        android:name=".wxapi.WXEntryActivity"
        android:exported="true"
        android:launchMode="singleTop"
        android:theme="@android:style/Theme.Translucent" />
    <activity
        android:name=".wxapi.WXPayEntryActivity"
        android:exported="true"
        android:launchMode="singleTop"
        android:screenOrientation="portrait" />
    <activity-alias
        android:name="${applicationId}.wxapi.WXEntryActivity"
        android:exported="true"
        android:targetActivity=".wxapi.WXEntryActivity" />
    <activity-alias
        android:name="${applicationId}.wxapi.WXPayEntryActivity"
        android:exported="true"
        android:targetActivity=".wxapi.WXPayEntryActivity" />
</application>
```

## 类图：
![image.png](https://cdn.nlark.com/yuque/0/2019/png/375089/1566795881500-09576bf6-44d6-455e-b3e1-7d9d2615f96c.png#align=left&display=inline&height=1533&name=image.png&originHeight=1533&originWidth=1813&size=262544&status=done&width=1813)

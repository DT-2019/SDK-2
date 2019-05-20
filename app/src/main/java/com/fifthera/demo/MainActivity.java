package com.fifthera.demo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;

import com.ali.auth.third.ui.context.CallbackContext;
import com.alibaba.baichuan.android.trade.AlibcTradeSDK;
import com.alibaba.baichuan.android.trade.callback.AlibcTradeInitCallback;
import com.fifthera.ecwebview.ECWebView;
import com.fifthera.ecwebview.ECWebViewClient;
import com.fifthera.ecwebview.ErrorCode;
import com.fifthera.ecwebview.HomePageInterceptListener;
import com.fifthera.ecwebview.JSApi;
import com.fifthera.ecwebview.OnApiResponseListener;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    //淘宝 授权 测试环境-true，正式环境-false
    public static boolean isDebug = true;
    //流量主-平台申请的clientId
    public static final String clientId = "XXXXXXXXXXXXXXX";
    //流量主-平台申请的clientSecret
    private static final String clientSecret = "XXXXXXXXXXXXXXXXX";

    private String TAG = "MainActivity";
    private Context mContext;
    private ECWebView mWebView;
    private JSApi mApi;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //百川初始化
        initAlibc();
        mContext = this;
        mWebView = findViewById(R.id.ec_webview);
        mApi = new JSApi(this);

        mApi.setBaichuanParams(clientId, isDebug);
        mWebView.addJavascriptObject(mApi);

        mWebView.shouldInterceptHomePageUrl(new HomePageInterceptListener() {
            @Override
            public boolean interceptUrl(String s) {
                //点击商品列表页的商品，打开新的页面展示商品详情
                ECWebviewActivity.invoke(mContext, s);
                return true;
            }
        });

        //处理JSApi返回的结果
        mApi.setOnApiResponseListener(new OnApiResponseListener() {
            @Override
            public void fail(int errorCode) {
                if (errorCode == ErrorCode.TOKEN_FAIL) {
                    Toast.makeText(MainActivity.this, "token失效了", Toast.LENGTH_SHORT).show();
                }
                if (errorCode == ErrorCode.COMPOSITE_FAIL) {
                    Toast.makeText(MainActivity.this, "图片合成失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void getShareImageBitmap(Bitmap bitmap, String text, int shareType) {
                // 处理需要分享的图片
            }

            @Override
            public void goBack() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onBackPressed();
                    }
                });
            }

            @Override
            public void authoResult(JSONObject result) {
                //此方法可不必实现
            }
        });

        //兜推SDK授权,分两种方式，loadUrl()为客户端授权方式
        loadUrl();

    }

    //流量主后端实现授权
    public void loadUrl2() {
        mWebView.loadUrl("");
    }

    private void loadUrl() {
        // 用户id-可标识用户的唯一信息。订单记录，佣金分成等功能都需要通过用户id进行操作
        String uid = "123456780";
        long currnetTime = System.currentTimeMillis() / 1000;  //时间戳
        String sign = getSign(clientId, clientSecret, currnetTime, uid);
        StringBuilder str = new StringBuilder();
        //注意区分正式环境和测试环境
        if (isDebug) {
            str.append("https://ec-api-test.thefifthera.com/");
        } else {
            str.append("https://ec-api.thefifthera.com/");
        }

        str.append("h5/v1/auth/redirect?client_id=")
                .append(clientId)
                .append("&sign=")
                .append(sign)
                .append("&timestamp=")
                .append(currnetTime)
                .append("&uid=")
                .append(uid)
                .append("&type=page.goods");
        String url = str.toString();
        mWebView.loadUrl(url);
    }

    private String getSign(String clientId, String clientSecret, long currentTime, String uid) {
        StringBuilder str = new StringBuilder();
        str.append(clientSecret);
        str.append("client_id").append(clientId)
                .append("timestamp").append(currentTime)
                .append("type").append("page.goods")
                .append("uid").append(uid);
        str.append(clientSecret);

        String s = new String(Hex.encodeHex(DigestUtils.md5(str.toString())));
        return s.toUpperCase();
    }

    //百川授权，最好放在application中进行
    private void initAlibc() {
        AlibcTradeSDK.asyncInit(this.getApplication(), new AlibcTradeInitCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "asyncInit onSuccess:");
            }

            @Override
            public void onFailure(int i, String s) {
                Log.d(TAG, "asyncInit onFailure: i:" + i + "   s:" + s);
            }
        });
    }

    //阿里百川授权结果返回回调
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        CallbackContext.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWebView != null) {
            mWebView.removeAllViews();
            mWebView = null;
        }
        mApi = null;
        //百川资源销毁，防止内存泄漏
        AlibcTradeSDK.destory();
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}

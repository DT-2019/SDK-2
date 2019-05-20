package com.fifthera.demo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;
import android.widget.Toast;

import com.fifthera.alibaichuan.AuthRefreshEvent;
import com.fifthera.ecwebview.ECWebChromeClient;
import com.fifthera.ecwebview.ECWebView;
import com.fifthera.ecwebview.ErrorCode;
import com.fifthera.ecwebview.HomePageInterceptListener;
import com.fifthera.ecwebview.JSApi;
import com.fifthera.ecwebview.OnApiResponseListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;

import static com.fifthera.demo.MainActivity.clientId;
import static com.fifthera.demo.MainActivity.isDebug;
import static com.fifthera.ecwebview.BitmapShareType.TYPE_WECHAT;
import static com.fifthera.ecwebview.BitmapShareType.TYPE_WECHAT_MOMENT;

public class ECWebviewActivity extends AppCompatActivity {
    private ECWebView mWebView;
    private JSApi mApi;
    private Context mContext;

    public static void invoke(Context context, String url) {
        Intent intent = new Intent(context, ECWebviewActivity.class);
        intent.putExtra("web_url", url);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        setContentView(R.layout.ec_webview_activity);
        final String webUrl = getIntent().getStringExtra("web_url");
        mContext = this;
        mWebView = findViewById(R.id.new_ec_webview);

        mApi = new JSApi(this);
        mApi.setBaichuanParams(clientId, isDebug);
        mWebView.addJavascriptObject(mApi);

        mApi.setOnApiResponseListener(new OnApiResponseListener() {

            @Override
            public void fail(int i) {
                if (i == ErrorCode.TOKEN_FAIL) {
                    Toast.makeText(ECWebviewActivity.this, "token失效了", Toast.LENGTH_SHORT).show();
                }
                if (i == ErrorCode.COMPOSITE_FAIL) {
                    Toast.makeText(ECWebviewActivity.this, "图片合成失败,请重新分享", Toast.LENGTH_SHORT).show();
                }
            }

            /**
             * 分享图片到相应的渠道
             * @param bitmap  需要分享出去的图片
             * @param s  需要复制的文案
             * @param i   分享渠道： 微信好友 - 0x01 ,  微信朋友圈 - 0x02
             */
            @Override
            public void getShareImageBitmap(Bitmap bitmap, String s, int i) {
               // Toast.makeText(mContext, "别忘了发送复制内容哦~ ，在输入文字地方点击，粘贴文字即可", Toast.LENGTH_SHORT).show();
                //TODO copy s
                Toast.makeText(mContext, "分享调用成功 s:"+s+"  i:"+i, Toast.LENGTH_SHORT).show();
                if (i == TYPE_WECHAT) {
                    //TODO 分享到微信好友
                } else if (i == TYPE_WECHAT_MOMENT) {
                    //TODO 分享到微信朋友圈
                }
            }

            /**
             * H5的返回事件
             */
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

            }
        });

        mWebView.shouldInterceptHomePageUrl(new HomePageInterceptListener() {
            @Override
            public boolean interceptUrl(String s) {
                ECWebviewActivity.invoke(mContext, s);
                return true;
            }
        });

        mWebView.setOnWebChromeClientListener(new ECWebChromeClient() {
            @Override
            public void onProgressChanged(WebView webView, int i) {
                super.onProgressChanged(webView, i);
                //可以在这里加入进度条相关的显示
            }
        });

        mWebView.loadUrl(webUrl);
    }

    //通过eventbus方式进行消息传递
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void authoSuccess(AuthRefreshEvent event) {
        if (event.isRefresh() && mWebView != null) {
            mWebView.authoParams(event.getObject());
        }
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        if (mWebView != null) {
            mWebView.removeAllViews();
            mWebView = null;
        }
        mApi = null;
    }
}

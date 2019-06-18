package com.fifthera.demo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;

import com.fifthera.alibaichuan.AuthRefreshEvent;
import com.fifthera.ecwebview.ECWebChromeClient;
import com.fifthera.ecwebview.ECWebView;
import com.fifthera.ecwebview.ErrorCode;
import com.fifthera.ecwebview.HomePageInterceptListener;
import com.fifthera.ecwebview.JSApi;
import com.fifthera.ecwebview.OnApiResponseListener;
import com.kepler.jd.Listener.OpenAppAction;
import com.kepler.jd.login.KeplerApiManager;
import com.kepler.jd.sdk.bean.KeplerAttachParameter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;

import static com.fifthera.demo.MainActivity.clientId;
import static com.fifthera.demo.MainActivity.isDebug;
import static com.fifthera.ecwebview.BitmapShareType.TYPE_WECHAT;
import static com.fifthera.ecwebview.BitmapShareType.TYPE_WECHAT_MOMENT;

public class ECWebviewActivity extends AppCompatActivity {
    private KeplerAttachParameter mKeplerAttachParameter = new KeplerAttachParameter();
    private Handler mHandler = new Handler();
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
            public void calendarCallback() {
                requestPermission();
            }

            @Override
            public void jdShoppingCallback(String s) {
                openJdUrl(s);
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

    public void requestPermission() {
        String permissions[] = new String[]{
                Manifest.permission.WRITE_CALENDAR,
                Manifest.permission.READ_CALENDAR};

        if (Build.VERSION.SDK_INT >= 23) {
            boolean needRequest = false;
            for (int i = 0; i < permissions.length; i++) {
                int chechpermission = ContextCompat.checkSelfPermission(getApplicationContext(), permissions[i]);
                if (chechpermission != PackageManager.PERMISSION_GRANTED) {
                    needRequest = true;
                }
            }
            if (needRequest) {
                ActivityCompat.requestPermissions(ECWebviewActivity.this, permissions, 1);
            }
        }
    }
    private void openJdUrl(String url) {
        OpenAppAction mOpenAppAction = new OpenAppAction() {
            @Override
            public void onStatus(final int status, final String url) {
                mHandler.post(
                        new Runnable() {
                            @Override
                            public void run() {
                                if (status == OpenAppAction.OpenAppAction_start) {
                                    //开始状态未必一定执行，
                                    Log.d("lzh", "OpenAppAction start");
                                } else {
                                    //  mKelperTask = null;
                                    //  dialogDiss();
                                    Log.d("lzh", "mKelperTask = null");
                                }
                                if (status == OpenAppAction.OpenAppAction_result_NoJDAPP) {
                                    //未安装京东
                                    Log.d("lzh","OpenAppAction_result_NoJDAPP");

                                    mWebView.loadUrl(url);


                                } else if (status == OpenAppAction.OpenAppAction_result_BlackUrl) {
                                    //不在白名单
                                    Log.d("lzh","OpenAppAction_result_BlackUrl");
                                } else if (status == OpenAppAction.OpenAppAction_result_ErrorScheme) {
                                    //协议错误
                                    Log.d("lzh","OpenAppAction_result_ErrorScheme");
                                } else if (status == OpenAppAction.OpenAppAction_result_APP) {
                                    //呼京东成功
                                    Log.d("lzh","OpenAppAction_result_APP");
                                } else if (status == OpenAppAction.OpenAppAction_result_NetError) {
                                    Log.d("lzh","OpenAppAction_result_NetError");
                                    //网络异常
                                }
                            }
                        });
            }
        };
        KeplerApiManager.getWebViewService().openAppWebViewPage(this, url, mKeplerAttachParameter, mOpenAppAction);
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

package com.example.open_sample;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.connect.UserInfo;
import com.tencent.connect.auth.QQAuth;
import com.tencent.connect.auth.QQToken;
import com.tencent.connect.common.Constants;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity implements View.OnClickListener {
    TextView openidTextView;
    TextView nicknameTextView;
    Button loginButton;
    ImageView userlogo;
    private Tencent mTencent;
    public static QQAuth mQQAuth;
    public static String mAppid;
    public static String openidString;
    public static String nicknameString;
    Bitmap bitmap = null;
    private BaseUiListener listener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //用来登录的Button
        loginButton = (Button) findViewById(R.id.login);
        loginButton.setOnClickListener(this);
        //用来显示OpenID的textView
        openidTextView = (TextView) findViewById(R.id.user_openid);
        //用来显示昵称的textview
        nicknameTextView = (TextView) findViewById(R.id.user_nickname);
        //用来显示头像的Imageview
        userlogo = (ImageView) findViewById(R.id.user_logo);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.login:
                LoginQQ();
                break;
            default:
                break;
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REQUEST_LOGIN) {
            if (resultCode == Constants.ACTIVITY_OK || resultCode == Constants.ACTIVITY_CANCEL) {
                //如果少了这句，监听器的没效果。onComplete不会执行
                Tencent.handleResultData(data, listener);
            }
        }

    }

    public void LoginQQ() {
        mAppid = AppConstant.APP_ID;
        mTencent = Tencent.createInstance(mAppid, getApplicationContext());
        listener = new BaseUiListener();
        mTencent.login(MainActivity.this, "all", listener);
    }

    private class BaseUiListener implements IUiListener {
        public void onComplete(Object response) {
            Toast.makeText(getApplicationContext(), "登录成功", Toast.LENGTH_SHORT).show();
            try {
                String token = ((JSONObject) response).getString("access_token");
                String expires = ((JSONObject) response).getString("expires_in");
                String openId = ((JSONObject) response).getString("openid");
                //设置token
                mTencent.setAccessToken(token, expires);
                //设置openid
                mTencent.setOpenId(openId);
                openidString = ((JSONObject) response).getString("openid");
                openidTextView.setText(openidString);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            QQToken qqToken = mTencent.getQQToken();
            UserInfo info = new UserInfo(getApplicationContext(), qqToken);
            info.getUserInfo(new IUiListener() {
                public void onComplete(final Object response) {
                    Message msg = new Message();
                    msg.obj = response;
                    msg.what = 0;
                    mHandler.sendMessage(msg);
                    new Thread() {
                        @Override
                        public void run() {
                            JSONObject json = (JSONObject) response;
                            try {
                                bitmap = Util.getbitmap(json.getString("figureurl_qq_2"));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            Message msg = new Message();
                            msg.obj = bitmap;
                            msg.what = 1;
                            mHandler.sendMessage(msg);
                        }
                    }.start();
                }

                public void onCancel() {

                }

                public void onError(UiError arg0) {

                }
            });
        }

        public void onCancel() {
            Toast.makeText(getApplicationContext(), "取消登录", Toast.LENGTH_SHORT).show();
        }

        public void onError(UiError arg0) {
            Toast.makeText(getApplicationContext(), "登陆失败", Toast.LENGTH_SHORT).show();
        }
    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                JSONObject response = (JSONObject) msg.obj;
                if (response.has("nickname")) {
                    try {
                        nicknameString = response.getString("nickname");
                        nicknameTextView.setText(nicknameString);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } else if (msg.what == 1) {
                Bitmap bitmap = (Bitmap) msg.obj;
                userlogo.setImageBitmap(bitmap);
            }
        }
    };
}
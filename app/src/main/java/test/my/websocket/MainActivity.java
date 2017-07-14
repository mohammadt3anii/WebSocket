package test.my.websocket;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import test.my.websocket.socket.WebSocket;
import test.my.websocket.socket.WebSocketCall;
import test.my.websocket.socket.WebSocketListener;

public class MainActivity extends AppCompatActivity {


    final  static OkHttpClient mOkHttpClient = new OkHttpClient.Builder()
            .readTimeout(3000, TimeUnit.SECONDS)//设置读取超时时间
            .writeTimeout(3000, TimeUnit.SECONDS)//设置写的超时时间
            .connectTimeout(3000, TimeUnit.SECONDS)//设置连接超时时间
            .build();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new Thread(){
            @Override
            public void run() {
                webSocketTest();
            }
        }.start();
    }

    public void webSocketTest(){
        String url="ws://10.0.0.20:8080/hyt/websocket.do?userId=1&userType=2"; //改成自已服务端的地址
        Request request = new Request.Builder().url(url).build();
        WebSocketCall webSocketCall = WebSocketCall.create(mOkHttpClient, request);
        webSocketCall.enqueue(new WebSocketListener() {
            private final ExecutorService sendExecutor = Executors.newSingleThreadExecutor();
            private WebSocket webSocket;
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d("WebSocketCall", "onOpen");
                this.webSocket=webSocket;
            }
            /**
             * 连接失败
             * @param e
             * @param response Present when the failure is a direct result of the response (e.g., failed
             * upgrade, non-101 response code, etc.). {@code null} otherwise.
             */
            @Override
            public void onFailure(IOException e, Response response) {
                Log.d("WebSocketCall","onFailure");
            }

            /**
             * 接收到消息
             * @param message
             * @throws IOException
             */
            @Override
            public void onMessage(ResponseBody message) throws IOException {
                final RequestBody response;
                Log.d("WebSocketCall", "onMessage:" + message.source().readByteString().utf8());
                if (message.contentType() == WebSocket.TEXT) {//
                    response = RequestBody.create(WebSocket.TEXT, "你好");//文本格式发送消息
                } else {
                    BufferedSource source = message.source();
                    Log.d("WebSocketCall", "onMessage:" + source.readByteString());
                    response = RequestBody.create(WebSocket.BINARY, source.readByteString());
                }
                message.source().close();
                sendExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000*60);
                            webSocket.sendMessage(response);//发送消息
                        } catch (IOException e) {
                            e.printStackTrace(System.out);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void onPong(Buffer payload) {
                Log.d("WebSocketCall", "onPong:");
            }


            /**
             * 关闭
             * @param code The <a href="http://tools.ietf.org/html/rfc6455#section-7.4.1">RFC-compliant</a>
             * status code.
             * @param reason Reason for close or an empty string.
             */
            @Override
            public void onClose(int code, String reason) {
                sendExecutor.shutdown();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

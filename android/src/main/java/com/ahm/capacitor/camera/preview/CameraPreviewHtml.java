package com.ahm.capacitor.camera.preview;

import static android.Manifest.permission.CAMERA;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.pm.ActivityInfo;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.getcapacitor.JSObject;
import com.getcapacitor.Logger;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import java.io.IOException;
import org.java_websocket.server.WebSocketServer;

@CapacitorPlugin(name = "CameraPreviewHtml", permissions = { @Permission(strings = { CAMERA }, alias = CameraPreview.CAMERA_PERMISSION_ALIAS) })
public class CameraPreviewHtml extends CameraPreview {
    private WebView webView;
    private WebSocketServer webSocketServer;
    private Camera camera;

    @Override
    public void createPreview() {
        webView = new WebView(getContext());
        webView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        webView.setWebViewClient(new WebViewClient());
        webView.loadData("<video id='camera-preview' autoplay></video>", "text/html", "UTF-8");
        addView(webView);

        webSocketServer = new WebSocketServer(8080) {
            @Override
            public void onOpen(WebSocket conn, ClientHandshake handshake) {
                startCamera();
            }

            @Override
            public void onMessage(WebSocket conn, String message) {
                // Handle messages if needed
                Logger.d("WebSocket", "Received message: " + message);
            }

            @Override
            public void onClose(WebSocket conn, int code, String reason, boolean remote) {
                stopCamera();
            }

            @Override
            public void onError(WebSocket conn, Exception ex) {
                Log.e("WebSocket", "Error: " + ex.getMessage());
            }
        };
    }

    @Override
    public void startCamera() {
        try {
            camera = Camera.open();
            camera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    // Convert the byte array to base64 or another format suitable for video streaming
                    String videoData = convertToBase64(data);
                    if (webSocketServer != null) {
                        webSocketServer.broadcast(videoData); // Send data to all connected clients
                    }
                }
            });
            camera.setPreviewDisplay(new SurfaceView(getContext()).getHolder());
            camera.startPreview();
        } catch (IOException e) {
            Log.e("CameraPreview", "Error starting camera: " + e.getMessage());
        }
    }

    @Override
    public void stopCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
        if (webSocketServer != null) {
            webSocketServer.stop();
        }
    }

    private String convertToBase64(byte[] data) {
        // Convert the camera frame byte array to a base64 string or appropriate format
        // Return the base64 encoded string or relevant format for video playback
        return Base64.encodeToString(data, Base64.DEFAULT);
    }
}

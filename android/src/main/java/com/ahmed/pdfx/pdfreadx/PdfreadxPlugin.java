package com.ahmed.pdfx.pdfreadx;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;

import java.util.Map;
import java.util.Objects;

import io.flutter.Log;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/** PdfreadxPlugin */
public class PdfreadxPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;
  private Context applicationContext;
  private Activity currentActivity;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "pdfreadx");
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
    }else if (call.method.equals("startPDFViewActivity")) {
      if (currentActivity == null) {
        result.error("NO_ACTIVITY", "Plugin not attached to an activity.", null);
        return;
      }
      try {
        Intent intent = new Intent(currentActivity, PDFViewActivity.class);

        // Optional: Pass arguments from Flutter to the native Activity
        if (call.hasArgument("data")) {
          //Log.d("PdfreadxPlugin", "Received data from Flutter: " + call.argument("data"));
          Map<String, Object> args = call.argument("data");
          //Log.d("PdfreadxPlugin", "Received data from Flutter: " + args.get("message"));
          String message = (String) Objects.requireNonNull(args).get("message");
          String filePath = (String) Objects.requireNonNull(args).get("filePath");
          String fileName = (String) Objects.requireNonNull(args).get("fileName");
          intent.putExtra("message_from_flutter", message);
          intent.putExtra("filePath", filePath);
          intent.putExtra("fileName", fileName);
        }
        currentActivity.startActivity(intent);
        result.success("Native Activity Started from Java"); // Or null
      } catch (Exception e) {
        result.error("START_ACTIVITY_FAILED", "Failed to start native activity: " + e.getMessage(), null);
      }
    }else {
      result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  // ActivityAware methods
  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    currentActivity = binding.getActivity();
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    currentActivity = null;
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    currentActivity = binding.getActivity();
  }

  @Override
  public void onDetachedFromActivity() {
    currentActivity = null;
  }
}

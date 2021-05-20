package com.apptentive.apptentive_flutter;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apptentive.android.sdk.Apptentive;
import com.apptentive.android.sdk.ApptentiveConfiguration;

import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import static com.apptentive.apptentive_flutter.PluginUtils.unpackConfiguration;

/** ApptentiveFlutterPlugin */
public class ApptentiveFlutterPlugin implements FlutterPlugin, MethodCallHandler {
  private static final String ERROR_CODE_NO_APPLICATION = "100";

  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;

  /**
   * Current application object
   */
  private @Nullable Application application;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "apptentive_flutter");
    channel.setMethodCallHandler(this);

    application = (Application) flutterPluginBinding.getApplicationContext();
    Apptentive.registerCallbacks(application);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (call.method.equals("register")) {
      register(call, result);
    } else if (call.method.equals("showMessageCenter")) {
      showMessageCenter(call, result);
    } else {
      result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  //region Methods

  private void register(@NonNull MethodCall call, @NonNull Result result) {
    if (application == null) {
      result.error(ERROR_CODE_NO_APPLICATION, "Unable to register Apptentive SDK", null); // TODO: provide a better error details
      return;
    }

    @SuppressWarnings("unchecked")
    ApptentiveConfiguration configuration = unpackConfiguration((Map<String, Object>) call.arguments);
    Apptentive.register(application, configuration);
    result.success(true);
  }

  private void showMessageCenter(@NonNull MethodCall call, @NonNull final Result result) {
    if (application == null) {
      result.error(ERROR_CODE_NO_APPLICATION, "Unable to show message center", null); // TODO: provide a better error details
      return;
    }

    @SuppressWarnings("unchecked")
    final Map<String, Object> customData = (Map<String, Object>) call.arguments;

    Apptentive.showMessageCenter(application, new Apptentive.BooleanCallback() {
        @Override
        public void onFinish(boolean showed) {
            result.success(showed);
        }
    }, customData);
  }

  //endregion
}

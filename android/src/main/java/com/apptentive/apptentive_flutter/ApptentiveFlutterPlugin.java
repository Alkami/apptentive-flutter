package com.apptentive.apptentive_flutter;

import android.app.Application;
import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apptentive.android.sdk.Apptentive;
import com.apptentive.android.sdk.ApptentiveConfiguration;
import com.apptentive.android.sdk.module.survey.OnSurveyFinishedListener;
import com.apptentive.android.sdk.module.messagecenter.UnreadMessagesListener;

import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import static com.apptentive.apptentive_flutter.PluginUtils.getStackTrace;
import static com.apptentive.apptentive_flutter.PluginUtils.parsePushProvider;
import static com.apptentive.apptentive_flutter.PluginUtils.unpackConfiguration;

/** ApptentiveFlutterPlugin */
public class ApptentiveFlutterPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
  private static final String ERROR_CODE_NO_APPLICATION = "100";
  private static final String ERROR_CODE_ARGUMENT_ERROR = "200";
  private static final String ERROR_CODE_EXCEPTION = "300";

  /// The MethodChannel that will communicate between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;

  // Current Application object
  private @Nullable Application application;

  // Current Activity object
  private @Nullable Activity activity;

  private UnreadMessagesListener unreadMessagesListener;
  private OnSurveyFinishedListener surveyFinishedListener;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "apptentive_flutter");
    channel.setMethodCallHandler(this);

    application = (Application) flutterPluginBinding.getApplicationContext();

    Apptentive.registerCallbacks(application);
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding activityPluginBinding) {
    this.activity = (Activity) activityPluginBinding.getActivity();
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    try {
      switch (call.method) {
        case "register":
          register(call, result);
          break;
        case "showMessageCenter":
          showMessageCenter(call, result);
          break;
        case "engage":
          engage(call, result);
          break;
        case "canShowInteraction":
          canShowInteraction(call, result);
          break;
        case "setPersonName":
          setPersonName(call, result);
          break;
        case "setPersonEmail":
          setPersonEmail(call, result);
          break;
        case "addCustomPersonData":
          addCustomPersonData(call, result);
          break;
        case "removeCustomPersonData":
          removeCustomPersonData(call, result);
          break;
        case "addCustomDeviceData":
          addCustomDeviceData(call, result);
          break;
        case "removeCustomDeviceData":
          removeCustomDeviceData(call, result);
          break;
        case "setPushNotificationIntegration":
          setPushNotificationIntegration(call, result);
          break;
        case "getUnreadMessageCount":
          getUnreadMessageCount(call, result);
          break;
        case "registerListeners":
          registerListeners(call, result);
          break;
        case "handleRequestPushPermissions":
          // Do nothing to avoid not implemented result
          break;
        default:
          result.notImplemented();
          break;
      }
    } catch (Exception e) {
      result.error(
              ERROR_CODE_EXCEPTION,
              "Exception while invoking method " + call.method + " with arguments: " + call.arguments + "\n" + getStackTrace(e),
              null
      );
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  @Override
  public void onDetachedFromActivity() {
    this.activity = null;
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    this.activity = null;
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding activityPluginBinding) {
    this.activity = (Activity) activityPluginBinding.getActivity();
  }

  //region Methods

  private void register(@NonNull MethodCall call, @NonNull Result result) {
    if (application == null) {
      result.error(ERROR_CODE_NO_APPLICATION, "Unable to register Apptentive SDK, FlutterPluginBinding Application is null.", null);
      return;
    }
    @SuppressWarnings("unchecked")
    ApptentiveConfiguration configuration = unpackConfiguration((Map<String, Object>) call.argument("configuration"));
    Apptentive.register(application, configuration);
    result.success(true);
  }

  // Register listeners for native callbacks:
  // onSurveyFinished and onUnreadMessageCountChanged
  private void registerListeners(@NonNull MethodCall call, @NonNull final Result result){
    surveyFinishedListener = new OnSurveyFinishedListener() {
      @Override
      public void onSurveyFinished(boolean completed) {
        channel.invokeMethod("onSurveyFinished",PluginUtils.map("completed", completed));
      }
    };
    Apptentive.setOnSurveyFinishedListener(surveyFinishedListener);
    unreadMessagesListener = new UnreadMessagesListener() {
      @Override
      public void onUnreadMessageCountChanged(int unreadMessages) {
        channel.invokeMethod("onUnreadMessageCountChanged",PluginUtils.map("count", unreadMessages));
      }
    };
    Apptentive.addUnreadMessagesListener(unreadMessagesListener);
    result.success(true);
  }

  private void engage(@NonNull MethodCall call, @NonNull final Result result) {
    final String event = call.argument("event_name");
    final Map<String, Object> customData = call.argument("custom_data");

    if (application == null) {
      result.error(ERROR_CODE_NO_APPLICATION, "Unable to engage event: " + event + ", bound Application is null.", null);
      return;
    }

    Apptentive.engage(activity, event, new Apptentive.BooleanCallback() {
      @Override
      public void onFinish(boolean engaged) {
        result.success(engaged);
      }
    }, customData);
  }

  private void canShowInteraction(@NonNull MethodCall call, @NonNull final Result result) {
    if (application == null) {
      result.error(ERROR_CODE_NO_APPLICATION, "Unable to check if interaction can be shown, bound Application is null.", null);
      return;
    }

    final String event = call.argument("event_name");
    Apptentive.queryCanShowInteraction(event, new Apptentive.BooleanCallback() {
      @Override
      public void onFinish(boolean showed) {
        result.success(showed);
      }
    });
  }

  private void showMessageCenter(@NonNull MethodCall call, @NonNull final Result result) {
    if (application == null) {
      result.error(ERROR_CODE_NO_APPLICATION, "Unable to show message center, bound Application is null.", null);
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

  private void setPersonName(@NonNull MethodCall call, @NonNull final Result result) {
    if (application == null) {
      result.error(ERROR_CODE_NO_APPLICATION, "Unable to set person name, bound Application is null.", null);
      return;
    }
    final String name = call.argument("name");
    Apptentive.setPersonName(name);
    result.success(true);
  }

  private void setPersonEmail(@NonNull MethodCall call, @NonNull final Result result) {
    if (application == null) {
      result.error(ERROR_CODE_NO_APPLICATION, "Unable to set person email, bound Application is null.", null);
      return;
    }
    final String email = call.argument("email");
    Apptentive.setPersonEmail(email);
    result.success(true);
  }

  private void addCustomPersonData(@NonNull MethodCall call, @NonNull final Result result) {
    final String key = call.argument("key");
    final Object value = call.argument("value");
    if (value instanceof String || value == null) {
      Apptentive.addCustomPersonData(key, (String) value);
    } else if (value instanceof Boolean) {
      Apptentive.addCustomPersonData(key, (Boolean) value);
    } else if (value instanceof Number) {
      Apptentive.addCustomPersonData(key, (Number) value);
    } else {
      result.error(
        ERROR_CODE_ARGUMENT_ERROR,
        "Unable to add custom person data for key '" + key + "': unexpected type " + value.getClass(),
        null
      );
      return;
    }
    result.success(true);
  }

  private void removeCustomPersonData(@NonNull MethodCall call, @NonNull final Result result) {
    final String key = call.argument("key");
    Apptentive.removeCustomPersonData(key);
    result.success(true);
  }

  private void addCustomDeviceData(@NonNull MethodCall call, @NonNull final Result result) {
    final String key = call.argument("key");
    final Object value = call.argument("value");
    if (value instanceof String || value == null) {
      Apptentive.addCustomDeviceData(key, (String) value);
    } else if (value instanceof Boolean) {
      Apptentive.addCustomDeviceData(key, (Boolean) value);
    } else if (value instanceof Number) {
      Apptentive.addCustomDeviceData(key, (Number) value);
    } else {
      result.error(
        ERROR_CODE_ARGUMENT_ERROR,
        "Unable to add custom device data for key '" + key + "': unexpected type " + value.getClass(),
        null
      );
      return;
    }
    result.success(true);
  }

  private void removeCustomDeviceData(@NonNull MethodCall call, @NonNull final Result result) {
    final String key = call.argument("key");
    Apptentive.removeCustomDeviceData(key);
    result.success(true);
  }

  private void setPushNotificationIntegration(@NonNull MethodCall call, @NonNull final Result result) {
    final int pushProvider = parsePushProvider((String) call.argument("push_provider"));
    final String token = call.argument("token");
    Apptentive.setPushNotificationIntegration(pushProvider, token);
    result.success(true);
  }

  private void getUnreadMessageCount(@NonNull MethodCall call, @NonNull final Result result) {
    int unreadMessages = Apptentive.getUnreadMessageCount();
    result.success(unreadMessages);
  }

  //endregion
}

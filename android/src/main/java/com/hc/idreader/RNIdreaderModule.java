
package com.hc.idreader;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.zkteco.android.biometric.core.device.ParameterHelper;
import com.zkteco.android.biometric.core.device.TransportType;
import com.zkteco.android.biometric.core.utils.LogHelper;
import com.zkteco.android.biometric.module.idcard.IDCardReader;
import com.zkteco.android.biometric.module.idcard.IDCardReaderFactory;
import com.zkteco.android.biometric.module.idcard.exception.IDCardReaderException;
import com.zkteco.android.biometric.module.idcard.meta.IDCardInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class RNIdreaderModule extends ReactContextBaseJavaModule {

  private static final int VID = 1024;    //IDR VID
  private static final int PID = 50010;     //IDR PID
  private IDCardReader idCardReader = null;
  private boolean bopen = false;
  private boolean bStoped = false;
  private int mReadCount = 0;
  private CountDownLatch countdownLatch = new CountDownLatch(1);
  SharedPreferences sharedPref;
  private Context mContext = null;
  private UsbManager musbManager = null;
  private final String ACTION_USB_PERMISSION = "com.example.scarx.idcardreader.USB_PERMISSION";

  public RNIdreaderModule(ReactApplicationContext reactContext) {
    super(reactContext);
    mContext = reactContext;
  }

  @Override
  public String getName() {
    return "RNIdreader";
  }
  private BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();

      if (ACTION_USB_PERMISSION.equals(action))
      {
        synchronized (this)
        {
          UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
          if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
          {
          }
          else
          {
            Toast.makeText(mContext, "USB未授权", Toast.LENGTH_SHORT).show();
            //mTxtReport.setText("USB未授权");
          }
        }
      }
    }
  };

  private void RequestDevicePermission()
  {
    musbManager = (UsbManager)mContext.getSystemService(Context.USB_SERVICE);
    IntentFilter filter = new IntentFilter();
    filter.addAction(ACTION_USB_PERMISSION);
    filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
    mContext.registerReceiver(mUsbReceiver, filter);

    for (UsbDevice device : musbManager.getDeviceList().values())
    {
      if (device.getVendorId() == VID && device.getProductId() == PID)
      {
        Intent intent = new Intent(ACTION_USB_PERMISSION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
        musbManager.requestPermission(device, pendingIntent);
      }
    }
  }
  @Override
  public void initialize() {
    sharedPref = mContext.getSharedPreferences("IDREADER", Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = sharedPref.edit();
    editor.putString("IDREADERINFO", "f");

    editor.commit();
    // RequestDevicePermission();
    startIDCardReader();
  }

  public  String bytesToHexString(byte[] src){
    StringBuilder stringBuilder = new StringBuilder();
    if (src == null || src.length <= 0) {
      return null;
    }
    for (int i = 0; i < src.length; i++) {
      int v = src[i] & 0xFF;
      String hv = Integer.toHexString(v);
      if (hv.length() < 2) {
        stringBuilder.append(0);
      }
      stringBuilder.append(hv);
    }
    return stringBuilder.toString();
  }
  private void startIDCardReader() {
    // Define output log level
    LogHelper.setLevel(Log.VERBOSE);
    // Start fingerprint sensor
    Map idrparams = new HashMap();
    idrparams.put(ParameterHelper.PARAM_KEY_VID, VID);
    idrparams.put(ParameterHelper.PARAM_KEY_PID, PID);
    idCardReader = IDCardReaderFactory.createIDCardReader(mContext, TransportType.USB, idrparams);
  }

  public static void writeLogToFile(String log) {
    try {
      File dirFile = new File("/sdcard/zkteco/");  //目录转化成文件夹
      if (!dirFile.exists()) {              //如果不存在，那就建立这个文件夹
        dirFile.mkdirs();
      }
      String path = "/sdcard/zkteco/idrlog.txt";
      File file = new File(path);
      if (!file.exists()) {
        File dir = new File(file.getParent());
        dir.mkdirs();
        file.createNewFile();
      }
      FileOutputStream outStream = new FileOutputStream(file, true);
      log += "\r\n";
      outStream.write(log.getBytes());
      outStream.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onCatalystInstanceDestroy() {
    super.onCatalystInstanceDestroy();
    IDCardReaderFactory.destroy(idCardReader);

  }


  public static void sendEvent(ReactContext reactContext, String eventName, @Nullable WritableMap params) {
    reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);

  }


  @ReactMethod
  public void getIDStatus(Promise promise){
    //SharedPreferences sharedPref = MainActivity.getCurrentActivity().getSharedPreferences("IDREADER", Context.MODE_PRIVATE);
    String idStr = sharedPref.getString("IDREADERINFO", "f");
    promise.resolve(idStr);
  }


  @ReactMethod
  public void getIcardInfo(int type){

    startIDCardReader();
    RequestDevicePermission();
    Log.i("blue","tixcvxcvxc");
    try {
      if (bopen)
      {
        //SharedPreferences sharedPref = MainActivity.getCurrentActivity().getSharedPreferences("IDREADER", Context.MODE_PRIVATE);

        Toast.makeText(mContext, "身份证连接设备失败", Toast.LENGTH_SHORT).show();
        return;
      }

      idCardReader.open(0);
      bStoped = false;
      mReadCount = 0;
      writeLogToFile("连接设备成功");
      //   SharedPreferences sharedPref = MainActivity.getCurrentActivity().getSharedPreferences("REACT-NATIVE", Context.MODE_PRIVATE);
      SharedPreferences.Editor editor = sharedPref.edit();
      editor.putString("IDREADERINFO", "t");

      editor.commit();
      Toast.makeText(mContext, "身份证连接设备成功", Toast.LENGTH_SHORT).show();
      bopen = true;
      new Thread(new Runnable() {
        public void run() {
          while (!bStoped) {
            try {
              Thread.sleep(500);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
            try {
              boolean bSamStatus = false;
              try {
                bSamStatus = idCardReader.getStatus(0);
              } catch (IDCardReaderException e) {
                e.printStackTrace();
              }

              if (!bSamStatus) {
                try {
                  idCardReader.reset(0);
                } catch (IDCardReaderException e) {
                  e.printStackTrace();
                }
              }
              final IDCardInfo idCardInfo = new IDCardInfo();
              boolean ret = false;
              final long nTickstart = System.currentTimeMillis();
              try {
                idCardReader.findCard(0);
                idCardReader.selectCard(0);
              } catch (IDCardReaderException e) {
                LogHelper.e("errcode:" + e.getErrorCode() + ",internalerrorcode:" + e.getInternalErrorCode());
                //continue;
              }
              try {
                Thread.sleep(50);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
              try {
                ret = idCardReader.readCard(0, 0, idCardInfo);
              } catch (IDCardReaderException e) {
                writeLogToFile("读卡失败，错误信息：" + e.getMessage());
              }
              if (ret) {
                final long nTickUsed = (System.currentTimeMillis() - nTickstart);
                writeLogToFile("读卡成功：" + (++mReadCount) + "次" + "，耗时：" + nTickUsed + "毫秒");
                Log.i("blue", "读取次数：" + mReadCount + ",耗时：" + nTickUsed + "毫秒,姓名：" + idCardInfo.getName() + "，民族：" + idCardInfo.getNation() + "，住址：" + idCardInfo.getAddress() + ",身份证号：" + idCardInfo.getId());
                WritableMap params = Arguments.createMap();
                params.putString("address", idCardInfo.getAddress());
//                            params.putString("authority", idCardInfo.get);
                params.putString("birth", idCardInfo.getBirth());
                params.putString("birthPrim", idCardInfo.getBirth());

                params.putString("cardNo", idCardInfo.getId());
                params.putString("name", idCardInfo.getName());
                params.putString("period", idCardInfo.getValidityTime());
                params.putString("periodStart", "");

                params.putString("periodEnd", "");
                params.putString("sex", idCardInfo.getSex());
                params.putString("chineseName", idCardInfo.getName());

                sendEvent(getReactApplicationContext(), "READ_IREADER", params);
                idReaderClose();

//                            runOnUiThread(new Runnable() {
//                                public void run() {
//                                   // textView.setText("读取次数："  + mReadCount + ",耗时："+  nTickUsed +  "毫秒,姓名：" + idCardInfo.getName() + "，民族：" + idCardInfo.getNation() + "，住址：" + idCardInfo.getAddress() + ",身份证号：" + idCardInfo.getId());
//                                    if (idCardInfo.getPhotolength() > 0) {
//                                        //String orgPhotoData = Base64.encodeToString(idCardInfo.getPhoto(), Base64.NO_WRAP);
//                                        //保存OrgPhotoData,可以直接写文件，崩溃后给我日志就行。
//                                        byte[] buf = new byte[WLTService.imgLength];
//                                        if (1 == WLTService.wlt2Bmp(idCardInfo.getPhoto(), buf)) {
//                                           // imageView.setImageBitmap(IDPhotoHelper.Bgr2Bitmap(buf));
//                                        }
//                                    }
//                                }
//                            });
              }
            } catch (Exception e) {
              e.printStackTrace();
              startIDCardReader();
            }
          }
          countdownLatch.countDown();
        }
      }).start();
    }catch (IDCardReaderException e)
    {
      writeLogToFile("连接设备失败"+e.toString());
      SharedPreferences.Editor editor = sharedPref.edit();
      editor.putString("IDREADERINFO", "f");

      editor.commit();
      if (!bopen){
        RequestDevicePermission();
        //  getIcardInfo(1);
      }


    }

  }
  @ReactMethod
  public void cancelGetIcardInfo(){

    idReaderClose();
  }

  public void idReaderClose(){
    if (!bopen)
    {
      return;
    }
    bStoped = true;
    mReadCount = 0;
    try {
      countdownLatch.await(2, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    try {
      idCardReader.close(0);
    } catch (IDCardReaderException e) {
      e.printStackTrace();
    }
    // textView.setText("设备断开连接");
    bopen = false;
  }
}
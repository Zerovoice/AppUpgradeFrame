
package cn.bobby.appupgrade;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import cn.bobby.appupgrade.interfaces.IUpgradeInfoLinstener;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * <p>
 * Title: .
 * </p>
 * <p>
 * Description: .
 * </p>
 * 
 * @author Zouxiaobo(zeroapp@126.com) 2013-11-5.
 * @version $Id$
 */
public class UpgradeUIBuilder implements IUpgradeInfoLinstener {

    private static final String TAG = "AppUpgrade";
    public static final int NOTIFY_ID = 0;
    private Context mContext = null;
    private File updateFile = null;
    private NotificationManager mNotificationManager = null;
    private long downloadFileSize = 0;
    private long fileSize = 0;
    public Notification mNotification = null;
    private boolean cancelled = true;
    private String mAppUpgradeURL = "";
    private String pkgPath = "";

    public static final int ACTION_UPGRADE_DOWNLOAD_BEGIN = 1;
    public static final int ACTION_UPGRADE_DOWNLOAD_SUCCESS = 2;
    public static final int ACTION_UPGRADE_NETWORK_DISCONNECTED = 9;
    public static final int ACTION_UPGRADE_IOEXCEPTION = 10;

    private Handler progressHandler = new Handler() {

        @SuppressWarnings("deprecation")
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    int rate = msg.arg1;
//                    Log.i(TAG, "handleMessage: ---------1--------" + rate);
                    if (rate < 100) {
                        if (rate % 10 == 0) {
                            Log.i(TAG, "rate:" + rate);
                        }
                        // 更新进度
//                        RemoteViews contentView = mNotification.contentView;
//                        contentView.setTextViewText(R.id.rate, rate + "%");
//                        contentView.setProgressBar(R.id.progress, 100, rate, false);
                        // 最后别忘了通知一下,否则不会更新
//                        mNotificationManager.notify(NOTIFY_ID, mNotification);
                    } else {
                        // 下载完毕后变换通知形式
                        mNotification.flags = Notification.FLAG_AUTO_CANCEL;
                        // android应用升级接口
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.fromFile(updateFile),
                                "application/vnd.android.package-archive");
                        Log.i(TAG, "intent:" + intent.getAction());

                        // 更新参数,注意flags要使用FLAG_UPDATE_CURRENT
                        PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT);
                        mNotification.contentIntent = contentIntent;
                        CharSequence contentTitle = mContext.getApplicationInfo().loadLabel(
                                mContext.getPackageManager());
                        mNotification.setLatestEventInfo(mContext, contentTitle,
                                "New version available", contentIntent);
                        // 最后别忘了通知一下,否则不会更新
                        mNotificationManager.notify(NOTIFY_ID, mNotification);
                        onInfo(ACTION_UPGRADE_DOWNLOAD_SUCCESS);
                    }
                    break;
                case 0:
                    // 取消通知
                    Log.i(TAG, "handleMessage: ---------0--------");
                    mNotificationManager.cancel(NOTIFY_ID);
                    break;
            }
        };
    };

    public UpgradeUIBuilder(Context context, String URL) {
        this.mContext = context;
        this.mAppUpgradeURL = URL;
        pkgPath = Environment.getDataDirectory() + "/data/" + context.getPackageName();
        mNotificationManager = (NotificationManager) context
                .getSystemService(android.content.Context.NOTIFICATION_SERVICE);
        Log.i(TAG, "mNotificationManager:" + mNotificationManager);
    }

    public void downloadNewApp() {
        onInfo(ACTION_UPGRADE_DOWNLOAD_BEGIN);
        setUpNotification();
        new Thread() {
            public void run() {
                downloadFile(mAppUpgradeURL);
            };
        }.start();

        new Thread() {
            public void run() {
                readRate();
            };
        }.start();

    }

    /**
     * 创建通知
     */
    private void setUpNotification() {
        int icon = mContext.getApplicationInfo().icon;
        CharSequence tickerText = mContext.getApplicationInfo().loadLabel(
                mContext.getPackageManager());
        Log.i(TAG, "tickerText:" + tickerText);
        long when = System.currentTimeMillis();
        mNotification = new Notification(icon, tickerText, when);
//
//        // 放置在"正在运行"栏目中
//        mNotification.flags = Notification.FLAG_ONGOING_EVENT;
//
//        RemoteViews contentView = new RemoteViews(context.getPackageName(),
//                R.layout.download_notification_layout);
//        contentView.setTextViewText(R.id.fileName, "Action.apk");
//
//        // TODO 添加停止按钮 停止下载
////    		Intent stopIntent = new Intent();
////    		contentView.setOnClickFillInIntent(R.id.noti_stop_button, stopIntent);
//        // 指定个性化视图
//        mNotification.contentView = contentView;
//
////    		Intent intent = new Intent(context, FileMgrActivity2.class);
//        Intent intent = new Intent();
//        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent,
//                PendingIntent.FLAG_UPDATE_CURRENT);
//        // 指定内容意图
//        mNotification.contentIntent = contentIntent;
//        mNotificationManager.notify(NOTIFY_ID, mNotification);
    }

    /**
     * 下载模块
     */
    private void readRate() {
        cancelled = false;
        int rate = 0;
        while (!cancelled && rate < 100) {
            try {
                // 读取下载进度
                Thread.sleep(500);
                if (fileSize > 0) {
                    rate = (int) (downloadFileSize * 100 / fileSize);
//                    rate = 100;
//                    Log.i(TAG, "readRate:" + rate);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Message msg = progressHandler.obtainMessage();
            msg.what = 1;
            msg.arg1 = rate;
            progressHandler.sendMessage(msg);
//    			progress = rate;
        }
        if (cancelled) {
            Message msg = progressHandler.obtainMessage();
            msg.what = 0;
            progressHandler.sendMessage(msg);
        }
    }

    private void downloadFile(final String url) {
        cancelled = false;
        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(url);
        HttpResponse response;
        try {
            createUpdateFile();

            response = client.execute(get);
            HttpEntity entity = response.getEntity();
            fileSize = entity.getContentLength();
            Log.i(TAG, "downloadFile --fileSize--" + fileSize);
            InputStream is = entity.getContent();
            FileOutputStream fileOutputStream = null;

            if (is != null) {
                fileOutputStream = new FileOutputStream(updateFile);
                Log.i(TAG, "downloadFile --localFilePath--" + updateFile.getAbsolutePath());
                byte[] buf = new byte[1024];
                int ch = -1;
                while ((!cancelled) && ((ch = is.read(buf)) != -1)) {
                    fileOutputStream.write(buf, 0, ch);
                    downloadFileSize += ch;
                }
            }
            fileOutputStream.flush();
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            onInfo(ACTION_UPGRADE_NETWORK_DISCONNECTED);
        } catch (IOException e) {
            e.printStackTrace();
            onInfo(ACTION_UPGRADE_IOEXCEPTION);
        }
    }

    protected void createUpdateFile() {
        try {
            if (mkFilePath(pkgPath)) {
                updateFile = new File(pkgPath + "/newApp.apk");
            }
            Log.i(TAG, "file --" + updateFile.getAbsolutePath());
            Log.i(TAG, "file --" + updateFile.exists());
            if (!(updateFile.exists())) {
                updateFile.createNewFile();
                Log.i(TAG, "creat file --" + updateFile.exists());
            }
            // 设置文件可以被其他程序读写
            boolean readable = updateFile.setReadable(true, false);
            Log.i(TAG, "file readable --" + readable);
            boolean writable = updateFile.setWritable(true, false);
            Log.i(TAG, "file writable--" + writable);
        } catch (Exception e) {
            e.printStackTrace();
            onInfo(ACTION_UPGRADE_IOEXCEPTION);
        }

    }

    /**
     * <p>
     * Title:mkdir
     * </p>
     * <p>
     * Description: 创建path指定目录.
     * </p>
     * 
     * @param path
     */

    public static boolean mkFilePath(String path) {
        File fPath = new File(path);
        if (fPath.exists()) {
            return true;
        } else {
            if (fPath.mkdirs()) {
                Log.i("mkFilePath", "create directory [" + fPath.getName() + "] success!!!");
                return true;
            } else {
                Log.e("mkFilePath", "create directory [" + fPath.getName() + "] failed!!!");
                return false;
            }
        }
    }

    @Override
    public void onInfo(int info) {
        switch (info) {
            case ACTION_UPGRADE_DOWNLOAD_BEGIN:
                makeToast(mContext, R.drawable.upgrade_toast_media, R.string.downloading_new_apk)
                        .show();
                break;
            case ACTION_UPGRADE_DOWNLOAD_SUCCESS:
                makeToast(mContext, R.drawable.upgrade_toast_media, R.string.downloading_success)
                        .show();
                break;
            case ACTION_UPGRADE_NETWORK_DISCONNECTED:
                makeToast(mContext, R.drawable.upgrade_toast_warning,
                        R.string.downloading_terminated).show();
                break;
            case ACTION_UPGRADE_IOEXCEPTION:
                makeToast(mContext, R.drawable.upgrade_toast_warning,
                        R.string.downloading_terminated).show();
                break;

            default:
                break;
        }

    }

    /**
     * <p>
     * Title: To make a Toast used specified ImageResource.
     * </p>
     * <p>
     * Description: To make a Toast used specified ImageResource.
     * </p>
     * 
     * @param context
     * @param imageResId
     * @param textResId
     * @return the required toast;
     */
    @SuppressLint("ShowToast")
    public static Toast makeToast(Context context, int imageResId, int textResId) {
        // 创建一个Toast提示消息
        // Toast toast = new Toast(context);
        Toast toast = Toast.makeText(context, textResId, Toast.LENGTH_LONG);
        // 设置Toast提示消息在屏幕上的位置
        // toast.setGravity(Gravity.CENTER, 0, 0);
        // 获取Toast提示消息里原有的View
        View toastView = toast.getView();
        // 创建一个ImageView
        ImageView img = new ImageView(context);
        img.setImageResource(imageResId);
        // 创建一个LineLayout容器
        LinearLayout ll = new LinearLayout(context);
        ll.setGravity(Gravity.CENTER);
        // 向LinearLayout中添加ImageView和Toast原有的View
        ll.addView(img);
        ll.addView(toastView);
        // 将LineLayout容器设置为toast的View
        toast.setView(ll);

        return toast;
    }


}

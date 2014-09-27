package cn.bobby.appupgrade.test;

import android.app.Activity;
import android.os.Bundle;

import cn.bobby.appupgrade.AppUpgradeApplication;
import cn.bobby.appupgrade.R;
import cn.bobby.appupgrade.impl.OnNewVersionListener;

/**
 * <p>Title: TODO.</p>
 * <p>Description: TODO.</p>
 *
 * @author Bobby Zou(zeroapp@126.com) 2014-3-5.
 * @version $Id$
 */

public class AppUpgradeTestActivity extends Activity {

    public static final String TAG = "AppUpgrade";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        ((AppUpgradeApplication) getApplication()).setOnNewVersionListener(
                new OnNewVersionListener(this), "", "");


    }
    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }

}

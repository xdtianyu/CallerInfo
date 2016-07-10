package org.xdty.callerinfo;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.Until;

import org.junit.Before;
import org.junit.Rule;
import org.xdty.callerinfo.activity.MainActivity;
import org.xdty.callerinfo.model.database.Database;
import org.xdty.callerinfo.model.database.DatabaseImpl;
import org.xdty.callerinfo.model.db.InCall;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.model.setting.SettingImpl;

import java.util.ArrayList;
import java.util.List;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class ActivityTestBase {

    protected static final String BASIC_PACKAGE = "org.xdty.callerinfo";

    protected static final int LAUNCH_TIMEOUT = 5000;

    @Rule
    public IntentsTestRule<MainActivity> mActivityRule = new IntentsTestRule<>(
            MainActivity.class);
    protected UiDevice mDevice;

    protected Setting mSetting;
    protected Database mDatabase;
    protected List<InCall> mInCalls;

    public ActivityTestBase() {
        init();
    }

    private void init() {
        SettingImpl.init(getTargetContext());
        mSetting = SettingImpl.getInstance();
        mDatabase = DatabaseImpl.getInstance();

        mSetting.clear();
        mSetting.setEula();

        mDatabase.clearAllInCallSync();
        mDatabase.clearAllCallerSync();
        mDatabase.clearAllMarkedRecordSync();

        long time = System.currentTimeMillis();

        mInCalls = new ArrayList<>();
        mInCalls.add(new InCall("10086", time - 10000, 10332, 17223));
        mInCalls.add(new InCall("4001016172", time - 86400000, 2112, 0));
        mInCalls.add(new InCall("075583763333", time - 86400000 * 2, 3234, 15112));
        mInCalls.add(new InCall("0291235555", time - 86400000 * 3, 32340, 151120));

        mDatabase.addInCallersSync(mInCalls);
    }

    @Before
    public void startMainActivityFromHomeScreen() {
        // Initialize UiDevice instance
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Start from the home screen
        mDevice.pressHome();

        // Wait for launcher
        final String launcherPackage = getLauncherPackageName();
        assertThat(launcherPackage, notNullValue());
        mDevice.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), LAUNCH_TIMEOUT);

        // Launch the blueprint app
        Context context = InstrumentationRegistry.getContext();
        final Intent intent = context.getPackageManager().getLaunchIntentForPackage(BASIC_PACKAGE);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);    // Clear out any previous instances
        context.startActivity(intent);

        // Wait for the app to appear
        mDevice.wait(Until.hasObject(By.pkg(BASIC_PACKAGE).depth(0)), LAUNCH_TIMEOUT);

        beforeTest();
    }

    public void beforeTest() {

    }

    /**
     * Uses package manager to find the package name of the device launcher. Usually this package
     * is "com.android.launcher" but can be different at times. This is a generic solution which
     * works on all platforms.
     */
    private String getLauncherPackageName() {
        // Create launcher Intent
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);

        // Use PackageManager to get the launcher package name
        PackageManager pm = InstrumentationRegistry.getContext().getPackageManager();
        ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo.activityInfo.packageName;
    }

}

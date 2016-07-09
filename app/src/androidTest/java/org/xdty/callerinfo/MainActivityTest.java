package org.xdty.callerinfo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.ViewAssertion;
import android.support.test.espresso.action.GeneralLocation;
import android.support.test.espresso.action.GeneralSwipeAction;
import android.support.test.espresso.action.Press;
import android.support.test.espresso.action.Swipe;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.Until;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xdty.callerinfo.activity.MainActivity;
import org.xdty.callerinfo.activity.SettingsActivity;
import org.xdty.callerinfo.model.database.Database;
import org.xdty.callerinfo.model.database.DatabaseImpl;
import org.xdty.callerinfo.model.db.InCall;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.model.setting.SettingImpl;
import org.xdty.callerinfo.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressKey;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.action.ViewActions.swipeRight;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withHint;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
public class MainActivityTest {

    private static final String BASIC_PACKAGE = "org.xdty.callerinfo";

    private static final int LAUNCH_TIMEOUT = 5000;

    private static final String STRING_TO_BE_TYPED = "UiAutomator";
    @Rule
    public IntentsTestRule<MainActivity> mActivityRule = new IntentsTestRule<>(
            MainActivity.class);
    private UiDevice mDevice;

    private Setting mSetting;
    private Database mDatabase;
    private List<InCall> mInCalls;

    public MainActivityTest() {
        init();
    }

    public static ViewAction clickChildViewWithId(final int id) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return null;
            }

            @Override
            public String getDescription() {
                return "Click on a child view with specified id.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                View v = view.findViewById(id);
                if (v != null) {
                    v.performClick();
                }
            }
        };
    }

    public static ViewAction swipeUp() {
        return new GeneralSwipeAction(Swipe.FAST, GeneralLocation.BOTTOM_CENTER,
                GeneralLocation.TOP_CENTER, Press.FINGER);
    }

    public static ViewAction swipeDown() {
        return new GeneralSwipeAction(Swipe.FAST, GeneralLocation.TOP_CENTER,
                GeneralLocation.BOTTOM_CENTER, Press.FINGER);
    }

    public static Matcher<View> isWindowAtPosition(final int x, final int y) {
        return new TypeSafeMatcher<View>() {

            int rootX = -1;
            int rootY = -1;

            @Override
            protected boolean matchesSafely(View view) {
                if (view.getRootView().getLayoutParams() instanceof WindowManager.LayoutParams) {
                    WindowManager.LayoutParams params = (WindowManager.LayoutParams) view.getRootView()
                            .getLayoutParams();
                    rootX = params.x;
                    rootY = params.y;
                    return rootX == x && rootY == y;
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("view is at position: (" + rootX + " ," + rootY
                        + "), but should at x=" + x + ", y=" + y + "");
            }
        };
    }

    public static Matcher<View> atPosition(final int position,
            @NonNull final Matcher<View> itemMatcher) {
        return new BoundedMatcher<View, RecyclerView>(RecyclerView.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("has item at position " + position + ": ");
                itemMatcher.describeTo(description);
            }

            @Override
            protected boolean matchesSafely(final RecyclerView view) {
                RecyclerView.ViewHolder viewHolder = view.findViewHolderForAdapterPosition(
                        position);
                return viewHolder != null && itemMatcher.matches(viewHolder.itemView);
            }
        };
    }

    public static ViewAssertion itemsCountIs(final int count) {
        return new ViewAssertion() {
            @Override
            public void check(View view, NoMatchingViewException e) {
                if (!(view instanceof RecyclerView)) {
                    throw e;
                }
                RecyclerView rv = (RecyclerView) view;
                assertThat(rv.getAdapter().getItemCount(), is(count));
            }
        };
    }

    private void init() {
        SettingImpl.init(getTargetContext());
        mSetting = SettingImpl.getInstance();
        mDatabase = DatabaseImpl.getInstance();

        mDatabase.clearAllInCallSync();
        mDatabase.clearAllCallerSync();
        mDatabase.clearAllMarkedRecordSync();

        long time = System.currentTimeMillis();

        mInCalls = new ArrayList<>();
        mInCalls.add(new InCall("10086", time - 10000, 10332, 17223));
        mInCalls.add(new InCall("4001016172", time - 86400000, 2112, 0));
        mInCalls.add(new InCall("075583763333", time - 86400000 * 2, 3234, 15112));

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
    }

    @Test
    public void testEmptyList() {
        UiObject2 list = mDevice.wait(Until.findObject(By.res(BASIC_PACKAGE, "history_list")), 500);
        UiObject2 empty = mDevice.findObject(By.res(BASIC_PACKAGE, "empty_text"));

        if (list == null) {
            assertThat(empty, notNullValue());
        }

        if (empty == null) {
            assertThat(list, notNullValue());
        }
    }

    @Test
    public void testActionSetting() {
        openActionBarOverflowOrOptionsMenu(getTargetContext());
        onView(withText(R.string.action_settings))
                .perform(click());
        intended(hasComponent(new ComponentName(getTargetContext(), SettingsActivity.class)));
        //pressBack();
    }

    @Test
    public void testRecyclerViewItemClick() {

        String text = Utils.readableTime(getTargetContext(), mInCalls.get(0).getDuration());

        onView(withId(R.id.history_list)).perform(RecyclerViewActions.actionOnItemAtPosition(0,
                clickChildViewWithId(R.id.card_view)));
        onView(allOf(withId(R.id.time),
                hasSibling(withText(text)))).check(matches(isDisplayed()));
        onView(withId(R.id.history_list)).perform(RecyclerViewActions.actionOnItemAtPosition(0,
                clickChildViewWithId(R.id.card_view)));
        onView(allOf(withId(R.id.time),
                hasSibling(withText(text)))).check(matches(not(isDisplayed())));
    }

    @Test
    public void testRecyclerViewItemSwap() {

    }

    @Test
    public void testActionClear() {

        onView(withId(R.id.history_list)).check(matches(isDisplayed()));
        onView(withId(R.id.empty_text)).check(matches(not(isDisplayed())));

        openActionBarOverflowOrOptionsMenu(getTargetContext());
        onView(withText(R.string.action_clear_history))
                .perform(click());

        // check confirm dialog and click cancel
        onView(withText(R.string.clear_history_message))
                .inRoot(isDialog())
                .check(matches(isDisplayed()));
        onView(withText(R.string.cancel))
                .inRoot(isDialog())
                .perform(click());

        // check list exist
        onView(withId(R.id.history_list)).check(matches(isDisplayed()));
        onView(withId(R.id.empty_text)).check(matches(not(isDisplayed())));

        openActionBarOverflowOrOptionsMenu(getTargetContext());
        onView(withText(R.string.action_clear_history))
                .perform(click());

        // check confirm dialog and click ok
        onView(withText(R.string.clear_history_message))
                .inRoot(isDialog())
                .check(matches(isDisplayed()));
        onView(withText(R.string.ok))
                .inRoot(isDialog())
                .perform(click());

        // check list not exist
        onView(withId(R.id.history_list)).check(matches(isDisplayed()));
        onView(withId(R.id.empty_text)).check(matches(isDisplayed()));

        // reinsert data for other tests
        mDatabase.addInCallers(mInCalls);
    }

    @Test
    public void testActionSearch() {
        onView(withId(R.id.action_search)).perform(click());
        onView(withId(R.id.search_src_text)).check(matches(isDisplayed()));
        onView(withId(R.id.search_src_text)).check(matches(withHint(R.string.search_hint)));
        onView(isAssignableFrom(ImageButton.class)).perform(click());
        onView(withId(R.id.search_src_text)).check(doesNotExist());
        onView(withId(R.id.action_search)).perform(click());
        onView(isAssignableFrom(EditText.class)).perform(typeText("10086"),
                pressKey(KeyEvent.KEYCODE_ENTER));

        onView(withId(R.id.window_layout)).inRoot(
                withDecorView(not(is(mActivityRule.getActivity().getWindow().getDecorView()))))
                .check(matches(isDisplayed()));
        onView(withId(R.id.number_info)).inRoot(
                withDecorView(not(is(mActivityRule.getActivity().getWindow().getDecorView()))))
                .check(matches(withText("中国移动客服")));

        onView(withId(R.id.history_list)).check(matches(not(isDisplayed())));

        onView(isAssignableFrom(ImageButton.class)).perform(click());
        onView(withId(R.id.window_layout)).inRoot(
                withDecorView(not(is(mActivityRule.getActivity().getWindow().getDecorView()))))
                .check(doesNotExist());
        onView(withId(R.id.history_list)).check(matches(isDisplayed()));
    }

    @Test
    public void testActionMoveWindowPosition() {

        // click move window menu and check window visibility
        openActionBarOverflowOrOptionsMenu(getTargetContext());
        onView(withText(R.string.action_float_window))
                .perform(click());
        onView(withId(R.id.window_layout)).inRoot(
                withDecorView(not(is(mActivityRule.getActivity().getWindow().getDecorView()))))
                .check(matches(isDisplayed()));
        onView(withId(R.id.number_info)).inRoot(
                withDecorView(not(is(mActivityRule.getActivity().getWindow().getDecorView()))))
                .check(matches(withText(R.string.float_window_hint)));

        // swipe up window and check position
        onView(withId(R.id.number_info)).inRoot(
                withDecorView(not(is(mActivityRule.getActivity().getWindow().getDecorView()))))
                .perform(swipeUp());
        onView(withId(R.id.window_layout)).inRoot(
                withDecorView(not(is(mActivityRule.getActivity().getWindow().getDecorView()))))
                .check(matches(isWindowAtPosition(mSetting.getWindowX(), mSetting.getWindowY())));

        // swipe down window and check position
        onView(withId(R.id.number_info)).inRoot(
                withDecorView(not(is(mActivityRule.getActivity().getWindow().getDecorView()))))
                .perform(swipeDown());
        onView(withId(R.id.window_layout)).inRoot(
                withDecorView(not(is(mActivityRule.getActivity().getWindow().getDecorView()))))
                .check(matches(isWindowAtPosition(mSetting.getWindowX(), mSetting.getWindowY())));

        // click close window menu and check window visibility
        openActionBarOverflowOrOptionsMenu(getTargetContext());
        onView(withText(R.string.close_window))
                .perform(click());
        onView(withId(R.id.window_layout)).inRoot(
                withDecorView(not(is(mActivityRule.getActivity().getWindow().getDecorView()))))
                .check(doesNotExist());
    }

    @Test
    public void testNotificationClick() throws UiObjectNotFoundException {
        // click move window menu and check notification
        openActionBarOverflowOrOptionsMenu(getTargetContext());
        onView(withText(R.string.action_float_window))
                .perform(click());
        onView(withId(R.id.window_layout)).inRoot(
                withDecorView(not(is(mActivityRule.getActivity().getWindow().getDecorView()))))
                .check(matches(isDisplayed()));

        mDevice.openNotification();
        mDevice.wait(Until.hasObject(By.pkg("com.android.systemui")), 10000);
        UiSelector notificationStackScroller = new UiSelector().packageName("com.android.systemui")
                .className("android.view.ViewGroup")
                .resourceId("com.android.systemui:id/notification_stack_scroller");
        UiObject notificationStackScrollerUiObject = mDevice.findObject(notificationStackScroller);
        assertTrue(notificationStackScrollerUiObject.exists());

        String text = getTargetContext().getString(R.string.app_name);
        UiObject notify = notificationStackScrollerUiObject.getChild(new UiSelector().text(text));
        assertTrue(notify.exists());
        notify.click();
        onView(withId(R.id.window_layout)).inRoot(
                withDecorView(not(is(mActivityRule.getActivity().getWindow().getDecorView()))))
                .check(doesNotExist());
    }

    @Test
    public void testRecyclerViewItemSwipe() {

        // swipe and undo
        onView(withId(R.id.history_list))
                .perform(RecyclerViewActions.actionOnItemAtPosition(1, swipeLeft()));
        onView(withId(R.id.history_list))
                .check(itemsCountIs(2));
        onView(allOf(withText(R.string.undo), hasSibling(withText(R.string.deleted))))
                .perform(click());
        onView(withId(R.id.history_list))
                .check(itemsCountIs(3));

        // swipe and delete
        onView(withId(R.id.history_list))
                .perform(RecyclerViewActions.actionOnItemAtPosition(1, swipeLeft()));
        onView(withId(R.id.history_list))
                .check(itemsCountIs(2));
        onView(allOf(withId(android.support.design.R.id.snackbar_text), withText(R.string.deleted)))
                .perform(swipeRight());
        onView(withId(R.id.history_list))
                .check(itemsCountIs(2));

        onView(withId(R.id.history_list)).check(
                matches(atPosition(0, hasDescendant(withText(mInCalls.get(0).getNumber())))));
        onView(withId(R.id.history_list)).check(
                matches(atPosition(1, hasDescendant(withText(mInCalls.get(2).getNumber())))));
    }

    @Test
    public void testSwipeRefresh() {
        long time = System.currentTimeMillis();
        InCall inCall = new InCall("10000", time, 3553, 35052);
        inCall.save();

        onView(withId(R.id.swipe_refresh_layout)).perform(swipeDown());
        onView(withId(R.id.history_list)).check(
                matches(atPosition(0, hasDescendant(withText("中国电信客服")))));

        inCall.delete();

        onView(withId(R.id.swipe_refresh_layout)).perform(swipeDown());
        onView(withId(R.id.history_list)).check(
                matches(atPosition(0, hasDescendant(withText("中国移动客服")))));
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

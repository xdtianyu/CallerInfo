package org.xdty.callerinfo;

import android.os.SystemClock;
import android.support.test.filters.LargeTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.xdty.callerinfo.service.FloatWindow;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.xdty.callerinfo.TestUtils.atPosition;
import static org.xdty.callerinfo.TestUtils.checkRadioItem;
import static org.xdty.callerinfo.TestUtils.childWithBackgroundColor;
import static org.xdty.callerinfo.TestUtils.setProgress;
import static org.xdty.callerinfo.TestUtils.withHeight;
import static org.xdty.callerinfo.TestUtils.withTextAlign;
import static org.xdty.callerinfo.TestUtils.withTextPadding;
import static org.xdty.callerinfo.TestUtils.withTextSize;
import static org.xdty.callerinfo.TestUtils.withTransparency;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
@LargeTest
public class SettingsActivityTest extends ActivityTestBase {

    @Override
    public void beforeTest() {
        navigateToSetting();
    }

    public void navigateToSetting() {
        openActionBarOverflowOrOptionsMenu(getTargetContext());
        onView(withText(R.string.action_settings)).perform(click());
        SystemClock.sleep(1000);
    }

    @Test
    public void testColorSettings() {

        // normal color
        onView(withText(R.string.color_normal)).perform(click());
        onView(withContentDescription("Color 6"))
                .inRoot(isDialog())
                .perform(click());
        pressBack();
        onView(withId(R.id.history_list)).check(
                matches(atPosition(3,
                        childWithBackgroundColor(R.id.card_view, mSetting.getNormalColor()))));

        // poi color
        navigateToSetting();
        onView(withText(R.string.color_poi)).perform(click());
        onView(withContentDescription("Color 8"))
                .inRoot(isDialog())
                .perform(click());
        pressBack();
        onView(withId(R.id.history_list)).check(
                matches(atPosition(0,
                        childWithBackgroundColor(R.id.card_view, mSetting.getPoiColor()))));

        // report color
        navigateToSetting();
        onView(withText(R.string.color_report)).perform(click());
        onView(withContentDescription("Color 3"))
                .inRoot(isDialog())
                .perform(click());
        pressBack();
        onView(withId(R.id.history_list)).check(
                matches(atPosition(1,
                        childWithBackgroundColor(R.id.card_view, mSetting.getReportColor()))));
        onView(withId(R.id.history_list)).check(
                matches(atPosition(2,
                        childWithBackgroundColor(R.id.card_view, mSetting.getReportColor()))));
    }

    @Test
    public void testWindowTextSizeSetting() {
        onView(withText(R.string.window_text_style)).perform(click());

        // open text size setting dialog
        onView(withText(R.string.window_text_size)).perform(click());

        onView(withId(R.id.seek_bar))
                .inRoot(isDialog())
                .perform(setProgress(10));

        onView(withId(R.id.window_layout)).inRoot(not(isDialog()))
                .check(matches(isDisplayed()));
        onView(withId(R.id.number_info)).inRoot(not(isDialog()))
                .check(matches(withText(R.string.text_size)));

        // check preview window text size
        onView(withId(R.id.number_info)).inRoot(not(isDialog()))
                .check(matches(withTextSize(10)));

        // set another value
        onView(withId(R.id.seek_bar))
                .inRoot(isDialog())
                .perform(setProgress(40));

        onView(withId(R.id.number_info)).inRoot(not(isDialog()))
                .check(matches(withTextSize(40)));

        // cancel size setting
        onView(withText(R.string.cancel))
                .inRoot(isDialog())
                .perform(click());

        // check text size setting not performed
        onView(withText(R.string.window_text_size)).perform(click());

        onView(withId(R.id.number_info)).inRoot(not(isDialog()))
                .check(matches(not(withTextSize(10))));
        onView(withId(R.id.number_info)).inRoot(not(isDialog()))
                .check(matches(not(withTextSize(40))));

        // set text size value and click ok
        onView(withId(R.id.seek_bar))
                .inRoot(isDialog())
                .perform(setProgress(30));
        onView(withId(R.id.number_info)).inRoot(not(isDialog()))
                .check(matches(withTextSize(30)));

        onView(withText(R.string.ok))
                .inRoot(isDialog())
                .perform(click());

        // check text size setting
        pressBack();
        pressBack();

        openActionBarOverflowOrOptionsMenu(getTargetContext());
        onView(withText(R.string.action_float_window))
                .perform(click());

        onView(withId(R.id.number_info)).inRoot(
                withDecorView(not(is(mActivityRule.getActivity().getWindow().getDecorView()))))
                .check(matches(withTextSize(mSetting.getTextSize())));

    }

    @Test
    public void testWindowTextAlignmentSetting() {
        onView(withText(R.string.window_text_style)).perform(click());

        testWindowTextAlignmentSetting(0, FloatWindow.TEXT_ALIGN_LEFT);
        testWindowTextAlignmentSetting(1, FloatWindow.TEXT_ALIGN_CENTER);
        testWindowTextAlignmentSetting(2, FloatWindow.TEXT_ALIGN_RIGHT);

    }

    private void testWindowTextAlignmentSetting(int itemId, int alignment) {
        // open text alignment setting dialog
        onView(withText(R.string.window_text_alignment)).perform(click());

        onView(withId(R.id.radio))
                .inRoot(isDialog())
                .perform(checkRadioItem(itemId));
        // check text style alignment
        onView(withText(R.string.window_text_size)).perform(click());
        onView(withId(R.id.number_info)).inRoot(not(isDialog()))
                .check(matches(withTextAlign(alignment)));
        pressBack();
    }

    @Test
    public void testWindowTextPaddingSetting() {
        onView(withText(R.string.window_text_style)).perform(click());

        int align = mSetting.getTextAlignment();

        // open text padding setting dialog
        onView(withText(R.string.window_text_padding)).perform(click());

        onView(withId(R.id.seek_bar))
                .inRoot(isDialog())
                .perform(setProgress(10));

        onView(withId(R.id.window_layout)).inRoot(not(isDialog()))
                .check(matches(isDisplayed()));
        onView(withId(R.id.number_info)).inRoot(not(isDialog()))
                .check(matches(withText(R.string.text_padding)));

        // check preview window text padding
        onView(withId(R.id.number_info)).inRoot(not(isDialog()))
                .check(matches(withTextPadding(align, 10)));

        onView(withId(R.id.seek_bar))
                .inRoot(isDialog())
                .perform(setProgress(20));
        onView(withId(R.id.number_info)).inRoot(not(isDialog()))
                .check(matches(withTextPadding(align, 20)));

        onView(withId(R.id.seek_bar))
                .inRoot(isDialog())
                .perform(setProgress(40));
        onView(withId(R.id.number_info)).inRoot(not(isDialog()))
                .check(matches(withTextPadding(align, 40)));

        // cancel padding setting
        onView(withText(R.string.cancel))
                .inRoot(isDialog())
                .perform(click());

        // check text padding setting not performed
        onView(withText(R.string.window_text_padding)).perform(click());

        onView(withId(R.id.number_info)).inRoot(not(isDialog()))
                .check(matches(not(withTextPadding(align, 10))));
        onView(withId(R.id.number_info)).inRoot(not(isDialog()))
                .check(matches(not(withTextPadding(align, 20))));
        onView(withId(R.id.number_info)).inRoot(not(isDialog()))
                .check(matches(not(withTextPadding(align, 40))));

        // set text padding value and click ok
        onView(withId(R.id.seek_bar))
                .inRoot(isDialog())
                .perform(setProgress(30));
        onView(withId(R.id.number_info)).inRoot(not(isDialog()))
                .check(matches(withTextPadding(align, 30)));

        onView(withText(R.string.ok))
                .inRoot(isDialog())
                .perform(click());

        // check text padding in text size setting
        onView(withText(R.string.window_text_size)).perform(click());
        onView(withId(R.id.number_info)).inRoot(not(isDialog()))
                .check(matches(withTextPadding(align, 30)));
    }

    @Test
    public void testWindowTextColorSetting() {
        onView(withText(R.string.window_text_style)).perform(click());

        onView(withText(R.string.window_text_color)).perform(click());

        // TODO: test incoming window text color
    }

    @Test
    public void testWindowHeightSetting() {
        onView(withText(R.string.window_height)).perform(click());

        SystemClock.sleep(1000);

        onView(withId(R.id.window_layout)).inRoot(not(isDialog()))
                .check(matches(isDisplayed()));
        onView(withId(R.id.number_info)).inRoot(not(isDialog()))
                .check(matches(withText(R.string.window_height_message)));

        onView(withId(R.id.seek_bar))
                .inRoot(isDialog())
                .perform(setProgress(60));
        onView(withId(R.id.content)).inRoot(not(isDialog()))
                .check(matches(withHeight(60)));

        onView(withId(R.id.seek_bar))
                .inRoot(isDialog())
                .perform(setProgress(90));
        onView(withId(R.id.content)).inRoot(not(isDialog()))
                .check(matches(withHeight(90)));

        onView(withId(R.id.seek_bar))
                .inRoot(isDialog())
                .perform(setProgress(120));
        onView(withId(R.id.content)).inRoot(not(isDialog()))
                .check(matches(withHeight(120)));

        // cancel height setting
        onView(withText(R.string.cancel))
                .inRoot(isDialog())
                .perform(click());

        // check text height setting not performed
        onView(withText(R.string.window_transparent)).perform(click());
        onView(withId(R.id.content)).inRoot(not(isDialog()))
                .check(matches(not(withHeight(60))));
        onView(withId(R.id.content)).inRoot(not(isDialog()))
                .check(matches(not(withHeight(90))));
        onView(withId(R.id.content)).inRoot(not(isDialog()))
                .check(matches(not(withHeight(120))));

        pressBack();

        onView(withText(R.string.window_height)).perform(click());

        // set height setting and click ok
        onView(withId(R.id.seek_bar))
                .inRoot(isDialog())
                .perform(setProgress(110));
        onView(withId(R.id.content)).inRoot(not(isDialog()))
                .check(matches(withHeight(110)));
        onView(withText(R.string.ok))
                .inRoot(isDialog())
                .perform(click());

        onView(withText(R.string.window_transparent)).perform(click());
        onView(withId(R.id.content)).inRoot(not(isDialog()))
                .check(matches(withHeight(110)));
    }

    @Test
    public void testWindowTransparentSetting() {
        onView(withText(R.string.window_transparent)).perform(click());

        onView(withId(R.id.window_layout)).inRoot(not(isDialog()))
                .check(matches(isDisplayed()));
        onView(withId(R.id.number_info)).inRoot(not(isDialog()))
                .check(matches(withText(R.string.text_transparent)));

        onView(withId(R.id.seek_bar))
                .inRoot(isDialog())
                .perform(setProgress(60));
        onView(withId(R.id.content)).inRoot(not(isDialog()))
                .check(matches(withTransparency(60)));

        onView(withId(R.id.seek_bar))
                .inRoot(isDialog())
                .perform(setProgress(20));
        onView(withId(R.id.content)).inRoot(not(isDialog()))
                .check(matches(withTransparency(20)));

        onView(withId(R.id.seek_bar))
                .inRoot(isDialog())
                .perform(setProgress(90));
        onView(withId(R.id.content)).inRoot(not(isDialog()))
                .check(matches(withTransparency(90)));

        // cancel the change
        onView(withText(R.string.cancel))
                .inRoot(isDialog())
                .perform(click());

        // check window transparency
        onView(withText(R.string.window_height))
                .perform(click());
        onView(withId(R.id.content)).inRoot(not(isDialog()))
                .check(matches(not(withTransparency(60))));
        onView(withId(R.id.content)).inRoot(not(isDialog()))
                .check(matches(not(withTransparency(20))));
        onView(withId(R.id.content)).inRoot(not(isDialog()))
                .check(matches(not(withTransparency(90))));

        pressBack();

        // save the change
        onView(withText(R.string.window_transparent)).perform(click());
        onView(withId(R.id.seek_bar))
                .inRoot(isDialog())
                .perform(setProgress(50));
        onView(withId(R.id.content)).inRoot(not(isDialog()))
                .check(matches(withTransparency(50)));

        onView(withText(R.string.ok))
                .inRoot(isDialog())
                .perform(click());

        // check the transparency is performed
        onView(withText(R.string.window_height))
                .perform(click());
        onView(withId(R.id.content)).inRoot(not(isDialog()))
                .check(matches(withTransparency(50)));
    }

}

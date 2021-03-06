package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import com.firebirdberlin.nightdream.CustomAnalogClock;
import com.firebirdberlin.nightdream.CustomDigitalClock;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.AnalogClockConfig;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

public class ClockLayout extends LinearLayout {
    public static final int LAYOUT_ID_DIGITAL = 0;
    public static final int LAYOUT_ID_ANALOG = 1;
    public static final int LAYOUT_ID_ANALOG2 = 2;
    public static final int LAYOUT_ID_ANALOG3 = 3;
    public static final int LAYOUT_ID_ANALOG4 = 4;
    private static final String TAG = "NightDream.ClockLayout";
    private int layoutId = LAYOUT_ID_DIGITAL;

    private Context context;
    private AutoAdjustTextView clock = null;
    private AutoAdjustTextView clock_ampm = null;
    private CustomAnalogClock analog_clock = null;
    private AutoAdjustTextView date = null;
    private WeatherLayout weatherLayout = null;
    private View divider = null;
    private boolean showDivider = true;
    private boolean mirrorText = false;

    public ClockLayout(Context context) {
        super(context);
        this.context = context;
    }

    public ClockLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    private void init() {
        if( getChildCount() > 0) {
            removeAllViews();
        }

        LayoutInflater inflater = (LayoutInflater)
            context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View child;
        if (layoutId == LAYOUT_ID_DIGITAL) {
            child = inflater.inflate(R.layout.clock_layout, null);
        } else
        if (layoutId == LAYOUT_ID_ANALOG ){
            child = inflater.inflate(R.layout.analog_clock_layout, null);
        } else {
            child = inflater.inflate(R.layout.analog_clock_layout_4, null);
        }
        if (child != null) {
            addView(child);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        return true;
    }

    public void setLayout(int layoutId) {
        this.layoutId = layoutId;
        init();
        onFinishInflate();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Log.v(TAG, "onFinishInflate");
        clock = findViewById(R.id.clock);
        clock_ampm = findViewById(R.id.clock_ampm);
        date = findViewById(R.id.date);
        weatherLayout = findViewById(R.id.weatherLayout);
        divider = findViewById(R.id.divider);
        analog_clock = findViewById(R.id.analog_clock);
    }

    public void setTypeface(Typeface typeface) {
        if (clock != null) {
            clock.setTypeface(typeface);
        }
        if ( clock_ampm != null ) {
            clock_ampm.setTypeface(typeface);
        }
    }

    public void setPrimaryColor(int color) {
        if (clock != null) {
            clock.setTextColor(color);
        }
        if ( clock_ampm != null ) {
            clock_ampm.setTextColor(color);
        }
        if (analog_clock != null) {
            analog_clock.setPrimaryColor(color);
        }
    }

    public void setSecondaryColor(int color) {
        if (date != null) {
            date.setTextColor(color);
        }
        if (weatherLayout != null ) {
            weatherLayout.setColor(color);
        }
        if (divider != null) {
            divider.setBackgroundColor(color);
        }
        if (analog_clock != null) {
            analog_clock.setSecondaryColor(color);
        }
    }

    public void setTemperature(boolean on, int unit) {
        if (weatherLayout == null) return;
        weatherLayout.setTemperature(on, unit);
    }

    public void setWindSpeed(boolean on, int unit) {
        weatherLayout.setWindSpeed(on, unit);
    }

    public void setShowDivider(boolean on) {
        this.showDivider = on;
    }

    public void setMirrorText(boolean on) {
        this.mirrorText = on;
    }

    public void showDate(boolean on) {
        date.setVisibility( (on) ? View.VISIBLE : View.GONE);
        toggleDivider();
    }

    public void showWeather(boolean on) {
        weatherLayout.setVisibility( (on) ? View.VISIBLE : View.GONE);
        toggleDivider();
    }

    private void toggleDivider() {
        if (divider == null) return;

        if (!showDivider) {
            divider.setVisibility(GONE);
        }

        if (date.getVisibility() != VISIBLE
                && weatherLayout.getVisibility() != VISIBLE) {

            if (showDivider) {
                divider.setVisibility(INVISIBLE);
            }
            setBackgroundColor(Color.parseColor("#00000000"));
        } else {
            if (showDivider) {
                divider.setVisibility(VISIBLE);
            }
            setBackgroundColor(Color.parseColor("#44000000"));
        }
    }

    public boolean isDigital() {
        return layoutId == LAYOUT_ID_DIGITAL;
    }

    public void updateLayout(int parentWidth, Configuration config){
        updateLayout(parentWidth, -1, config, false);
    }

    public void updateLayoutForWidget(int parentWidth, int parentHeight, Configuration config){
        updateLayout(parentWidth, parentHeight, config, true);
    }

    private void updateLayout(int parentWidth, int parentHeight, Configuration config, boolean displayInWidget){
        final float minFontSize = 8.f; // in sp

        if (layoutId == LAYOUT_ID_DIGITAL) {
            setSize(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

            if (clock != null && !displayInWidget) {
                clock.setSampleText("22:55");
            }

            if (displayInWidget) {
                setPadding(15, 15, 15, 15);
                //ignore orientation, 100% width, so it fills whole space of the widget area
                if (clock != null) {
                    clock.setPadding(0, 0, 0, 0);
                    clock.setMaxWidth((int) (0.8 * parentWidth));
                    clock.setMaxHeight((int) (0.35 * parentHeight));
                    clock.setMaxFontSizesInSp(6.f, 300.f);
                    clock.invalidate(); // must invalidate to get correct getHeightOfView below
                }
                if (date != null  && date.getVisibility() == VISIBLE) {
                    date.setMaxWidth((int) (0.9 * parentWidth));
                    date.setMaxHeight(parentHeight / 5);
                    date.setMaxFontSizesInSp(6.f, 20.f);
                    date.invalidate(); // must invalidate to get correct getHeightOfView below
                }
                if (weatherLayout != null && weatherLayout.getVisibility() == VISIBLE) {
                    weatherLayout.setMaxWidth((int) (0.9 * parentWidth));
                    weatherLayout.setMaxFontSizesInPx(
                            Utility.spToPx(context, 6.f),
                            Utility.spToPx(context, 20.f));
                    weatherLayout.update();
                    weatherLayout.invalidate(); // must invalidate to get correct getHeightOfView below
                }

                int measuredHeight = Utility.getHeightOfView(this);
                Log.i(TAG, "### measuredHeight=" + measuredHeight + ", parentHeight=" + parentHeight);

                if (measuredHeight > parentHeight) {
                    Log.i(TAG, "### measuredHeight > parentHeight");
                    // shrink clock width so that its height fits the widget height
                    if (clock != null) {
                        clock.setMaxHeight(parentHeight / 4);
                    }
                    if (date != null) {
                        date.setMaxHeight(parentHeight / 6);
                    }
                    if (weatherLayout != null && weatherLayout.getVisibility() == VISIBLE) {
                        weatherLayout.setMaxWidth((int) (0.7 * parentWidth));
                        weatherLayout.update();
                    }
                }

            } else {

                switch (config.orientation) {
                    case Configuration.ORIENTATION_LANDSCAPE:
                        if (clock != null) {
                            clock.setMaxWidth((int) (0.3f * parentWidth));
                            clock.setMaxFontSizesInSp(minFontSize, (300.f));
                        }
                        if (date != null) {
                            date.setMaxWidth(parentWidth / 2);
                            date.setMaxFontSizesInSp(minFontSize, (20.f));
                        }
                        if (weatherLayout != null) {
                            weatherLayout.setMaxWidth(parentWidth / 2);
                            weatherLayout.setMaxFontSizesInPx(
                                    Utility.spToPx(context, minFontSize),
                                    Utility.spToPx(context, 20.f)
                            );
                            weatherLayout.update();
                        }
                        break;
                    case Configuration.ORIENTATION_PORTRAIT:
                    default:
                        if (clock != null) {
                            clock.setMaxWidth((int) (0.6f * parentWidth));
                            clock.setMaxFontSizesInSp(minFontSize, (300.f));
                        }
                        if (date != null) {
                            date.setMaxWidth((int) (0.8f * parentWidth));
                            date.setMaxFontSizesInSp(minFontSize, (25.f));
                        }
                        if (weatherLayout != null) {
                            weatherLayout.setMaxWidth((int) (0.8f * parentWidth));
                            weatherLayout.setMaxFontSizesInPx(Utility.spToPx(context, minFontSize),
                                    Utility.spToPx(context, 25.f));
                            weatherLayout.update();
                        }
                        break;
                }
            }
        } else if (layoutId == LAYOUT_ID_ANALOG) {
            setupLayoutAnalog(parentWidth, parentHeight, config, displayInWidget);
        } else {
            setupLayoutAnalog2(parentWidth, parentHeight, config, displayInWidget);
        }

        if ( date != null ) {
            date.invalidate();
        }
        if (clock != null ) {
            clock.invalidate();
        }
    }

    private void setSize(int width, int height) {
        getLayoutParams().width = width;
        getLayoutParams().height = height;
        requestLayout();
    }

    public float getAbsScaleFactor() {
        return Math.abs(getScaleX());
    }

    public void setScaleFactor(float factor) {
        setScaleFactor(factor, false);
    }

    public void setScaleFactor(final float factor, final boolean animated) {
        final float sign = mirrorText ? -1.f : 1.f;
        if (animated) {
            animate().setDuration(1000).scaleX(sign * factor).scaleY(factor);

        } else {
            setScaleX(sign * factor);
            setScaleY(factor);
            invalidate();
        }
    }

    private void setupLayoutAnalog(int parentWidth, int parentHeight, Configuration config,
                                   boolean displayInWidget) {
        if (analog_clock != null) {
            analog_clock.setStyle(AnalogClockConfig.Style.MINIMALISTIC);
        }
        final float minFontSize = 8.f; // in sp
        final float maxFontSize = 18.f; // in sp
        int widgetSize;
        if (displayInWidget) {
            widgetSize = parentHeight > 0 ? Math.min(parentWidth, parentHeight) : parentWidth;
        } else {
            widgetSize = getAnalogWidgetSize(parentWidth, config);
        }
        setSize(widgetSize, widgetSize);
        if (date != null) {
            date.setMaxWidth(widgetSize / 2);
            date.setMaxFontSizesInSp(minFontSize, maxFontSize);
            date.setTranslationY(0.2f * widgetSize);
        }
        if (weatherLayout != null) {
            weatherLayout.setMaxWidth(widgetSize / 2);
            weatherLayout.setMaxFontSizesInPx(
                    Utility.spToPx(context, minFontSize),
                    Utility.spToPx(context, maxFontSize));
            weatherLayout.update();
            weatherLayout.setTranslationY(-0.2f * widgetSize);
        }
    }

    private void setupLayoutAnalog2(int parentWidth, int parentHeight, Configuration config,
                                    boolean displayInWidget) {
        switch (layoutId) {
            case LAYOUT_ID_ANALOG:
                analog_clock.setStyle(AnalogClockConfig.Style.MINIMALISTIC);
                break;
            case LAYOUT_ID_ANALOG2:
                analog_clock.setStyle(AnalogClockConfig.Style.SIMPLE);
                break;
            case LAYOUT_ID_ANALOG3:
                analog_clock.setStyle(AnalogClockConfig.Style.ARC);
                break;
            case LAYOUT_ID_ANALOG4:
                analog_clock.setStyle(AnalogClockConfig.Style.DEFAULT);
                break;
        }
        final float minFontSize = (displayInWidget) ? 6f : 10f; // in sp
        final float maxFontSize = 20.f; // in sp

        int widgetSize;
        if (displayInWidget) {
            widgetSize = (parentHeight > 0 && parentHeight < parentWidth) ? parentHeight : parentWidth;
        } else {
            widgetSize = getAnalogWidgetSize(parentWidth, config);
        }

        analog_clock.getLayoutParams().width = widgetSize;
        analog_clock.getLayoutParams().height = widgetSize;

        if (date != null && date.getVisibility() == VISIBLE) {
            date.setMaxWidth(widgetSize / 3 * 2);
            date.setMaxHeight(widgetSize / 10);
            date.setMaxFontSizesInSp(minFontSize, maxFontSize);
            date.invalidate();
        }
        if (weatherLayout != null && weatherLayout.getVisibility() == VISIBLE) {
            weatherLayout.setMaxWidth(widgetSize / 3 * 2);
            weatherLayout.setMaxFontSizesInPx(
                    Utility.spToPx(context, minFontSize),
                    Utility.spToPx(context, maxFontSize)
            );
            weatherLayout.update();
            weatherLayout.invalidate();
        }
        int additionalHeight = (int) (getHeightOf(date) + getHeightOf(weatherLayout));
        Log.i(TAG, String.format("additionalHeight: %s", additionalHeight));
        setSize(widgetSize, widgetSize + additionalHeight);

        int measuredHeight = Utility.getHeightOfView(this);
        Log.i(TAG, "### measuredHeight=" + measuredHeight + ", parentHeight=" + parentHeight);

        if (displayInWidget && parentHeight > 0 && measuredHeight > parentHeight) {
            // shrink analog clock
            int newHeight = parentHeight - additionalHeight;
            LayoutParams params = (LayoutParams) analog_clock.getLayoutParams();
            params.gravity = Gravity.CENTER_HORIZONTAL;
            params.width = newHeight;
            params.height = newHeight;
        }
    }

    private float getHeightOf(View view) {
        if (view == null || view.getVisibility() == GONE) return 0f;
        float height = 1.2f * Utility.getHeightOfView(view);
        Log.i(TAG, String.format("visibility %d", view.getVisibility()));
        Log.i(TAG, String.format("height %f", height));
        return height;
    }

    private int getAnalogWidgetSize(int parentWidth, Configuration config) {
        switch (config.orientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                return parentWidth / 4;
            case Configuration.ORIENTATION_PORTRAIT:
            default:
                return parentWidth / 2;
        }
    }

    public void setDateFormat(String formatString) {
        CustomDigitalClock tdate = (CustomDigitalClock) date;
        tdate.setFormat12Hour(formatString);
        tdate.setFormat24Hour(formatString);
    }

    public void setTimeFormat(String formatString12h, String formatString24h) {
        if (clock == null) return;
        CustomDigitalClock tclock = (CustomDigitalClock) clock;
        tclock.setFormat24Hour(formatString24h);
        tclock.setFormat12Hour(formatString12h);
    }

    public void clearWeather() {
        if (weatherLayout == null) return;
        weatherLayout.clear();
    }

    public void update(WeatherEntry entry) {
        if (weatherLayout == null) return;
        weatherLayout.update(entry);
    }
}

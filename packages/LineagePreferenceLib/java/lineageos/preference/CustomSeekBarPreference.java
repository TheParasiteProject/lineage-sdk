/*
 * Copyright (C) 2016-2017 The Dirty Unicorns Project
 * Copyright (C) 2016-2025 crDroid Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package lineageos.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import lineageos.preference.R;
import com.android.settingslib.widget.SliderPreference;

import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;

public class CustomSeekBarPreference extends SliderPreference {

    private static final String AUTONS = "http://schemas.android.com/apk/res-auto";

    private final ConstraintsHelper mConstraints;

    private boolean mShowSign;
    @Nullable
    private String mUnits = "";
    @Nullable
    private String mDefaultValueText;
    private boolean mDefaultValueTextExists;
    private boolean mDefaultValueExists;
    private int mDefaultValue;

    private CharSequence mUserSummary;  

    private boolean mInUserDrag = false;

    public CustomSeekBarPreference(Context context, AttributeSet attrs, int defStyle, int defStyleRes) {
        this(context, attrs);
    }

    public CustomSeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
        this(context, attrs);
    }

    public CustomSeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mConstraints = new ConstraintsHelper(context, attrs, this);
        readLegacyAttrs(context, attrs);
        initDefaults(mShowSign);
        mUserSummary = super.getSummary();
        updateSummaryNow();
    }

    public CustomSeekBarPreference(Context context) {
        super(context, null);
        mConstraints = new ConstraintsHelper(context, null, this);
        initDefaults(true);
        mUserSummary = super.getSummary();
        updateSummaryNow();
    }

    private void initDefaults(boolean showSign) {
        setShowSliderValue(showSign);
        setHapticFeedbackMode(HAPTIC_FEEDBACK_MODE_ON_TICKS);
        setLabelFormater(new LabelFormatter() {
            @Override public String getFormattedValue(float value) {
                return formatValueForSummary((int) value);
            }
        });
    }

    private void readLegacyAttrs(Context c, AttributeSet attrs) {
        if (attrs == null) return;
        final TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.CustomSeekBarPreference);
        try {
            String showSign = attrs.getAttributeValue(AUTONS, "showSign");
            if (showSign != null && !showSign.isEmpty()) {
                try {
                    mShowSign = Boolean.parseBoolean(showSign);
                } catch (Exception ignored) {
                    mShowSign = false;
                }
            }

            String units = attrs.getAttributeValue(AUTONS, "units");
            if (units != null && !units.isEmpty()) {
                mUnits = units;
            }

            String continuous = attrs.getAttributeValue(AUTONS, "continuousUpdates");
            if (continuous != null && !continuous.isEmpty()) {
                try {
                    setUpdatesContinuously(Boolean.parseBoolean(continuous));
                } catch (Exception ignored) {
                    setUpdatesContinuously(false);
                }
            }

            mDefaultValueText = attrs.getAttributeValue(AUTONS, "defaultValueText");
            mDefaultValueTextExists = mDefaultValueText != null && !mDefaultValueText.isEmpty();

            String defaultValue = attrs.getAttributeValue(AUTONS, "defaultValue");
            if (defaultValue != null && !defaultValue.isEmpty()) {
                try {
                    mDefaultValue = Integer.parseInt(defaultValue);
                    mDefaultValueExists = true;
                } catch (NumberFormatException ignored) {
                    mDefaultValueExists = false;
                }
            }

            int interval = attrs.getAttributeIntValue(AUTONS, "interval", 0);
            if (interval > 0) setSliderIncrement(interval);

            // Guard against improper slider increment
            int min = getMin();
            int max = getMax();
            int span = Math.max(0, max - min);

            int step = getSliderIncrement();
            if (step <= 0 || span == 0) {
                setSliderIncrement(1); // Always use discrete steps for CustomSeekBarPreference
            } else if ((span % step) != 0) {
                int gcd = gcd(span, step);
                if (gcd <= 0) gcd = 1;
                setSliderIncrement(gcd);
            }
        } catch (Throwable ignored) {
            // keep safe defaults
        } finally {
            a.recycle();
        }
    }

    @Override
    public void onAttached() {
        super.onAttached();
        mConstraints.onAttached();
    }

    @Override
    public void setSummary(CharSequence summary) {
        mUserSummary = summary;
        updateSummaryNow();
    }

    @Override
    public void setValue(int sliderValue) {
        super.setValue(sliderValue);
        if (!mInUserDrag) updateSummaryNow();
    }

    private void updateSummaryNow() {
        CharSequence composed = composeSummary(mUserSummary, getValue());
        super.setSummary(composed);
    }

    private String formatValueForSummary(int v) {
        if (mDefaultValueExists && mDefaultValueTextExists && v == mDefaultValue) {
            return mDefaultValueText;
        }
        String s = String.valueOf(v);
        if (mShowSign && v > 0) s = "+" + s;
        if (mUnits != null && !mUnits.isEmpty()) s = s + " " + mUnits;
        return s;
    }

    private CharSequence composeSummary(CharSequence userSummary, int v) {
        final String valueText = formatValueForSummary(v);
        if (userSummary == null || userSummary.length() == 0) return valueText;
        return valueText + " \u2022 " + userSummary;
    }

    @Override
    public void setDefaultValue(Object defaultValue) {
        if (defaultValue instanceof Integer) {
            mDefaultValueExists = true;
            mDefaultValue = (Integer) defaultValue;
        }
        super.setDefaultValue(defaultValue);
        updateSummaryNow();
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mConstraints.onBindViewHolder(holder);

        final TextView summaryView = (TextView) holder.findViewById(android.R.id.summary);
        if (summaryView != null) {
            summaryView.setText(composeSummary(mUserSummary, getValue()));
        }

        final View labelFrame = holder.findViewById(
                com.android.settingslib.widget.preference.slider.R.id.label_frame);
        final TextView startText = (TextView) holder.findViewById(android.R.id.text1);
        final TextView endText = (TextView) holder.findViewById(android.R.id.text2);

        if (labelFrame != null) {
            boolean hasStart = startText != null && startText.getText() != null
                    && startText.getText().length() > 0;
            boolean hasEnd = endText != null && endText.getText() != null
                    && endText.getText().length() > 0;
            boolean parentWantsLabels = hasStart || hasEnd;

            labelFrame.setVisibility((parentWantsLabels || mDefaultValueExists) ? View.VISIBLE : View.GONE);
        }

        if (endText != null) {
            attachResetIcon(endText);
        }

        ViewGroup minusFrame = (ViewGroup) holder.findViewById(
                com.android.settingslib.widget.preference.slider.R.id.icon_start_frame);
        ImageView minusIcon = (ImageView) holder.findViewById(
                com.android.settingslib.widget.preference.slider.R.id.icon_start);

        ViewGroup plusFrame = (ViewGroup) holder.findViewById(
                com.android.settingslib.widget.preference.slider.R.id.icon_end_frame);
        ImageView plusIcon = (ImageView) holder.findViewById(
                com.android.settingslib.widget.preference.slider.R.id.icon_end);

        final Slider slider = (Slider) holder.findViewById(
                com.android.settingslib.widget.preference.slider.R.id.slider);

        int stepForClicks = Math.max(1, getSliderIncrement());

        if (minusFrame != null && minusIcon != null) {
            minusFrame.setVisibility(View.VISIBLE);
            minusIcon.setImageResource(R.drawable.ic_custom_seekbar_minus);
            minusFrame.setOnClickListener(v -> {
                if (!isEnabled()) return;
                int base = slider != null ? Math.round(slider.getValue()) : getValue();
                int newVal = Math.max(getMin(), base - stepForClicks);
                applyUserValue(newVal, slider);
                updatePlusMinusEnabledStates(holder);
            });
        }

        if (plusFrame != null && plusIcon != null) {
            plusFrame.setVisibility(View.VISIBLE);
            plusIcon.setImageResource(R.drawable.ic_custom_seekbar_plus);
            plusFrame.setOnClickListener(v -> {
                if (!isEnabled()) return;
                int base = slider != null ? Math.round(slider.getValue()) : getValue();
                int newVal = Math.min(getMax(), base + stepForClicks);
                applyUserValue(newVal, slider);
                updatePlusMinusEnabledStates(holder);
            });
        }

        updatePlusMinusEnabledStates(holder);

        if (slider != null && summaryView != null) {
            slider.addOnChangeListener((s, value, fromUser) -> {
                if (fromUser) {
                    summaryView.setText(composeSummary(mUserSummary, (int) value));
                    updatePlusMinusEnabledStates(holder);
                }
            });
            slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
                @Override
                public void onStartTrackingTouch(@NonNull Slider s) {
                    mInUserDrag = true;
                }

                @Override
                public void onStopTrackingTouch(@NonNull Slider s) {
                    mInUserDrag = false;
                    applyUserValue(Math.round(s.getValue()), s);
                    updatePlusMinusEnabledStates(holder);
                }
            });
        }
    }

    @Override
    public void onDependencyChanged(@NonNull Preference dependency, boolean disableDependent) {
        super.onDependencyChanged(dependency, disableDependent);
        notifyChanged();
    }

    private void applyUserValue(int newVal, @Nullable Slider slider) {
        if (newVal == getValue()) return;
        if (!callChangeListener(newVal)) {
            if (slider != null) slider.setValue(getValue());
            return;
        }
        setValue(newVal);
        updateSummaryNow();
        notifyChanged();
    }

    private static int gcd(int a, int b) {
        a = Math.abs(a); b = Math.abs(b);
        if (a == 0) return b;
        if (b == 0) return a;
        while (b != 0) {
            int t = b; b = a % b; a = t;
        }
        return a;
    }

    private void updatePlusMinusEnabledStates(PreferenceViewHolder holder) {
        View minusFrame = holder.findViewById(
                com.android.settingslib.widget.preference.slider.R.id.icon_start_frame);
        ImageView minusIcon = (ImageView) holder.findViewById(
                com.android.settingslib.widget.preference.slider.R.id.icon_start);
        View plusFrame = holder.findViewById(
                com.android.settingslib.widget.preference.slider.R.id.icon_end_frame);
        ImageView plusIcon = (ImageView) holder.findViewById(
                com.android.settingslib.widget.preference.slider.R.id.icon_end);
        boolean enabled = isEnabled();
        int value = getValue();

        if (minusFrame != null && minusIcon != null) {
            int min = getMin();
            minusFrame.setEnabled(enabled && (value > min));
            minusIcon.setEnabled(enabled && (value > min));
        }
        if (plusFrame  != null && plusIcon != null) {
            int max = getMax();
            plusFrame.setEnabled(enabled && (value < max));
            plusIcon.setEnabled(enabled && (value < max));
        }
    }

    private void attachResetIcon(TextView tv) {
        if (!mDefaultValueExists) {
            tv.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null);
            tv.setOnTouchListener(null);
            tv.setClickable(false);
            return;
        }

        final Drawable icon = ResourcesCompat.getDrawable(
                tv.getResources(), R.drawable.ic_custom_seekbar_reset, tv.getContext().getTheme());
        if (icon == null) return;

        tv.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, icon, null);
        tv.setCompoundDrawablePadding(dp(tv, 6));
        tv.setClickable(isEnabled());
        tv.setFocusable(isEnabled());

        tv.setOnTouchListener((v, ev) -> {
            if (!isEnabled()) return false;

            if (ev.getAction() != android.view.MotionEvent.ACTION_UP) return false;
            final boolean isRtl = androidx.core.view.ViewCompat.getLayoutDirection(tv)
                    == androidx.core.view.ViewCompat.LAYOUT_DIRECTION_RTL;
            final Drawable[] drs = tv.getCompoundDrawablesRelative();
            final Drawable end = isRtl ? drs[0] : drs[2];
            if (end == null) return false;

            final int iconW = end.getIntrinsicWidth();
            final int x = (int) ev.getX();
            if (!isRtl) {
                int left = tv.getWidth() - tv.getPaddingRight() - iconW;
                if (x >= left) { performReset(); return true; }
            } else {
                int right = tv.getPaddingLeft() + iconW;
                if (x <= right) { performReset(); return true; }
            }
            return false;
        });
    }

    private void performReset() {
        if (mDefaultValueExists) {
            applyUserValue(mDefaultValue, null);
        }
    }

    private static int dp(TextView v, int dp) {
        return Math.round(dp * v.getResources().getDisplayMetrics().density);
    }

    public void setAvailable(boolean available) {
        mConstraints.setAvailable(available);
    }

    public boolean isAvailable() {
        return mConstraints.isAvailable();
    }

}

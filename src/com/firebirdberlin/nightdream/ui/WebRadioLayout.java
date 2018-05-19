package com.firebirdberlin.nightdream.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.receivers.RadioStreamSleepTimeReceiver;
import com.firebirdberlin.nightdream.services.RadioStreamService;
import com.firebirdberlin.radiostreamapi.IcecastMetadata;
import com.firebirdberlin.radiostreamapi.StreamMetadataTask;
import com.firebirdberlin.radiostreamapi.models.RadioStation;

public class WebRadioLayout extends RelativeLayout {

    public static String TAG ="WebRadioLayout";

    public boolean locked = false;
    private Context context;
    private TextView textView;
    private ImageView buttonSleepTimer;
    private ImageView volumeMutedIndicator;
    private boolean showConnectingHint = false;
    private boolean isVisible = false;
    private ProgressBar spinner;
    private WebRadioStationButtonsLayout webRadioButtons;
    private NightDreamBroadcastReceiver broadcastReceiver = null;
    private AudioVolumeContentObserver audioVolumeContentObserver = null;
    private UserInteractionObserver userInteractionObserver;
    final private Handler handler = new Handler();

    public WebRadioLayout(Context context) {
        super(context);
        this.context = context;
    }

    public WebRadioLayout(final Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        setBackgroundResource(R.drawable.webradiopanelborder);

        textView = new TextView(context);
        textView.setId(R.id.web_radio_text_view); // id for placing spinner LEFT_OF this view
        textView.setEllipsize(TextUtils.TruncateAt.END);
        // more than one lines of text whould appear below the preset buttons
        textView.setSingleLine(true);
        int padding = Utility.dpToPx(context, 6.f);
        int paddingRight = Utility.dpToPx(context, 40.f); // let text end before sleep button
        //textView.setPadding(padding, padding, padding, padding);
        textView.setPadding(padding, padding, paddingRight, padding);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lp.addRule(RelativeLayout.CENTER_IN_PARENT);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);

        textView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                notifyUserInteraction();
                showInfoDialog();
            }
        });
        textView.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                notifyUserInteraction();
                updateMetaData();
                return true;
            }
        });

        buttonSleepTimer = new ImageView(context);
        buttonSleepTimer.setImageResource(R.drawable.ic_nightmode);

        padding = Utility.dpToPx(context, 6.f);
        buttonSleepTimer.setPadding(padding, padding, padding, padding);
        buttonSleepTimer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showSleepTimerDialog();
            }
        });
        RelativeLayout.LayoutParams lp2 = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        lp2.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        //lp2.addRule(RelativeLayout.CENTER_IN_PARENT);

        spinner = new ProgressBar(context, null, android.R.attr.progressBarStyleSmall);
        spinner.setPadding(0, padding, 0, padding);
        spinner.setVisibility(View.VISIBLE);

        RelativeLayout.LayoutParams lp3 = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp3.addRule(RelativeLayout.LEFT_OF, textView.getId());
        lp3.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

        addView(textView, lp);
        addView(buttonSleepTimer, lp2);
        addView(spinner, lp3);

        webRadioButtons = new WebRadioStationButtonsLayout(context, attrs);
        RelativeLayout.LayoutParams lp4 = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp4.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        lp4.addRule(RelativeLayout.CENTER_IN_PARENT);
        addView(webRadioButtons, lp4);

        initVolumeMutedIndicator(padding);

        updateText();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        broadcastReceiver = registerBroadcastReceiver();
        registerAudioVolumeContentObserver();
    }

    private NightDreamBroadcastReceiver registerBroadcastReceiver() {
        NightDreamBroadcastReceiver receiver = new NightDreamBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Config.ACTION_RADIO_STREAM_STARTED);
        filter.addAction(Config.ACTION_RADIO_STREAM_STOPPED);
        filter.addAction(Config.ACTION_RADIO_STREAM_READY_FOR_PLAYBACK);
        context.registerReceiver(receiver, filter);
        return receiver;
    }

    private void registerAudioVolumeContentObserver() {
        audioVolumeContentObserver = new AudioVolumeContentObserver(context, new Handler());
        context.getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, audioVolumeContentObserver );
    }

    private void unregisterAudioVolumeContentObserver() {

        if (audioVolumeContentObserver != null) {
            context.getContentResolver().unregisterContentObserver(audioVolumeContentObserver);
            audioVolumeContentObserver = null;
        }

    }

    public void setUserInteractionObserver(UserInteractionObserver o) {
        userInteractionObserver = o;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        try {
            context.unregisterReceiver(broadcastReceiver);
            unregisterAudioVolumeContentObserver();
        } catch (IllegalArgumentException ignored) {
        }
    }

    public void setCustomColor(int accentColor, int textColor) {
        Drawable bg = getBackground();
        bg.setColorFilter( accentColor, PorterDuff.Mode.MULTIPLY );

        buttonSleepTimer.setColorFilter(
                RadioStreamSleepTimeReceiver.isSleepTimeSet() ? accentColor : textColor,
                PorterDuff.Mode.SRC_ATOP
        );
        volumeMutedIndicator.setColorFilter(
                textColor,
                PorterDuff.Mode.SRC_ATOP
        );
        textView.setTextColor(textColor);

        if (webRadioButtons != null) {
            webRadioButtons.setCustomColor(accentColor, textColor);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void showSleepTimerDialog() {
        FragmentManager fm = ((Activity) getContext()).getFragmentManager();
        SleepTimerDialogFragment dialog = new SleepTimerDialogFragment();
        dialog.show(fm, "sleep_timer");
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    protected void hide() {
        isVisible = false;
    }

    protected void updateText() {
        if (textView == null) return;

        int radioStationIndex = RadioStreamService.getCurrentRadioStationIndex();
        RadioStation station = RadioStreamService.getCurrentRadioStation();

        if (RadioStreamService.streamingMode == RadioStreamService.StreamingMode.RADIO) {

            if (radioStationIndex >= 0 && station != null) {
                textView.setText(station.name);
                webRadioButtons.setActiveStation(radioStationIndex);
            }

            if (!isVisible) {
                // display current meta infos when panel was hidden and becomes visible
                updateMetaData();
            }
        } else {
            textView.setText("");
            webRadioButtons.clearActiveStation();
        }
        if (spinner != null) {
            spinner.setVisibility(showConnectingHint ? View.VISIBLE : View.GONE);
        }
        webRadioButtons.invalidate();
        updateVolumeMutedIndicator();
        isVisible = true;
    }



    private Runnable resetDefaultText = new Runnable() {
        @Override
        public void run() {
            updateText();
        }
    };

    private Runnable hideConnectingHint = new Runnable() {
        @Override
        public void run() {
            setShowConnectingHint(false);
        }
    };

    private void updateMetaData() {

        if (RadioStreamService.getCurrentRadioStation() == null) {
            return;
        };

        StreamMetadataTask.AsyncResponse metadataCallback = new StreamMetadataTask.AsyncResponse() {

            @Override
            public void onMetadataRequestStarted() {
                setShowConnectingHint(true, true);
            }

            @Override
            public void onMetadataAvailable(IcecastMetadata metadata) {
                String streamTitle = (metadata != null ? metadata.streamTitle : null);
                //Log.i(TAG, "onMetadataAvailable, streamTitle=" + (streamTitle != null ? streamTitle : "null"));
                showMetaTitle(streamTitle);
            }

        };

        RadioStreamService.updateMetaData(metadataCallback, context);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void showInfoDialog() {

        // dialog not available while stream is still loading or stopped
        if (!RadioStreamService.isReadyForPlayback() || RadioStreamService.getCurrentRadioStation() == null) {
            return;
        };

        FragmentManager fm = ((Activity) getContext()).getFragmentManager();
        RadioInfoDialogFragment dialog = new RadioInfoDialogFragment();
        dialog.show(fm, "radio info");

    }

    private void showMetaTitle(final String metaTitle) {
        Log.i(TAG, "showMetaTitle");
        setShowConnectingHint(false);
        if (metaTitle != null && !metaTitle.isEmpty()) {

            // use hander.post to update text, otherwise updated text doesnt show up sometimes
            // (e.g. on update after panel becomes visible)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    textView.setText(metaTitle);
                }
            });

            // switch back to radio name after x seconds
            handler.removeCallbacks(resetDefaultText);
            handler.postDelayed(resetDefaultText, 5000);
        }
    }

    private void setShowConnectingHint(boolean showConnectingHint) {
        setShowConnectingHint(showConnectingHint, false);
    }

    private void setShowConnectingHint(boolean showConnectingHint, boolean autoHide) {
        this.showConnectingHint = showConnectingHint;
        spinner.setVisibility(showConnectingHint ? View.VISIBLE : View.GONE);
        invalidate();

        if (autoHide) {
            handler.postDelayed(hideConnectingHint, 10000); // hide animation after 10s if anything goes wrong
        }
    }

    @Override
    public void setClickable(boolean clickable) {
        super.setClickable(clickable);
        buttonSleepTimer.setClickable(clickable);
        webRadioButtons.setClickable(clickable);
    }

    private void initVolumeMutedIndicator(int padding) {
        volumeMutedIndicator = new ImageView(context);
        volumeMutedIndicator.setImageResource(R.drawable.ic_no_audio);
        volumeMutedIndicator.setPadding(padding, padding, padding, padding);

        RelativeLayout.LayoutParams lp5 = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp5.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        lp5.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        addView(volumeMutedIndicator, lp5);

        volumeMutedIndicator.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (audio != null) {
                    int maxVol = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    //Log.i(TAG, "maxVol=" + maxVol);
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, Math.round((float) maxVol * 0.1f), AudioManager.FLAG_SHOW_UI);
                }
            }
        });

        updateVolumeMutedIndicator();
    }

    private void updateVolumeMutedIndicator() {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audio == null) {
            return;
        }
        int currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);

        boolean muted = false;
        if (Build.VERSION.SDK_INT >= 23) {
            muted = audio.isStreamMute(AudioManager.STREAM_MUSIC);
        }

        updateVolumeMutedIndicatorVisibility(currentVolume, muted);
    }

    private void updateVolumeMutedIndicatorVisibility(int currentVolume, boolean muted) {

        if (volumeMutedIndicator != null) {
            volumeMutedIndicator.setVisibility(currentVolume > 0 && !muted ? GONE : VISIBLE);
        }
    }

    private void notifyUserInteraction() {
        if (userInteractionObserver != null) {
            userInteractionObserver.notifyAction();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        notifyUserInteraction();
        return false;
    }

    class NightDreamBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Config.ACTION_RADIO_STREAM_STARTED.equals(action)) {
                setShowConnectingHint(true);
                updateText();
            } else if (Config.ACTION_RADIO_STREAM_READY_FOR_PLAYBACK.equals(action)) {
                setShowConnectingHint(false);
                updateMetaData();
                updateText();
            } else if (Config.ACTION_RADIO_STREAM_STOPPED.equals(action)) {
                updateText();
                setShowConnectingHint(false);
            }
        }
    }

    class AudioVolumeContentObserver extends ContentObserver {

        private int previousVolume = -1;
        private boolean previousMuted = false;

        AudioVolumeContentObserver(Context context, Handler handler) {
            super(handler);
            AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audio != null) {
                previousVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
            }
        }

        @Override
        public boolean deliverSelfNotifications() {
            return super.deliverSelfNotifications();
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audio == null) {
                return;
            }
            int currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);

            boolean muted = false;
            if (Build.VERSION.SDK_INT >= 23) {
                muted = audio.isStreamMute(AudioManager.STREAM_MUSIC);
            }


            if (previousVolume != currentVolume || muted != previousMuted) {
                previousVolume = currentVolume;
                previousMuted = muted;

                Log.i(TAG, "volume/muted changed");

                updateVolumeMutedIndicatorVisibility(currentVolume, muted);
            }
        }
    }
}

package ben.cn.googletrainingmanagingaudioplayback;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.IOException;

import ben.cn.googletrainingmanagingaudioplayback.databinding.ActivityMainBinding;

import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private ComponentName mRemoteControlReceiverComponentName;
    private MediaPlayer mp;
    private AudioManager am;
    private final IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private final BroadcastReceiver myNoisyAudioStreamReceiver = new NoisyAudioStreamReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding activityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        activityMainBinding.setActivity(this);

        requestPermissions();

        mRemoteControlReceiverComponentName = new ComponentName(getPackageName(), RemoteControlReceiver.class.getName());

        // From this point onwards, pressing the volume keys on the device affect the audio stream
        // you specify (in this case “music”) whenever the target activity or fragment is visible
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        checkHardwareBeingUsed();
    }

    public View.OnClickListener mPlayOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Request audio focus for playback
            int result = am.requestAudioFocus(afChangeListener,
                    // Use the music stream.
                    AudioManager.STREAM_MUSIC,
                    // Request permanent focus.
                    AudioManager.AUDIOFOCUS_GAIN);

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // TODO: 2016/11/5
                // Start listening for button presses
                am.registerMediaButtonEventReceiver(mRemoteControlReceiverComponentName);
                // Start playback.
                startPlayBack("1.mp3");
            }
        }
    };
    public View.OnClickListener mPlayTransientOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Request audio focus for playback
            int result = am.requestAudioFocus(afChangeListener,
                    // Use the music stream.
                    AudioManager.STREAM_MUSIC,
                    // Request permanent focus.
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // TODO: 2016/11/5
                // Start listening for button presses
                am.registerMediaButtonEventReceiver(mRemoteControlReceiverComponentName);
                // Start playback.
                startPlayBack("2.mp3");
            }
        }
    };
    public View.OnClickListener mPlaybackFinishedOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            stopPlayback();
        }
    };
    private final AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            Log.d(TAG, focusChange + "");
            if (focusChange == AUDIOFOCUS_LOSS_TRANSIENT) {
                // we pause the playback or our media player object??
                // TODO: 2016/11/5
                if (mp != null) mp.pause();
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                // Resume playback
                if (mp != null) {
                    mp.start();
                    // Raise it back to normal
                    mp.setVolume(1f, 1f);
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                // Stop playback
                stopPlayback();
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                // Lower the volume
                mp.setVolume(0.3f, 0.3f);
            }
        }
    };

    private void startPlayBack(String musicFileName) {
        if (mp != null && mp.isPlaying()) return;
        if (mp != null) mp.release();
        //构建MediaPlayer对象
        mp = new MediaPlayer();
        try {
            File musicFile = new File(Environment.getExternalStorageDirectory(), musicFileName);
            mp.setDataSource(musicFile.getAbsolutePath());//设置文件路径
            mp.prepare();//准备
        } catch (IOException e) {
            e.printStackTrace();
        }
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                // Abandon audio focus when playback complete
                am.abandonAudioFocus(afChangeListener);
            }
        });
        mp.start();//开始播放
        registerReceiver(myNoisyAudioStreamReceiver, intentFilter);
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    0);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mp != null) mp.release();
    }

    private void checkHardwareBeingUsed() {
        if (am.isBluetoothA2dpOn()) {
            // Adjust output for Bluetooth.
            Log.d(TAG, "Bluetooth");
        } else if (am.isSpeakerphoneOn()) {
            // Adjust output for Speakerphone.
            Log.d(TAG, "Speakerphone");
        } else if (am.isWiredHeadsetOn()) {
            // Adjust output for headsets
            Log.d(TAG, "Headsets");
        } else {
            // If audio plays and no one can hear it, is it still playing?
            Log.d(TAG, "No one can hear it");
        }
    }

    private void stopPlayback() {
        if (mp != null && mp.isPlaying()) {
            mp.stop();
        }
        // Abandon audio focus when playback complete
        am.abandonAudioFocus(afChangeListener);
        // TODO: 2016/11/5
        // Stop listening for button presses
        am.unregisterMediaButtonEventReceiver(mRemoteControlReceiverComponentName);
        try {
            unregisterReceiver(myNoisyAudioStreamReceiver);
        } catch (IllegalArgumentException e) {
            
        }
    }

    private class NoisyAudioStreamReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                // Pause the playback
                if (mp != null) mp.pause();
            }
        }
    }
}

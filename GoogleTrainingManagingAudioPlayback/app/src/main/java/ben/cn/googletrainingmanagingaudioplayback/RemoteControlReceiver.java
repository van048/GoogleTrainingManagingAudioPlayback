package ben.cn.googletrainingmanagingaudioplayback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

public class RemoteControlReceiver extends BroadcastReceiver {
    private static final String TAG = RemoteControlReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        // extract the media button pressed and affects the media playback accordingly.
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (KeyEvent.KEYCODE_MEDIA_PAUSE == event.getKeyCode()) {
                // Handle key press.
                // TODO: 2016/11/5
                Log.d(TAG, "Handle key press.");
            }
        }
    }
}

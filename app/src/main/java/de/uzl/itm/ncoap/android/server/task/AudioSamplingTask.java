package de.uzl.itm.ncoap.android.server.task;

/**
 * Created by olli on 27.08.15.
 */

import android.media.AudioRecord;

import de.uzl.itm.ncoap.android.server.MainActivity;

/**
 * Class to sample audio (peak amplitude) from the mic every 200ms
 */
public class AudioSamplingTask implements Runnable{

    private MainActivity mainActivity;
    private AudioRecord audioRecord;
    private int bufferSize;

    public AudioSamplingTask(MainActivity mainActivity, AudioRecord audioRecord, int bufferSize){
        this.mainActivity = mainActivity;
        this.audioRecord = audioRecord;
        this.bufferSize = bufferSize;
    }

    @Override
    public void run() {
        try {
            short[] buffer = new short[bufferSize];
            int bufferReadResult;
            int highestLevel = 0;

            bufferReadResult = audioRecord.read(buffer, 0, bufferSize);

            for (int i = 0; i < bufferReadResult; i++) {
                if (buffer[i] > highestLevel) {
                    highestLevel = buffer[i];
                }
            }

            mainActivity.setNoiseLevel(highestLevel);

            mainActivity.getHandler().postDelayed(this, 200);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

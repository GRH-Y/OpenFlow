package server.mp4;


import java.io.File;
import java.io.RandomAccessFile;

/**
 * AAC音频解析
 * Created by dell on 9/19/2017.
 *
 * @author yyz
 */
public class AacAnalysis {
    private static int config, channel, profile, samplingRate;

    /**
     * MPEG-4 Audio Object Types supported by ADTS.
     **/
    private static final String[] AUDIO_OBJECT_TYPES = {
            "NULL",
            "AAC Main",
            "AAC LC (Low Complexity)",
            "AAC SSR (Scalable Sample Rate)",
            "AAC LTP (Long Term Prediction)"
    };

    /**
     * There are 13 supported frequencies by ADTS.
     **/
    public static final int[] AUDIO_SAMPLING_RATES = {
            96000,
            88200,
            64000,
            48000,
            44100,
            32000,
            24000,
            22050,
            16000,
            12000,
            11025,
            8000,
            7350,
            -1,
            -1,
            -1,
    };

    public static void parserPath(String path) throws Exception {
        // ADTS header inputStream 7 or 9 bytes long
        byte[] buffer = new byte[9];
        File file = new File(path);
        RandomAccessFile accessFile = new RandomAccessFile(file, "r");

        // ADTS packets startTask with a sync word: 12bits set to 1
        while (true) {
            if ((accessFile.readByte() & 0xFF) == 0xFF) {
                buffer[0] = accessFile.readByte();
                if ((buffer[0] & 0xF0) == 0xF0) {
                    break;
                }
            }
        }

        accessFile.read(buffer, 1, 5);
        accessFile.close();

        int mSamplingRateIndex = (buffer[1] & 0x3C) >> 2;
        profile = ((buffer[1] & 0xC0) >> 6) + 1;
        channel = (buffer[1] & 0x01) << 2 | (buffer[2] & 0xC0) >> 6;
        samplingRate = AUDIO_SAMPLING_RATES[mSamplingRateIndex];

        // 5 bits for the object type / 4 bits for the sampling rate / 4 bits for the channel / padding
        config = (profile & 0x1F) << 11 | (mSamplingRateIndex & 0x0F) << 7 | (channel & 0x0F) << 3;

//        Log.i(TAG, "MPEG VERSION: " + ((buffer[0] & 0x08) >> 3));
//        Log.i(TAG, "PROTECTION: " + (buffer[0] & 0x01));
//        Log.i(TAG, "PROFILE: " + AUDIO_OBJECT_TYPES[mProfile]);
//        Log.i(TAG, "SAMPLING FREQUENCY: " + mQuality.samplingRate);
//        Log.i(TAG, "CHANNEL: " + mChannel);
    }

    public static int getConfig() {
        return config;
    }

    public static int getChannel() {
        return channel;
    }

    public static int getProfile() {
        return profile;
    }

    public static int getSamplingRate() {
        return samplingRate;
    }
}

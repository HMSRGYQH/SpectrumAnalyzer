package com.example.spectrumanalyzer;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * Created by usr1 on 2017/05/21.
 */

public class SpectrumRecorder {
    private static final String TAG = "SpectrumRecorder";
    private static final int SAMPLE_RATE = 44100;
    private static final int BIT_PER_SAMPLE = 16;
    private SpectrumRecorder.OnRecordPositionUpdateListener mRecordPositionUpdateListener;
    private AudioRecord mAudioRecord;
    private int mSamples;
    private int mFreqNum;
    private double[] mSpectrum;

    SpectrumRecorder () {
        int bufferSize;

        Log.d(TAG, "SpectrumRecorder");

        mRecordPositionUpdateListener = null;
        mAudioRecord = null;
        mSamples = 0;

        // 必要となるバッファサイズを計算する
        bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );
        Log.d(TAG, "bufferSize: " + bufferSize);
        bufferSize = 4096;

        // AudioRecordを初期化する
        mAudioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
        );

        // 一度に読み込むサンプル数を計算する
        mSamples = bufferSize / 2;  // 16bit = 2byteだから

        // FFTで得られるデータ数を計算する
        mFreqNum = mSamples / 2;

        // メモリを確保する
        mSpectrum = new double[mFreqNum];
        for (int i = 0; i < mFreqNum; i++) {
            mSpectrum[i] = 0.0;
        }

        // AudioRecord録音の設定をする
        mAudioRecord.setRecordPositionUpdateListener(new AudioRecord.OnRecordPositionUpdateListener() {
            @Override
            public void onPeriodicNotification(AudioRecord recorder) {
                short buf[];
                Fft4g fft;
                double[] fftData;

                // エラー処理
                if (mAudioRecord == null) {
                    return;
                }

                // バッファの初期化をする
                buf = new short[mSamples];
                fft = new Fft4g(mSamples);
                fftData = new double[mSamples];

                // 録音データを読み出す
                mAudioRecord.read(buf, 0, buf.length);

                // FFTへの入力データを作成する
                for (int i = 0; i < mSamples; i++) {
                    fftData[i] = (double)buf[i];
                }

                // FFTを実行する
                fft.rdft(1, fftData);

                // 正規化された振幅を計算する
                for (int i = 0; i < mFreqNum; i++) {
                    mSpectrum[i] = (Math.sqrt(
                            Math.pow(fftData[i * 2] / mFreqNum, 2)
                            + Math.pow(fftData[i * 2 + 1] / mFreqNum, 2)
                    ) / Math.pow(2, BIT_PER_SAMPLE)) / Math.sqrt(2);

//                    if (i == 40) {
//                        Log.d(TAG, "freq " + (((double)44100 / 2) / mFreqNum) * i);
//                        Log.d(TAG, "mSpectrum " + mSpectrum[i]);
//                        Log.d(TAG, "fftData " + fftData[i * 2]);
//                        Log.d(TAG, "fftDataNorm " + fftData[i * 2] / mFreqNum);
//                        Log.d(TAG, "fftDataNorm " + fftData[i * 2 + 1] / mFreqNum);
//                    }
                }

                // コールバック関数を呼び出す
                mRecordPositionUpdateListener.onPeriodicNotification(mSpectrum, mFreqNum);
            }

            @Override
            public void onMarkerReached(AudioRecord recorder) {
            }
        });

        // 通知間隔を受信周期にする
        mAudioRecord.setPositionNotificationPeriod(mSamples);
    }

    public interface OnRecordPositionUpdateListener {
        public void onPeriodicNotification(double[] spectrum, int spectrumNum);
    }

    public void setRecordPositionUpdateListener(SpectrumRecorder.OnRecordPositionUpdateListener listener) {
        mRecordPositionUpdateListener = listener;
    }

    public void start(){
        // エラー処理
        if (mAudioRecord == null) {
            Log.d(TAG, "start : not initialized");
            return;
        }

        // 録音する
        mAudioRecord.startRecording();
    }

    public void stop(){
        // エラー処理
        if (mAudioRecord == null) {
            Log.d(TAG, "stop : not initialized");
            return;
        }

        // 停止する
        mAudioRecord.stop();
    }

    public int getSampleRate(){
        return SAMPLE_RATE;
    }
}
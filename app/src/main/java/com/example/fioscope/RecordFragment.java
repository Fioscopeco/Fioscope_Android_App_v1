package com.example.fioscope;


import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A simple {@link Fragment} subclass.
 */
public class RecordFragment extends Fragment implements View.OnClickListener {


    private NavController navController;

    private ImageButton listBtn;
    private ImageButton recordBtn;
    private ImageButton soundbtn;
    private TextView filenameText;

    private boolean isRecording = false;
    private boolean isSound = false;

    private String recordPermission= Manifest.permission.RECORD_AUDIO;
    private int PERMISSION_CODE= 21;

    private MediaRecorder mediaRecorder;
    private String recordFile;

    private Chronometer timer;

    boolean m_isRun = false;
    int m_count = 0;
    int SAMPLE_RATE = 44100;
    int BUF_SIZE = 256;
    short[] buffer = new short[BUF_SIZE];
    AudioRecord m_record;
    AudioTrack m_track;
    NoiseSuppressor m_suppressor;
    AcousticEchoCanceler m_canceler;
    Thread m_thread;
    Timer time;

    public RecordFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_record, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Intitialize Variables
        navController = Navigation.findNavController(view);
        listBtn = view.findViewById(R.id.record_list_btn);
        recordBtn = view.findViewById(R.id.record_btn);
        timer = view.findViewById(R.id.record_timer);
        filenameText = view.findViewById(R.id.record_filename);
        soundbtn = view.findViewById(R.id.playBackBtn);


        /* Setting up on click listener
           - Class must implement 'View.OnClickListener' and override 'onClick' method
         */
        listBtn.setOnClickListener(this);
        recordBtn.setOnClickListener(this);
        soundbtn.setOnClickListener(this);

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onClick(View v) {
        /*  Check, which button is pressed and do the task accordingly
         */
        switch (v.getId()) {
            case R.id.record_list_btn:
                /*
                Navigation Controller
                Part of Android Jetpack, used for navigation between both fragments
                 */
                if(isRecording){
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
                    alertDialog.setPositiveButton("OKAY", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            navController.navigate(R.id.action_recordFragment_to_patientAudioFragment);
                            isRecording = false;
                        }
                    });
                    alertDialog.setNegativeButton("CANCEL", null);
                    alertDialog.setTitle("Audio Still recording");
                    alertDialog.setMessage("Are you sure, you want to stop the recording?");
                    alertDialog.create().show();
                } else {
                    navController.navigate(R.id.action_recordFragment_to_patientAudioFragment);
                }
                break;

            case R.id.record_btn:
                if(isRecording) {
                    //Stop Recording
                    stopRecording();

                    // Change button image and set Recording state to false
                    recordBtn.setImageDrawable(getResources().getDrawable(R.drawable.recording_btn_stopped, null));
                    isRecording = false;
                } else {
                    //Check permission to record audio
                    if(checkPermissions()) {
                        //Start Recording
                        startRecording();

                        // Change button image and set Recording state to false
                        recordBtn.setImageDrawable(getResources().getDrawable(R.drawable.recording_btn_recording, null));
                        isRecording = true;
                    }
                }
                break;

            case R.id.playBackBtn:
                if (isSound) {
                    m_isRun = true;

                    soundbtn.setImageDrawable(getResources().getDrawable(R.drawable.rsz_2logomakr_on));
                    isSound = false;
                } else {
                    m_isRun = false;
                    m_count = 0;
                    do_loopback();
                    soundbtn.setImageDrawable(getResources().getDrawable(R.drawable.rsz_2logomakr_off));
                    isSound = true;
                }
                break;


        }
    }

    private void do_loopback() {
        m_thread = new Thread() {
            public void run() {

                // stream audio
                int buffersize = BUF_SIZE;
                try {
                    buffersize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                            AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT);

                    if (buffersize <= BUF_SIZE) {
                        buffersize = BUF_SIZE;
                    }

                    m_record = new AudioRecord(MediaRecorder.AudioSource.MIC,
                            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT, buffersize * 1);
                    if (NoiseSuppressor.isAvailable()) {
                        m_suppressor = NoiseSuppressor.create(m_record.getAudioSessionId());
                    }
                    if (AcousticEchoCanceler.isAvailable()) {
                        m_canceler = AcousticEchoCanceler.create(m_record.getAudioSessionId());
                    }

                    m_track = new AudioTrack(AudioManager.STREAM_MUSIC,
                            SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT, buffersize * 1,
                            AudioTrack.MODE_STREAM);

                    m_track.setPlaybackRate(SAMPLE_RATE);
                } catch (Throwable t) {
                    Log.e("Error", "Initializing Audio Record and Play objects Failed "+t.getLocalizedMessage());
                    return;
                }

                m_record.startRecording();
                m_track.play();

                while (true) { //m_isRun) {
                    int samplesRead = m_record.read(buffer, 0, buffer.length);

                    if (m_isRun)
                        m_track.write(buffer, 0, samplesRead);

                    yield();
                }

            }
        };

        m_thread.start();
    }


    private void stopRecording() {
        timer.stop();

        filenameText.setText("Recording Stopped, Saved to: " +recordFile);

        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;
    }




    private void startRecording() {
        timer.setBase(SystemClock.elapsedRealtime());
        timer.start();

        String recordPath = getActivity().getExternalFilesDir("/").getAbsolutePath();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_hh_mm", Locale.CANADA);
        Date now = new Date();

        recordFile="Fioscope_" + formatter.format(now)  +".3gp";

        filenameText.setText("Now Recording audio");

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setOutputFile(recordPath + "/" + recordFile);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mediaRecorder.start();
    }


    private boolean checkPermissions() {
        if(ActivityCompat.checkSelfPermission(getContext(), recordPermission) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            ActivityCompat.requestPermissions(getActivity(), new String[]{recordPermission}, PERMISSION_CODE);
            return false;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (isRecording) {
            stopRecording();
        }
    }
}

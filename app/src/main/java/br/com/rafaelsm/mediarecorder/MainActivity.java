package br.com.rafaelsm.mediarecorder;

import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.TextureView;

public class MainActivity extends AppCompatActivity {
    private TextureView mPreview;
    private RecorderHelper recorderHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        mPreview = (TextureView) findViewById(R.id.surface_view);
        recorderHelper = new RecorderHelper(mPreview);
        recorderHelper.startRecording();
    }

    @Override
    protected void onPause() {
        super.onPause();
        recorderHelper.releaseMediaRecorder();
        recorderHelper.releaseCamera();
    }
}

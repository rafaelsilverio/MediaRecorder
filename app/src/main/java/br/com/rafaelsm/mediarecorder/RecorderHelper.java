package br.com.rafaelsm.mediarecorder;

import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.TextureView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class RecorderHelper {
    private TextureView mPreview;
    private Camera mCamera;
    private CamcorderProfile mProfile;

    private MediaRecorder mMediaRecorder;
    private String filePath;

    public static final int MEDIA_TYPE_VIDEO = 1;

    public RecorderHelper(TextureView mPreview) {
        this.mPreview = mPreview;
    }

    public void startRecording() {
        new PrepareAndRecord().execute(null, null, null);
    }

    public void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            if (mCamera != null) {
                mCamera.lock();
            }
        }
    }

    public void releaseCamera(){
        if (mCamera != null){
            mCamera.release();
            mCamera = null;
        }
    }

    public static Camera getFrontalCameraInstance() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int cameraIndex = 0; cameraIndex < Camera.getNumberOfCameras(); cameraIndex++) {
            Camera.getCameraInfo(cameraIndex, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                return Camera.open(cameraIndex);
            }
        }
        return null;
    }

    public static  Camera.Size getSafePreviewSize(List<Camera.Size> sizes,
                                                  int surfaceWidth, int surfaceHeight) {
        double minDiff = Double.MAX_VALUE;
        Integer currentBestFitIndex = 0;
        int indexCount = 0;
        for (Camera.Size size : sizes) {
            double heightDifference = Math.abs(size.height - surfaceHeight);
            if (heightDifference < minDiff) {
                currentBestFitIndex = indexCount;
                minDiff = heightDifference;
            }
            indexCount++;
        }
        return sizes.get(currentBestFitIndex);
    }

    public static File getOutputMediaFile(int type) {
        if (!Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            return null;
        }

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "CameraSample");
        if (!mediaStorageDir.exists()) {
            if (! mediaStorageDir.mkdirs()) {
                Log.d("CameraSample", "failed to create directory");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath()
                    + File.separator + "VID_RecorderHelper_"+ timeStamp + ".mp4");
        } else {
            return null;
        }
        return mediaFile;
    }

    class PrepareAndRecord extends AsyncTask<Void, Void, Boolean> {

        private boolean prepareCamera(boolean defineDimensions) {
            mCamera = getFrontalCameraInstance();

            Camera.Size optimalSize = getSafePreviewSize(mCamera.getParameters().getSupportedPreviewSizes(),
                            mPreview.getWidth(), mPreview.getHeight());

            mProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_LOW);
            if (defineDimensions) {
                mProfile.videoFrameWidth = optimalSize.width;
                mProfile.videoFrameHeight = optimalSize.height;
            }

            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(mProfile.videoFrameWidth, mProfile.videoFrameHeight);
            mCamera.setParameters(parameters);

            try {
                mCamera.setPreviewTexture(mPreview.getSurfaceTexture());
            } catch (Exception e) {
                Log.e("RecorderHelper", e.getMessage());
                return false;
            }
            return true;
        }

        private boolean prepareRecorder() {
            mMediaRecorder = new MediaRecorder();

            mCamera.unlock();
            mMediaRecorder.setCamera(mCamera);
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

            mMediaRecorder.setProfile(mProfile);
            filePath = getOutputMediaFile(MEDIA_TYPE_VIDEO).toString();
            mMediaRecorder.setOutputFile(filePath);

            try {
                mMediaRecorder.prepare();
            } catch (Exception e) {
                Log.e("RecorderHelper", e.getMessage());
                return false;
            }

            return true;
        }

        private boolean prepareResources() {
            return prepareCamera(true) && prepareRecorder();
        }

        @Override
        protected Boolean doInBackground(Void... args) {
            if (prepareResources()) {
                try {
                    mMediaRecorder.start();
                } catch (Exception e) {
                    releaseMediaRecorder();
                    mCamera.lock();
                    releaseCamera();

                    prepareCamera(false);
                    prepareRecorder();
                    mMediaRecorder.start();
                }
            } else {
                releaseMediaRecorder();
                return false;
            }
            return true;
        }
    }
}

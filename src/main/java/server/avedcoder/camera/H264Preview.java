package server.avedcoder.camera;


import util.LogDog;

/**
 * Created by dell on 11/13/2017.
 */

public class H264Preview {

    public static final int CAMERA_PIX_FMT_H264 = 0; // V4L2_PIX_FMT_H264    H264
    public static final int CAMERA_PIX_FMT_YUYV = 0; // V4L2_PIX_FMT_YUYV    422

    private static boolean isInit = false;
    private static boolean isOpen = false;

    static {
        try {
            System.load("/home/ubuntu/IdeaProjects/RtspServer/jni/PreviewH264.so");
            isInit = true;
        } catch (Throwable e) {
            isInit = false;
            e.printStackTrace();
            LogDog.e("==> H264Preview loadLibrary libPreviewH264.so error!");
        }
    }

    // JNI functions
    private static native byte[] nativeProcessCamera();

    private static native int nativePrepareCamera(int width, int height, int pixelFormat);

    private static native void nativeStopCamera();


    public static boolean isOpen() {
        return isOpen;
    }

    public static boolean openCamera(int width, int height, int pixelFormat) {
        if (isInit && !isOpen) {
            int ret = nativePrepareCamera(width, height, pixelFormat);
            isOpen = ret == 0;
        }
        return isOpen;
    }

    public static byte[] getH264() {
        if (!isInit || !isOpen) {
            return null;
        }
        return nativeProcessCamera();
    }

    public static void stopCamera() {
        if (isInit && isOpen) {
            nativeStopCamera();
        }
    }
}

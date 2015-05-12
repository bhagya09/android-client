package pl.droidsonroids.gif;

import android.os.SystemClock;

import java.util.concurrent.TimeUnit;

class RenderTask extends SafeRunnable {

    RenderTask(GifDrawable gifDrawable) {
        super(gifDrawable);
    }

    private final Runnable mNotifyListenersTask = new Runnable() {
        @Override
        public void run() {
            for (AnimationListener listener : mGifDrawable.mListeners)
                listener.onAnimationCompleted();
        }
    };

    @Override
    public void doWork() {
        final long invalidationDelay = mGifDrawable.mNativeInfoHandle.renderFrame(mGifDrawable.mBuffer);
        if (invalidationDelay >= 0) {
            mGifDrawable.mNextFrameRenderTime = SystemClock.elapsedRealtime() + invalidationDelay;
            if (mGifDrawable.isVisible()) {
                if (mGifDrawable.mIsRunning && !mGifDrawable.mIsRenderingTriggeredOnDraw) {
                    mGifDrawable.mExecutor.schedule(this, invalidationDelay, TimeUnit.MILLISECONDS);
                }
            }
        } else {
            mGifDrawable.mNextFrameRenderTime = Long.MIN_VALUE;
            mGifDrawable.mIsRunning = false;
        }
        if (!mGifDrawable.mListeners.isEmpty() && mGifDrawable.getCurrentFrameIndex() == mGifDrawable.mNativeInfoHandle.frameCount - 1) {
            mGifDrawable.scheduleSelf(mNotifyListenersTask, 0L);
        }
        if (mGifDrawable.isVisible() && !mGifDrawable.mInvalidationHandler.hasMessages(0)) {
            mGifDrawable.mInvalidationHandler.sendEmptyMessageAtTime(0, 0);
        }
    }
}

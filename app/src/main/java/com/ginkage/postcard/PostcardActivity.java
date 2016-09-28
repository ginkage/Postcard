package com.ginkage.postcard;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

public class PostcardActivity extends Activity {
	private PostcardSurfaceView mGLView = null;
	private final Handler mHandler = new Handler();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
    {
		super.onCreate(savedInstanceState);

        // Create a GLSurfaceView instance and set it
		// as the ContentView for this Activity
		mGLView = new PostcardSurfaceView(this);
		setContentView(mGLView);

		mHandler.removeCallbacks(mUpdateTimeTask);
		mHandler.postDelayed(mUpdateTimeTask, 1000);
	}

	@Override
	protected void onPause()
    {
		super.onPause();
		// The following call pauses the rendering thread.
		// If your OpenGL application is memory intensive,
		// you should consider de-allocating objects that
		// consume significant memory here.
		if (mGLView != null)
			mGLView.onPause();
		mHandler.removeCallbacks(mUpdateTimeTask);
	}

	@Override
	protected void onResume()
    {
		super.onResume();
		// The following call resumes a paused rendering thread.
		// If you de-allocated graphic objects for onPause()
		// this is a good place to re-allocate them.
		if (mGLView != null)
			mGLView.onResume();
		mHandler.removeCallbacks(mUpdateTimeTask);
		mHandler.postDelayed(mUpdateTimeTask, 1000);
	}

	private final Runnable mUpdateTimeTask = new Runnable() {
		public void run() {
			if (mGLView != null) {
				if (mGLView.mRenderer != null) {
					float fps = mGLView.mRenderer.fps;
					setTitle(String.format("%.1f FPS", fps));
				}
			}

			mHandler.postDelayed(this, 1000);
		}
	};
}


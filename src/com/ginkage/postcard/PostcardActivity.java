package com.ginkage.postcard;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.MotionEvent;
import android.widget.Toast;

public class PostcardActivity extends Activity
{
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
					//setTitle(String.format("%.1f FPS", fps));
					//setTitle("Стяне от Нявы");
					if (mGLView.mRenderer.showText)
						setTitle(String.format("%.1f FPS (fast mode)", fps));
					else
						setTitle(String.format("%.1f FPS (normal mode)", fps));
				}
			}

			mHandler.postDelayed(this, 1000);
		}
	};
}

class PostcardSurfaceView extends GLSurfaceView
{
	public PostcardRenderer mRenderer = null;

	public PostcardSurfaceView(Context context) {
		super(context);

		// Create an OpenGL ES 2.0 context.
		setEGLContextClientVersion(2);

		// Set the Renderer for drawing on the GLSurfaceView
		mRenderer = new PostcardRenderer(context);
		setRenderer(mRenderer);
	}

	@Override 
	public boolean onTouchEvent(MotionEvent e) {
		// MotionEvent reports input details from the touch screen
		// and other input controls. In this case, you are only
		// interested in events where the touch position changed.

		switch (e.getAction()) {
			case MotionEvent.ACTION_UP:
				mRenderer.showText = !mRenderer.showText;
				break;
		}

		return true;
	} 
}
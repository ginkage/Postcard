package com.ginkage.postcard;

import android.app.Activity;
import android.os.Bundle;

public class PostcardActivity extends Activity {
	private PostcardSurfaceView mGLView = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mGLView = new PostcardSurfaceView(this);
		setContentView(mGLView);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mGLView != null) {
			mGLView.onPause();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mGLView != null) {
            mGLView.onResume();
        }
	}
}


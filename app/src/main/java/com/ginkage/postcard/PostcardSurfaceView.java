package com.ginkage.postcard;

import android.content.Context;
import android.opengl.GLSurfaceView;

class PostcardSurfaceView extends GLSurfaceView {
	public PostcardRenderer mRenderer = null;

	public PostcardSurfaceView(Context context) {
		super(context);

		// Create an OpenGL ES 2.0 context.
		setEGLContextClientVersion(2);

		// Set the Renderer for drawing on the GLSurfaceView
		mRenderer = new PostcardRenderer(context);
		setRenderer(mRenderer);
	}
}

package com.ginkage.postcard;

import android.content.Context;
import android.opengl.GLSurfaceView;

class PostcardSurfaceView extends GLSurfaceView {
	public PostcardSurfaceView(Context context) {
		super(context);
		setEGLContextClientVersion(2);
		setRenderer(new PostcardRenderer(context));
	}
}

package com.ginkage.postcard;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

public class PostcardRenderer implements Renderer {
	private final String vertexShaderCode =
		"precision mediump float;\n" +
		"uniform mat4 uMVPMatrix;\n" +
		"uniform vec4 uAmbient;\n" +
		"uniform vec4 uDiffuse;\n" +

		"const int MaxLights = 8;\n" +

		"struct LightSourceParameters {\n" +
		"	vec4 color;\n" +
		"	vec3 position;\n" +
		"};\n" +
		"uniform LightSourceParameters uLight[MaxLights];\n" +

		"attribute vec4 vPosition;\n" +
		"attribute vec3 vNormal;\n" +

		"varying vec4 FrontColor;\n" +

		"void main() {\n" +
		"	gl_Position = uMVPMatrix * vPosition;\n" +
		"	vec4 vcolor = uAmbient;\n" +

		"	int i;\n" +
		"	for (i = 0; i < MaxLights; i++) {\n" +
		"		vec3 vert2light = uLight[i].position - vPosition.xyz;\n" +
		"		vec3 ldir = normalize(vert2light);\n" +
		"		float NdotL = dot(vNormal, ldir);\n" +

		"		if (NdotL > 0.0) {\n" +
		"			vcolor += uLight[i].color * uDiffuse * NdotL;\n" +
		"		}\n" +
		"	}\n" +

		"	FrontColor = clamp(vcolor, 0.0, 1.0);\n" +
		"}\n";

	private final String fragmentShaderCode =
		"precision mediump float;\n" +
		"varying vec4 FrontColor;\n" +
		"void main() {\n" +
		"	gl_FragColor = FrontColor;\n" +
		"}\n";

	private int mProgram;
	private int maPosition;
	private int maNormal;
	private int muMVPMatrix;
	private int muAmbient;
	private int muDiffuse;
	private final int[] muLightPos = new int[8];
	private final int[] muLightCol = new int[8];

	private final String quadVS =
	   	"precision mediump float;\n" +
		"attribute vec4 vPosition;\n" +
		"attribute vec4 vTexCoord0;\n" +
		"varying vec4 TexCoord0;\n" +
		"void main() {\n" +
		"	gl_Position = vPosition;\n" +
		"	TexCoord0 = vTexCoord0;\n" +
		"}\n";

	private final String quadFS =
		"precision mediump float;\n" +
		"uniform sampler2D uTexture0;\n" +
		"varying vec4 TexCoord0;\n" +
		"void main() {\n" +
		"	gl_FragColor = texture2D(uTexture0, TexCoord0.xy);\n" +
		"}\n";

	private int mQProgram;
	private int maQPosition;
	private int maQTexCoord;
	private int muQTexture;

	private final String gaussVS =
	   	"precision mediump float;\n" +
		"attribute vec4 vPosition;\n" +
		"attribute vec4 vTexCoord0;\n" +
		"uniform vec4 uTexOffset0;\n" +
		"uniform vec4 uTexOffset1;\n" +
		"uniform vec4 uTexOffset2;\n" +
		"uniform vec4 uTexOffset3;\n" +
		"varying vec4 TexCoord0;\n" +
		"varying vec4 TexCoord1;\n" +
		"varying vec4 TexCoord2;\n" +
		"varying vec4 TexCoord3;\n" +
		"void main() {\n" +
		"	gl_Position = vPosition;\n" +
		"	TexCoord0 = vTexCoord0 + uTexOffset0;\n" +
		"	TexCoord1 = vTexCoord0 + uTexOffset1;\n" +
		"	TexCoord2 = vTexCoord0 + uTexOffset2;\n" +
		"	TexCoord3 = vTexCoord0 + uTexOffset3;\n" +
		"}\n";

	private final String gaussFS =
	   	"precision mediump float;\n" +
		"uniform sampler2D uTexture0;\n" +
		"uniform vec4 uTexCoef0;\n" +
		"uniform vec4 uTexCoef1;\n" +
		"uniform vec4 uTexCoef2;\n" +
		"uniform vec4 uTexCoef3;\n" +
		"varying vec4 TexCoord0;\n" +
		"varying vec4 TexCoord1;\n" +
		"varying vec4 TexCoord2;\n" +
		"varying vec4 TexCoord3;\n" +
		"void main() {\n" +
		"	vec4 c0 = texture2D(uTexture0, TexCoord0.xy);\n" +
		"	vec4 c1 = texture2D(uTexture0, TexCoord1.xy);\n" +
		"	vec4 c2 = texture2D(uTexture0, TexCoord2.xy);\n" +
		"	vec4 c3 = texture2D(uTexture0, TexCoord3.xy);\n" +
		"	gl_FragColor = uTexCoef0 * c0 + uTexCoef1 * c1 + uTexCoef2 * c2 + uTexCoef3 * c3;\n" +
		"}\n";

	private int mGProgram;
	private int maGPosition;
	private int maGTexCoord;
	private int muGTexture;
	private final int[] muGTexCoef = new int[4];
	private final int[] muGTexOffset = new int[4];

	private final String particleVS =
	   	"precision mediump float;\n" +
		"attribute vec4 vPosition;\n" +
		"attribute float vSizeShift;\n" +

		"uniform float uPointSize;\n" +
		"uniform float uTime;\n" +
		"uniform vec4 uColor;\n" +

		"varying vec4 Color;\n" +

		"void main() {\n" +
		"	float Phase = abs(fract(uTime + vSizeShift) * 2.0 - 1.0);\n" +
		"	vec4 pColor = uColor;\n" +
		"	if (Phase > 0.75) {\n" +
		"		pColor.z = (Phase - 0.75) * 4.0;\n" +
		"	};\n" +
		"	Color = pColor;\n" +
		"	gl_PointSize = uPointSize * Phase;\n" +
		"	gl_Position = vPosition;\n" +
		"}\n";

	private final String particleFS =
	   	"precision mediump float;\n" +
		"uniform sampler2D uTexture0;\n" +
		"varying vec4 Color;\n" +

		"void main()\n" +
		"{\n" +
		"	gl_FragColor = texture2D(uTexture0, gl_PointCoord) * Color;\n" +
		"}\n";

	private int mPProgram;
	private int maPPosition;
	private int maPSizeShift;
	private int muPPointSize;
	private int muPTime;
	private int muPTexture;
	private int muPColor;

	private final int mParticles = 1500;
	private int glParticleVB;

	private final float[] mMVMatrix = new float[16];
	private final float[] mMVPMatrix = new float[16];
	private final float[] mVMatrix = new float[16];
	private final float[] mProjMatrix = new float[16];
	private final float[] mCenter = new float[3];
	private float mDist;
	private float ratio = 1;
	private final float[] mAmbient = new float[4];
	private final float[] mDiffuse = new float[4];
	private final float[] mSpecular = new float[4];

	private int filterBuf1;
	private int filterBuf2;
	private int sceneBuf;
	private int renderTex1;
	private int renderTex2;
	private int sceneTex;

	private int particleTex;

	private final float[] mOffsets = new float[4];
	private final float[] pix_mult = new float[4];
	private final FilterKernelElement[] mvGaussian1D = new FilterKernelElement[44];

	private float mfPerTexelWidth;
	private float mfPerTexelHeight;

	private int glQuadVB;

	private int scrWidth;
	private int scrHeight;

	private int texWidth;
	private int texHeight;

	private final int[] genbuf = new int[1];

	private float mAngle;

	private static Scene3D scene = null;

	public float fps = 0;
	private long start_frame;
	private long frames_drawn;
	public boolean showText = false;

	private float maxPointSize = 0;
	private float curPointSize = 0;
	private long prevTime;

	private final Context mContext;

	public PostcardRenderer(Context context)
	{
		super();
		mContext = context;
	}

	private int createBuffer(float[] buffer)
	{
		FloatBuffer floatBuf = ByteBuffer.allocateDirect(buffer.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		floatBuf.put(buffer);
		floatBuf.position(0);

		GLES20.glGenBuffers(1, genbuf, 0);
		int glBuf = genbuf[0];
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, glBuf);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, buffer.length * 4, floatBuf, GLES20.GL_STATIC_DRAW);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

		return glBuf;
	}

	private void initShapes()
	{
		mAmbient[0] = 0.587f;
		mAmbient[1] = 0.587f;
		mAmbient[2] = 0.587f;
		mAmbient[3] = 1.0f;
		mDiffuse[0] = 0.587f;
		mDiffuse[1] = 0.587f;
		mDiffuse[2] = 0.587f;
		mDiffuse[3] = 1.0f;
		mSpecular[0] = 0.896f;
		mSpecular[1] = 0.896f;
		mSpecular[2] = 0.896f;
		mSpecular[3] = 1.0f;

		if (scene == null) {
            try {
                AssetManager am = mContext.getAssets();
                InputStream rose = am.open("rose.3ds");
                scene = (new Load3DS()).Load(rose);
                rose.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            assert scene != null;
            int i, num = scene.lights.size();
			for (i = 0; i < num; i++) {
				Light3D light = scene.lights.get(i);
				light.color[0] /= 4.5;
				light.color[1] /= 4.5;
				light.color[2] /= 4.5;
			}
		}

		float[] minpos = new float[3];
		float[] maxpos = new float[3];
		minpos[0] = minpos[1] = minpos[2] = Float.MAX_VALUE;
		maxpos[0] = maxpos[1] = maxpos[2] = -Float.MAX_VALUE;

		int i, num = scene.objects.size();
		for (i = 0; i < num; i++) {
			Object3D obj = scene.objects.get(i);
			int j, verts = obj.vertCount;
			for (j = 0; j < verts; j++) {
				float x = obj.vertexBuffer[j*8 + 0];
				float y = obj.vertexBuffer[j*8 + 1];
				float z = obj.vertexBuffer[j*8 + 2];
				if (minpos[0] > x) minpos[0] = x;
				if (minpos[1] > y) minpos[1] = y;
				if (minpos[2] > z) minpos[2] = z;
				if (maxpos[0] < x) maxpos[0] = x;
				if (maxpos[1] < y) maxpos[1] = y;
				if (maxpos[2] < z) maxpos[2] = z;
			}

			obj.glVertices = createBuffer(obj.vertexBuffer);

			GLES20.glGenBuffers(1, genbuf, 0);
			obj.glIndices = genbuf[0];
			GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, obj.glIndices);
			GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, obj.indCount * 2, null, GLES20.GL_STATIC_DRAW);

			int k, mats = obj.faceMats.size();
		   	for (k = 0; k < mats; k++) {
		   		FaceMat mat = obj.faceMats.get(k);
		   		ShortBuffer indBuf = ByteBuffer.allocateDirect(mat.indexBuffer.length * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
				indBuf.put(mat.indexBuffer);
				indBuf.position(0);

				GLES20.glBufferSubData(GLES20.GL_ELEMENT_ARRAY_BUFFER, mat.bufOffset * 2, mat.indexBuffer.length * 2, indBuf);
		   	}
			GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
		}

		mCenter[0] = (minpos[0] + maxpos[0]) / 2;
		mCenter[1] = (minpos[1] + maxpos[1]) / 2;
		mCenter[2] = (minpos[2] + maxpos[2]) / 2;
		mDist = max(max(maxpos[0] - minpos[0], maxpos[1] - minpos[1]), maxpos[2] - minpos[2]);
		mCenter[1] += mDist / 4;

		final float quadv[] = {
			-1,  1, 0, 0, 1,
			-1, -1, 0, 0, 0,
			 1,  1, 0, 1, 1,
			 1, -1, 0, 1, 0
		};

		glQuadVB = createBuffer(quadv);
	}

	private static float max(float f, float g)
	{
		return (f > g ? f : g);
	}

	private static int min(int size, int i)
	{
		return (size < i ? size : i);
	}

	private void DrawGauss(boolean invert)
	{
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

		GLES20.glUseProgram(mGProgram);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, glQuadVB);
		GLES20.glEnableVertexAttribArray(maGPosition);
		GLES20.glVertexAttribPointer(maGPosition, 3, GLES20.GL_FLOAT, false, 20, 0);
		GLES20.glEnableVertexAttribArray(maGTexCoord);
		GLES20.glVertexAttribPointer(maGTexCoord, 2, GLES20.GL_FLOAT, false, 20, 12);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

		GLES20.glUniform1i(muGTexture, 0);

		int i, n, k;
		for (i = 0; i < mvGaussian1D.length; i += 4) {
			for (n = 0; n < 4; n++) {
					FilterKernelElement pE = mvGaussian1D[i + n];

					for (k = 0; k < 4; k++)
						pix_mult[k] = pE.coef * 0.10f;
					GLES20.glUniform4fv(muGTexCoef[n], 1, pix_mult, 0);

					mOffsets[0] = mfPerTexelWidth * (invert ? pE.dv : pE.du);
					mOffsets[1] = mfPerTexelHeight * (invert ? pE.du : pE.dv);
					GLES20.glUniform4fv(muGTexOffset[n], 1, mOffsets, 0);
			}

			GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
		}

		GLES20.glDisableVertexAttribArray(maGPosition);
		GLES20.glDisableVertexAttribArray(maGTexCoord);
	}

	private void DrawQuad()
	{
		GLES20.glUseProgram(mQProgram);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, glQuadVB);
		GLES20.glEnableVertexAttribArray(maQPosition);
		GLES20.glVertexAttribPointer(maQPosition, 3, GLES20.GL_FLOAT, false, 20, 0);
		GLES20.glEnableVertexAttribArray(maQTexCoord);
		GLES20.glVertexAttribPointer(maQTexCoord, 2, GLES20.GL_FLOAT, false, 20, 12);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

		GLES20.glUniform1i(muQTexture, 0);

		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

		GLES20.glDisableVertexAttribArray(maQPosition);
		GLES20.glDisableVertexAttribArray(maQTexCoord);
	}

	private void DrawText()
	{
		GLES20.glUseProgram(mPProgram);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, glParticleVB);
		GLES20.glEnableVertexAttribArray(maPPosition);
		GLES20.glVertexAttribPointer(maPPosition, 2, GLES20.GL_FLOAT, false, 12, 0);
		GLES20.glEnableVertexAttribArray(maPSizeShift);
		GLES20.glVertexAttribPointer(maPSizeShift, 1, GLES20.GL_FLOAT, false, 12, 8);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

		GLES20.glUniform1f(muPPointSize, curPointSize);
		GLES20.glUniform4f(muPColor, 1, 1, 0, 1);
		GLES20.glUniform1i(muPTexture, 0);
		GLES20.glUniform1f(muPTime, (SystemClock.uptimeMillis() % 1000) / 1000.0f);

		GLES20.glDrawArrays(GLES20.GL_POINTS, 0, mParticles);

		GLES20.glDisableVertexAttribArray(maPPosition);
		GLES20.glDisableVertexAttribArray(maPSizeShift);
	}

	private void DrawScene()
	{
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

		GLES20.glUseProgram(mProgram);
		GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);

		Matrix.setLookAtM(mVMatrix, 0, mCenter[0], mCenter[1], mCenter[2] - mDist*0.5f, mCenter[0], mCenter[1], mCenter[2], 0f, 1.0f, 0.0f);
		Matrix.translateM(mVMatrix, 0, mCenter[0], mCenter[1], mCenter[2]);
		Matrix.rotateM(mVMatrix, 0, -45, 1, 0, 0);
		Matrix.rotateM(mVMatrix, 0, mAngle, 0, 1, 0);
		Matrix.translateM(mVMatrix, 0, -mCenter[0], -mCenter[1], -mCenter[2]);

		int i, j, k, num;
		num = min(scene.lights.size(), 8);

		for (i = 0; i < num; i++) {
			Light3D light = scene.lights.get(i);
			GLES20.glUniform3fv(muLightPos[i], 1, light.pos, 0);
			GLES20.glUniform4fv(muLightCol[i], 1, light.color, 0);
		}
		
		// Prepare the triangle data
		GLES20.glEnableVertexAttribArray(maPosition);
		GLES20.glEnableVertexAttribArray(maNormal);

		num = scene.animations.size();
		for (i = 0; i < num; i++) {
			Animation anim = scene.animations.get(i);
			Object3D obj = anim.object;
			if (obj == null) continue;

			Matrix.multiplyMM(mMVMatrix, 0, mVMatrix, 0, anim.world, 0);
			Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVMatrix, 0);

			// Apply a ModelView Projection transformation
			GLES20.glUniformMatrix4fv(muMVPMatrix, 1, false, mMVPMatrix, 0);

			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, obj.glVertices);
			GLES20.glVertexAttribPointer(maPosition, 3, GLES20.GL_FLOAT, false, 32, 0);
			GLES20.glVertexAttribPointer(maNormal, 3, GLES20.GL_FLOAT, false, 32, 12);
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

			GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, obj.glIndices);

			int mats = obj.faceMats.size();
			for (j = 0; j < mats; j++) {
				FaceMat mat = obj.faceMats.get(j);

				if (mat.material != null) {
					if (mat.material.ambient != null && scene.ambient != null) {
						for (k = 0; k < 3; k++)
							mAmbient[k] = mat.material.ambient[k] * scene.ambient[k];
						GLES20.glUniform4fv(muAmbient, 1, mAmbient, 0);
					}
					else
						GLES20.glUniform4f(muAmbient, 0, 0, 0, 1);

					if (mat.material.diffuse != null)
						GLES20.glUniform4fv(muDiffuse, 1, mat.material.diffuse, 0);
					else
						GLES20.glUniform4fv(muDiffuse, 1, mDiffuse, 0);
				}
				else {
					GLES20.glUniform4f(muAmbient, 0, 0, 0, 1);
					GLES20.glUniform4fv(muDiffuse, 1, mDiffuse, 0);
				}

				GLES20.glDrawElements(GLES20.GL_TRIANGLES, mat.indexBuffer.length, GLES20.GL_UNSIGNED_SHORT, mat.bufOffset * 2);
			}

			GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
		}

		GLES20.glDisableVertexAttribArray(maPosition);
		GLES20.glDisableVertexAttribArray(maNormal);
	}

	private void setRenderTexture(int frameBuf, int texture)
	{
		if (frameBuf == 0 || frameBuf == sceneBuf)
			GLES20.glViewport(0, 0, scrWidth, scrHeight);
		else
			GLES20.glViewport(0, 0, texWidth, texHeight);

		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuf);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
	}

	@Override
	public void onDrawFrame(GL10 arg0)
	{
		long curTime = SystemClock.uptimeMillis();
		long time = curTime % 4000L;
		mAngle = 0.090f * ((int) time);

		if (curTime > start_frame + 1000) {
			fps = frames_drawn * 1000.0f / (curTime - start_frame);
			start_frame = curTime;
			frames_drawn = 0;
		}

		if (showText && curPointSize < maxPointSize) {
			// fade in
			double delta = (curTime - prevTime) / 1000.0;
			curPointSize += maxPointSize * delta;
			if (curPointSize > maxPointSize)
				curPointSize = maxPointSize;
		}
		if (!showText && curPointSize > 0) {
			// fade in
			double delta = (curTime - prevTime) / 1000.0;
			curPointSize -= maxPointSize * delta;
			if (curPointSize < 0)
				curPointSize = 0;
		}

		prevTime = curTime;

		if (showText) {
			setRenderTexture(sceneBuf, 0);
			DrawScene();
	
			setRenderTexture(filterBuf1, sceneTex);
			DrawQuad();
	
			GLES20.glEnable(GLES20.GL_BLEND);
			GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE);
	
			setRenderTexture(filterBuf2, renderTex1);
			DrawGauss(false);
	
			setRenderTexture(filterBuf1, renderTex2);
			DrawGauss(true);
	
			GLES20.glDisable(GLES20.GL_BLEND);
	
			setRenderTexture(0, sceneTex);
			DrawQuad();
		}
		else {
			setRenderTexture(filterBuf1, 0);
			DrawScene();

			GLES20.glEnable(GLES20.GL_BLEND);
			GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE);

			setRenderTexture(filterBuf2, renderTex1);
			DrawGauss(false);

			setRenderTexture(filterBuf1, renderTex2);
			DrawGauss(true);

			GLES20.glDisable(GLES20.GL_BLEND);

			setRenderTexture(0, 0);
			DrawScene();
		}

		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE);

		setRenderTexture(0, renderTex1);
		DrawQuad();

/*		if (curPointSize > 0) {
			setRenderTexture(0, particleTex);
			DrawText();
		}
*/
		GLES20.glDisable(GLES20.GL_BLEND);

		frames_drawn++;
	}

	private int makeRenderTarget(int width, int height, int[] handles)
	{
		GLES20.glGenTextures(1, genbuf, 0);
		int renderTex = genbuf[0];
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, renderTex);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);

		IntBuffer texBuffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
		GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, texBuffer);

		GLES20.glGenRenderbuffers(1, genbuf, 0);
		int depthBuf = genbuf[0];
		GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, depthBuf);
		GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, width, height);

		GLES20.glGenFramebuffers(1, genbuf, 0);
		int frameBuf = genbuf[0];
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuf);
		GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, renderTex, 0);
		GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, depthBuf);

		int res = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);

		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

		handles[0] = frameBuf;
		handles[1] = renderTex;

		return res;
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height)
	{
		ratio = (float) width / height;
		Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 1, 1000);

		int[] handles = new int[2];

		scrWidth = width;
		scrHeight = height;

		texWidth = 256;
		texHeight = 256;

		makeRenderTarget(texWidth, texHeight, handles);
		filterBuf1 = handles[0];
		renderTex1 = handles[1];

		makeRenderTarget(texWidth, texHeight, handles);
		filterBuf2 = handles[0];
		renderTex2 = handles[1];

		makeRenderTarget(scrWidth, scrHeight, handles);
		sceneBuf = handles[0];
		sceneTex = handles[1];

		float cent = (mvGaussian1D.length - 1.0f) / 2.0f, radi;
		for (int u = 0; u < mvGaussian1D.length; u++)
		{
			FilterKernelElement el = mvGaussian1D[u] = new FilterKernelElement();
			el.du = ((float)u) - cent - 0.1f;
			el.dv = 0.0f;
			radi = (el.du * el.du) / (cent * cent);
			el.coef = (float)((0.24/Math.exp(radi*0.18)) + 0.41/Math.exp(radi*4.5));
		}

		float rr = texWidth / (float) texHeight;
		float rs = rr / ratio;

		mfPerTexelWidth = rs / texWidth;
		mfPerTexelHeight = 1.0f / texHeight;

		initParticles();

		start_frame = SystemClock.uptimeMillis();
		frames_drawn = 0;
		fps = 0;
	}

	private int loadShader(int type, String shaderCode)
	{
		int shader = GLES20.glCreateShader(type);
		GLES20.glShaderSource(shader, shaderCode);
		GLES20.glCompileShader(shader);
		Log.e("Shader", GLES20.glGetShaderInfoLog(shader));
		return shader;
	}

	private int Compile(String vs, String fs)
	{
		int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vs);
		int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fs);

		int prog = GLES20.glCreateProgram();			 // create empty OpenGL Program
		GLES20.glAttachShader(prog, vertexShader);   // add the vertex shader to program
		GLES20.glAttachShader(prog, fragmentShader); // add the fragment shader to program
		GLES20.glLinkProgram(prog);				  // creates OpenGL program executables

		return prog;
	}

	private static int loadTexture(final Context context, final int resourceId)
	{
		final int[] textureHandle = new int[1];

		GLES20.glGenTextures(1, textureHandle, 0);
		if (textureHandle[0] != 0)
		{
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inScaled = false;   // No pre-scaling
			// Read in the resource
			final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);

			// Bind to the texture in OpenGL
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
			// Set filtering
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

			// Load the bitmap into the bound texture.
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

			// Recycle the bitmap, since its data has been loaded into OpenGL.
			bitmap.recycle();
		}
	 
		return textureHandle[0];
	}

	private void initParticles()
	{
		int width = scrWidth, height = scrHeight, fontSize = (int) (scrHeight / 8.59); // 7.84);

		// Create an empty, mutable bitmap
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
		// get a canvas to paint over the bitmap
		Canvas canvas = new Canvas(bitmap);
		bitmap.eraseColor(Color.BLACK);

		// Draw the text
		Paint textPaint = new Paint();
		textPaint.setTextSize(fontSize);
		textPaint.setAntiAlias(false);
		textPaint.setARGB(0xff, 0xff, 0xff, 0xff);
		textPaint.setTextAlign(Paint.Align.CENTER);
		textPaint.setTypeface(Typeface.SANS_SERIF);

		int fontHeight = (fontSize * 3) / 4;
		int pad = fontHeight / 2;
		int gap = (height - fontHeight * 3 - pad * 2) / 2;
		int hc = width / 2;

		// draw the text centered
		canvas.drawText("С Днём", hc, pad + fontHeight, textPaint);
		canvas.drawText("Рождения,", hc, pad + fontHeight * 2 + gap, textPaint);
		canvas.drawText("Настенька!", hc, pad + fontHeight * 3 + gap * 2, textPaint);

		int[] pixels = new int[width * height];
		bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
		bitmap.recycle();

		int colored = 0;
		float[] cx = new float[width * height];
		float[] cy = new float[width * height];

		for (int y = 0, idx = 0; y < height; y++)
			for (int x = 0; x < width; x++)
				if ((pixels[idx++] & 0xffffff) != 0) {
					cx[colored] = x / (float)width;
					cy[colored] = y / (float)height;
					colored++;
				}

		float[] particleBuf = new float[3 * mParticles];
		for (int i = 0, idx = 0; i < mParticles; i++, idx += 3) {
			int n = (int) (Math.random() * colored);
			particleBuf[idx + 0] = cx[n] * 2 - 1;
			particleBuf[idx + 1] = 1 - cy[n] * 2;
			particleBuf[idx + 2] = (float) Math.random();
		}

		curPointSize = 0;
		maxPointSize = scrHeight / 69.0f;
		prevTime = SystemClock.uptimeMillis();

		glParticleVB = createBuffer(particleBuf);

		mPProgram = Compile(particleVS, particleFS);
		maPPosition = GLES20.glGetAttribLocation(mPProgram, "vPosition");
		maPSizeShift = GLES20.glGetAttribLocation(mPProgram, "vSizeShift");
		muPPointSize = GLES20.glGetUniformLocation(mPProgram, "uPointSize");
		muPTime = GLES20.glGetUniformLocation(mPProgram, "uTime");
		muPTexture = GLES20.glGetUniformLocation(mPProgram, "uTexture0");
		muPColor = GLES20.glGetUniformLocation(mPProgram, "uColor");

		particleTex = loadTexture(mContext, R.drawable.particle);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config)
	{
		// Set the background frame color
		GLES20.glClearColor(0, 0, 0, 1);

		mProgram = Compile(vertexShaderCode, fragmentShaderCode);

		// get handle to the vertex shader's vPosition member
		maPosition = GLES20.glGetAttribLocation(mProgram, "vPosition");
		maNormal = GLES20.glGetAttribLocation(mProgram, "vNormal");
		muMVPMatrix = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
		muAmbient = GLES20.glGetUniformLocation(mProgram, "uAmbient");
		muDiffuse = GLES20.glGetUniformLocation(mProgram, "uDiffuse");

		int i;
		for (i = 0; i < 8; i++) {
			muLightPos[i] = GLES20.glGetUniformLocation(mProgram, String.format("uLight[%d].position", i));
			muLightCol[i] = GLES20.glGetUniformLocation(mProgram, String.format("uLight[%d].color", i));
		}

		mQProgram = Compile(quadVS, quadFS);
		maQPosition = GLES20.glGetAttribLocation(mQProgram, "vPosition");
		maQTexCoord = GLES20.glGetAttribLocation(mQProgram, "vTexCoord0");
		muQTexture = GLES20.glGetUniformLocation(mQProgram, "uTexture0");

		mGProgram = Compile(gaussVS, gaussFS);
		maGPosition = GLES20.glGetAttribLocation(mGProgram, "vPosition");
		maGTexCoord = GLES20.glGetAttribLocation(mGProgram, "vTexCoord0");
		muGTexture = GLES20.glGetUniformLocation(mGProgram, "uTexture0");

		for (i = 0; i < 4; i++) {
			muGTexOffset[i] = GLES20.glGetUniformLocation(mGProgram, String.format("uTexOffset%d", i));
			muGTexCoef[i] = GLES20.glGetUniformLocation(mGProgram, String.format("uTexCoef%d", i));
		}

		initShapes();
	}
}

class FilterKernelElement
{
	public float du;
	public float dv;
	public float coef;
}

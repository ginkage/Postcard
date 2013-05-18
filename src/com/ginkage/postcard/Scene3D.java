package com.ginkage.postcard;

import java.util.ArrayList;

import android.opengl.Matrix;

public class Scene3D {
	public ArrayList<Material3D> materials;
	public ArrayList<Object3D> objects;
	public ArrayList<Light3D> lights;
	public ArrayList<Animation> animations;
	public float[] background;
	public float[] ambient;

	public Material3D FindMaterial(String name)
	{
		if (materials == null || name == null) return null;
		int i, n = materials.size();
		for (i = 0; i < n; i++) {
			Material3D mat = materials.get(i);
			if (mat.name.equals(name))
				return mat;
		}
		return null;
	}

	public Object3D FindObject(String name)
	{
		if (objects == null || name == null) return null;
		int i, n = objects.size();
		for (i = 0; i < n; i++) {
			Object3D obj = objects.get(i);
			if (obj.name.equals(name))
				return obj;
		}
		return null;
	}

	public Light3D FindLight(String name)
	{
		if (lights == null || name == null) return null;
		int i, n = lights.size();
		for (i = 0; i < n; i++) {
			Light3D light = lights.get(i);
			if (light.name.equals(name))
				return light;
		}
		return null;
	}

	public Animation FindAnimation(int id)
	{
		if (animations == null || id == 0xffff) return null;
		int i, n = animations.size();
		for (i = 0; i < n; i++) {
			Animation anim = animations.get(i);
			if (anim.id == id)
				return anim;
		}
		return null;
	}

	private void lerp3(float[] out, float[] from, float[] to, float t)
	{
		for (int i = 0; i < 3; i++)
			out[i] = from[i] + (to[i] - from[i]) * t;
	}

	private AnimKey findVec(AnimKey[] keys, float time)
	{
		AnimKey key = keys[keys.length - 1];

		// We'll use either first, or last, or interpolated key
		for (int j = 0; j < keys.length; j++) {
			if (keys[j].time >= time) {
				if (j > 0) {
					float local = (time - keys[j - 1].time) /
						(keys[j].time - keys[j - 1].time);
					key = new AnimKey();
					key.time = time;
					key.data = new float[3];
					lerp3(key.data, keys[j - 1].data, keys[j].data, local);
				}
				else
					key = keys[j];
				break;
			}
		}

		return key;
	}

	private void applyRot(float[] result, float[] data, float t)
	{
		if (Math.abs(data[3]) > 1.0e-7 && Math.hypot(Math.hypot(data[0], data[1]), data[2]) > 1.0e-7)
			Matrix.rotateM(result, 0, (float) (data[3] * t * 180 / Math.PI), data[0], data[1], data[2]);
	}

	public void Compute(float time)
	{
		int i, n = animations.size();
		for (i = 0; i < n; i++) {
			Animation anim = animations.get(i);
			Object3D obj = anim.object;
			float[] result = new float[16];

			Matrix.setIdentityM(result, 0);

			if (anim.position != null && anim.position.length > 0) {
				AnimKey key = findVec(anim.position, time);
				float[] pos = key.data;
				Matrix.translateM(result, 0, pos[0], pos[1], pos[2]);
			}

			if (anim.rotation != null && anim.rotation.length > 0) {
				// All rotations that are prior to the target time should be applied sequentially
				for (int j = anim.rotation.length - 1; j > 0; j--) {
					if (time >= anim.rotation[j].time) // rotation in the past, apply as is
						applyRot(result, anim.rotation[j].data, 1);
					else if (time > anim.rotation[j - 1].time) {
						// rotation between key frames, apply part of it
						float local = (time - anim.rotation[j - 1].time) /
								(anim.rotation[j].time - anim.rotation[j - 1].time);
						applyRot(result, anim.rotation[j].data, local);
					}
					// otherwise, it's a rotation in the future, skip it
				}

				// Always apply the first rotation
				applyRot(result, anim.rotation[0].data, 1);
			}

			if (anim.scaling != null && anim.scaling.length > 0) {
				AnimKey key = findVec(anim.scaling, time);
				float[] scale = key.data;
				Matrix.scaleM(result, 0, scale[0], scale[1], scale[2]);
			}

			if (anim.parent != null)
				Matrix.multiplyMM(anim.result, 0, anim.parent.result, 0, result, 0);
			else
				Matrix.translateM(anim.result, 0, result, 0, 0, 0, 0);

			if (obj != null && obj.trMatrix != null) {
				float[] pivot = new float[16];
				Matrix.setIdentityM(pivot, 0);
				Matrix.translateM(pivot, 0, -anim.pivot[0], -anim.pivot[1], -anim.pivot[2]);
				Matrix.multiplyMM(result, 0, pivot, 0, obj.trMatrix, 0);
			}
			else {
				Matrix.setIdentityM(result, 0);
				Matrix.translateM(result, 0, -anim.pivot[0], -anim.pivot[1], -anim.pivot[2]);
			}
			Matrix.multiplyMM(anim.world, 0, anim.result, 0, result, 0);
		}
	}
}

class Object3D {
	public String name;
	public int vertCount;
	public int indCount;
	public ArrayList<FaceMat> faceMats;
	public int glVertices;
	public int glIndices;
	public float[] vertexBuffer;
	public float[] trMatrix;
}

class FaceMat {
	public Material3D material;
	public short[] indexBuffer;
	public int indCount;
	public int bufOffset;
}

class Light3D {
	public String name;
	public float[] pos;
	public float[] color;
	public float[] dir;
	public float theta, phi;
}

class Material3D {
	public String name;
	public float[] ambient;
	public float[] diffuse;
	public float[] specular;
	public String texture;
	public float shininess;
	public float shinStren;
	public float transparency;
	public float selfIllum;
	public int type;
}

class Animation {
	public int id;
	public String name;
	public Object3D object;
	public Light3D light;
	public Animation parent;
	public float[] pivot;

	public AnimKey[] position;
	public AnimKey[] rotation;
	public AnimKey[] scaling;

	public float[] result;
	public float[] world;
}

class AnimKey {
	public float time;
	public float[] data;
}

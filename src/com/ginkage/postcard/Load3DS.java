package com.ginkage.postcard;

import java.io.*;
import java.util.ArrayList;

import android.opengl.Matrix;
import android.util.FloatMath;
import android.util.Log;

class Load3DS {
	private final int CHUNK_MAIN     = 0x4D4D;
	private final int CHUNK_OBJMESH  = 0x3D3D;
	private final int CHUNK_OBJBLOCK = 0x4000;
	private final int CHUNK_TRIMESH  = 0x4100;
	private final int CHUNK_VERTLIST = 0x4110;
	private final int CHUNK_FACELIST = 0x4120;
	private final int CHUNK_FACEMAT  = 0x4130;
	private final int CHUNK_MAPLIST  = 0x4140;
	private final int CHUNK_SMOOTHG  = 0x4150;
	private final int CHUNK_TRMATRIX = 0x4160;
	private final int CHUNK_LIGHT    = 0x4600;
	private final int CHUNK_SPOTL    = 0x4610;
	private final int CHUNK_ONOFF    = 0x4620;
	private final int CHUNK_CAMERA   = 0x4700;
	private final int CHUNK_RGBC     = 0x0010;
	private final int CHUNK_RGB24    = 0x0011;
	private final int CHUNK_SHORT    = 0x0030;
	private final int CHUNK_BACKCOL  = 0x1200;
	private final int CHUNK_AMB      = 0x2100;
	private final int CHUNK_MATERIAL = 0xAFFF;
	private final int CHUNK_MATNAME  = 0xA000;
	private final int CHUNK_AMBIENT  = 0xA010;
	private final int CHUNK_DIFFUSE  = 0xA020;
	private final int CHUNK_SPECULAR = 0xA030;
	private final int CHUNK_SHININES = 0xA040;
	private final int CHUNK_SHINSTRN = 0xA041;
	private final int CHUNK_TRANSP   = 0xA050;
	private final int CHUNK_SELFILL  = 0xA084;
	private final int CHUNK_MTLTYPE  = 0xA100;
	private final int CHUNK_TEXTURE  = 0xA200;
	private final int CHUNK_REFLMAP  = 0xA220;
	private final int CHUNK_BUMPMAP  = 0xA230;
	private final int CHUNK_MAPFILE  = 0xA300;
	private final int CHUNK_MAPPARAM = 0xA351;
	private final int CHUNK_KEYFRAMER = 0xB000;
	private final int CHUNK_TRACKINFO = 0xB002;
	private final int CHUNK_SPOTINFO  = 0xB007;
	private final int CHUNK_FRAMES    = 0xB008;
	private final int CHUNK_OBJNAME   = 0xB010;
	private final int CHUNK_PIVOT     = 0xB013;
	private final int CHUNK_TRACKPOS  = 0xB020;
	private final int CHUNK_TRACKROT  = 0xB021;
	private final int CHUNK_TRACKSCL  = 0xB022;
	private final int CHUNK_HIERARCHY = 0xB030;

	private BufferedInputStream file;
	private final byte[] bytes = new byte[8];
	private long filePos;

	public Scene3D Load(InputStream stream)
	{
		file = null;
		Scene3D scene = null; 
//		File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
//		File fil = new File(dir.getAbsolutePath() + File.separator + fileName);
//		if (!fil.exists()) return scene;

		try {
			filePos = 0;
			file = new BufferedInputStream(stream);//new FileInputStream(fil));
			scene = ProcessFile(stream.available());//fil.length());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			if (file != null)
				file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return scene;
	}

	private void Skip(long count) throws IOException
	{
		file.skip(count);
		filePos += count;
	}

	private void Seek(long end) throws IOException
	{
		if (filePos < end) {
			Skip(end - filePos);
			filePos = end;
		}
	}

	private byte ReadByte() throws IOException
	{
		file.read(bytes, 0, 1);
		filePos++;
		return bytes[0];
	}

	private int ReadUnsignedByte() throws IOException
	{
		file.read(bytes, 0, 1);
		filePos++;
		return (bytes[0]&0xff);
	}

	private int ReadUnsignedShort() throws IOException
	{
		file.read(bytes, 0, 2);
		filePos += 2;
		return ((bytes[1]&0xff) << 8 | (bytes[0]&0xff));
	}

	private int ReadInt() throws IOException
	{
		file.read(bytes, 0, 4);
		filePos += 4;
		return (bytes[3]) << 24 | (bytes[2]&0xff) << 16 | (bytes[1]&0xff) <<  8 | (bytes[0]&0xff);
	}

	private float ReadFloat() throws IOException
	{
		return Float.intBitsToFloat(ReadInt());
	}

	private Scene3D ProcessFile(long fileLen) throws IOException
	{
		Scene3D scene = null;

		while (filePos < fileLen) {
			int chunkID = ReadUnsignedShort();
			int chunkLen = ReadInt() - 6;

			switch (chunkID) {
			case CHUNK_MAIN:
				if (scene == null)
					scene = ChunkMain(chunkLen);
				else
					Skip(chunkLen);
				break;

			default:
				Skip(chunkLen);
			}
		}

		return scene;
	}

	private Scene3D ChunkMain(int len) throws IOException
	{
		Scene3D scene = new Scene3D();
		scene.materials = new ArrayList<Material3D>();
		scene.objects = new ArrayList<Object3D>();
		scene.lights = new ArrayList<Light3D>();
		scene.animations = new ArrayList<Animation>();

		long end = filePos + len;
		while (filePos < end) {
			int chunkID = ReadUnsignedShort();
			int chunkLen = ReadInt() - 6;

			switch (chunkID) {
			case CHUNK_OBJMESH:
				Chunk3DEditor(scene, chunkLen);
				break;

			case CHUNK_KEYFRAMER:
				ChunkKeyframer(scene, chunkLen);
				break;

			case CHUNK_BACKCOL:
				scene.background = new float[4];
				ChunkColor(chunkLen, scene.background);
				break;

			case CHUNK_AMB:
				scene.ambient = new float[4];
				ChunkColor(chunkLen, scene.ambient);
				break;

			default:
				Skip(chunkLen);
			}
		}
		Seek(end);

		scene.Compute(0);

		return scene;
	}

	private void Chunk3DEditor(Scene3D scene, int len) throws IOException
	{
		long end = filePos + len;
		while (filePos < end) {
			int chunkID = ReadUnsignedShort();
			int chunkLen = ReadInt() - 6;

			switch (chunkID) {
			case CHUNK_OBJBLOCK:
				ChunkObject(scene, chunkLen);
				break;

			case CHUNK_MATERIAL:
				Material3D mat = ChunkMaterial(chunkLen);
				if (mat != null)
					scene.materials.add(mat);
				break;

			default:
				Skip(chunkLen);
			}
		}
		Seek(end);
	}

	private void ChunkObject(Scene3D scene, int len) throws IOException
	{
		long end = filePos + len;

		if (len == 0) return;
		String name = ChunkName(0);

		while (filePos < end) {
			int chunkID = ReadUnsignedShort();
			int chunkLen = ReadInt() - 6;

			switch (chunkID) {
			case CHUNK_TRIMESH:
				Object3D obj = ChunkTrimesh(chunkLen, name, scene);
				if (obj != null)
					scene.objects.add(obj);
				break;

			case CHUNK_LIGHT:
				Light3D light = ChunkLight(chunkLen, name);
				if (light != null)
					scene.lights.add(light);
				break;

			case CHUNK_CAMERA:
			default:
				Skip(chunkLen);
			}
		}
		Seek(end);
	}

	private Object3D ChunkTrimesh(int len, String name, Scene3D scene) throws IOException
	{
		long end = filePos + len;

		Object3D obj = new Object3D();
		obj.name = name;
		obj.faceMats = new ArrayList<FaceMat>();
		obj.indCount = 0;

		int i, k, num;

		while (filePos < end) {
			int chunkID = ReadUnsignedShort();
			int chunkLen = ReadInt() - 6;

			switch (chunkID) {
			case CHUNK_FACELIST:
				ChunkFaceList(chunkLen, obj, scene);
				break;

			case CHUNK_MAPLIST:
				num = ReadUnsignedShort();
				for (i = 0, k = 6; i < num; i++, k += 8) {
					obj.vertexBuffer[k + 0] = ReadFloat();
					obj.vertexBuffer[k + 1] = 1 - ReadFloat();
				}
				break;

			case CHUNK_VERTLIST:
				num = ReadUnsignedShort();
				obj.vertCount = num;
				obj.vertexBuffer = new float[8*num];
				for (i = 0, k = 0; i < num; i++, k += 8) {
					ChunkVector(obj.vertexBuffer, k);
					obj.vertexBuffer[k + 3] = 0;
					obj.vertexBuffer[k + 4] = 0;
					obj.vertexBuffer[k + 5] = 0;
					obj.vertexBuffer[k + 6] = 0;
					obj.vertexBuffer[k + 7] = 0;
				}
				break;

			case CHUNK_TRMATRIX:
				float[] localCoord = new float[16];
				ChunkVector(localCoord, 4*0);
				ChunkVector(localCoord, 4*2);
				ChunkVector(localCoord, 4*1);
				ChunkVector(localCoord, 4*3);
				localCoord[3] = localCoord[7] = localCoord[11] = 0;
				localCoord[15] = 1;

				obj.trMatrix = new float[16];
				Matrix.invertM(obj.trMatrix, 0, localCoord, 0);
				break;

			default:
				Skip(chunkLen);
			}
		}
		Seek(end);

		return obj;
	}

	private static void CrossProduct(float[] res, float[] v1, float[] v2)
	{
		res[0] = v1[1]*v2[2] - v1[2]*v2[1];
		res[1] = v1[2]*v2[0] - v1[0]*v2[2];
		res[2] = v1[0]*v2[1] - v1[1]*v2[0];
	}

	private static float DotSquare(float[] v, int offset)
	{
		return v[offset + 0]*v[offset + 0] + v[offset + 1]*v[offset + 1] + v[offset + 2]*v[offset + 2];
	}

	private static void VecSubstract(float[] res, float[] v, int offset1, int offset2)
	{
		res[0] = v[offset1 + 0] - v[offset2 + 0];
		res[1] = v[offset1 + 1] - v[offset2 + 1];
		res[2] = v[offset1 + 2] - v[offset2 + 2];
	}

	private static void VecAdd(float[] v, int offset, float[] a, int off)
	{
		v[offset + 0] += a[off + 0];
		v[offset + 1] += a[off + 1];
		v[offset + 2] += a[off + 2];
	}

	private void VecNormalize(float[] v, int offset)
	{
		double nlen = 1 / FloatMath.sqrt(DotSquare(v, offset));
		v[offset + 0] *= nlen;
		v[offset + 1] *= nlen;
		v[offset + 2] *= nlen;
	}

	private void ChunkFaceList(int len, Object3D obj, Scene3D scene) throws IOException
	{
		long end = filePos + len;

		int i, j, k, l, m, t, num = ReadUnsignedShort(), unused = num, idx;

		int faceCount = num;
		int[] faceBuffer = new int[3*faceCount];
		boolean[] faceUsed = new boolean[faceCount];
		float[] v = new float[3]; 
		float[] v1 = new float[3]; 
		float[] v2 = new float[3]; 

		float[] vgNorm = new float[3*3*faceCount]; // per-vertex normal for each face
		int[] vertGroup = new int[3*faceCount]; // per-vertex group for each face
		boolean[] vgUsed = new boolean[3*faceCount]; // per-vertex "group used" bit for each face
		int[] vgNum = new int[obj.vertCount + 1]; // per-vertex face count, and then offset
		int[] faceGroup = new int[faceCount]; // per-face smoothing group

		int[] vgUniqs = new int[obj.vertCount + 1]; // per-vertex unique groups count
		int[] vertUGroup = new int[3*faceCount]; // per-vertex unique groups list for each face

		for (i = 0; i <= obj.vertCount; i++)
			vgNum[i] = vgUniqs[i] = 0;

		for (i = 0, idx = 0; i < faceCount; i++, idx += 3) {
			j = ReadUnsignedShort();
			k = ReadUnsignedShort();
			l = ReadUnsignedShort();
			Skip(2);

			faceUsed[i] = false;
			faceBuffer[idx + 0] = j;
			faceBuffer[idx + 2] = k;
			faceBuffer[idx + 1] = l;

			// initialize smoothing groups data
			faceGroup[i] = 0;

			for (t = 0; t < 9; t++)
				vgNorm[i * 9 + t] = 0;

			for (t = 0; t < 3; t++) {
				vertGroup[idx + t] = 0;
				vertUGroup[idx + t] = 0;
				vgUsed[idx + t] = false;
			}

			vgNum[j]++;
			vgNum[k]++;
			vgNum[l]++;
		}

		int a, sum = 0;
		for (i = 0; i < obj.vertCount; i++) {
			a = vgNum[i];
			vgNum[i] = sum;
			sum += a;
		}
		vgNum[obj.vertCount] = sum; // now we can store all the faces and their normals and groups per-vertex

		for (i = 0, idx = 0; i < faceCount; i++, idx += 3) {
			j = faceBuffer[idx + 0];
			k = faceBuffer[idx + 2];
			l = faceBuffer[idx + 1];

			VecSubstract(v1, obj.vertexBuffer, l*8, j*8);
			VecSubstract(v2, obj.vertexBuffer, k*8, j*8);
			CrossProduct(v, v1, v2);

			VecAdd(vgNorm, vgNum[j]*3, v, 0);
			VecAdd(vgNorm, vgNum[k]*3, v, 0);
			VecAdd(vgNorm, vgNum[l]*3, v, 0);
			
			vgNum[j]++;
			vgNum[k]++;
			vgNum[l]++;
		}

		for (i = obj.vertCount - 1; i > 0; i--) // offsets were shifted, so shift them back
			vgNum[i] = vgNum[i - 1];
		vgNum[0] = 0;

		boolean gotSmoothGroups = false;

		while (filePos < end) {
			int chunkID = ReadUnsignedShort();
			int chunkLen = ReadInt() - 6;

			switch (chunkID) {
			case CHUNK_FACEMAT:
				FaceMat mat = new FaceMat();
				String name = ChunkName(0);
				mat.material = scene.FindMaterial(name);
				num = ReadUnsignedShort();
				mat.indCount = num;
				mat.indexBuffer = new short[3*num];
				mat.bufOffset = obj.indCount;
				obj.indCount += 3*num;
				k = 0;
				for (i = 0; i < num; i++) {
					j = ReadUnsignedShort();
					if (!faceUsed[j]) {
						faceUsed[j] = true;
						unused--;
					}
					for (t = 0; t < 3; t++)
						mat.indexBuffer[k++] = (short) j;
				}
				obj.faceMats.add(mat);
				break;

			case CHUNK_SMOOTHG:
				for (i = 0, idx = 0; i < faceCount; i++, idx += 3) {
					faceGroup[i] = ReadInt();
					
					for (t = 0; t < 3; t++) {
						j = faceBuffer[idx + t];
						vertGroup[vgNum[j]] = faceGroup[i];
						vgNum[j]++;
					}
				}
				for (i = obj.vertCount - 1; i > 0; i--)
					vgNum[i] = vgNum[i - 1];
				vgNum[0] = 0;
				gotSmoothGroups = true;
				break;

			default:
				Skip(chunkLen);
			}
		}
		Seek(end);

		int newVertCount = 0, g;

		if (gotSmoothGroups) {
			for (i = 0; i < obj.vertCount; i++) {
				for (m = vgNum[i]; m < vgNum[i + 1]; m++) { // for every normal and face of this vertex
					if (!vgUsed[m]) {
						// vertGroup[m] is a new group...
						vertUGroup[vgNum[i] + vgUniqs[i]] = vertGroup[m];
						vgUniqs[i]++;
						newVertCount++;
					}
	
					for (t = m; t < vgNum[i + 1]; t++) // mark all equal groups (including this one) as duplicates
						if (vertGroup[t] == vertGroup[m])
							vgUsed[t] = true;
				}
			}

			if (newVertCount == obj.vertCount)
				gotSmoothGroups = false;
		}

		if (gotSmoothGroups) {
			// reindex all vertices, build new normals
			int newIndex = 0;
			int[] vertIndex = new int[faceCount*3]; // new vertex indices
			float[] newVertexBuffer = new float[newVertCount*8];

			for (i = 0, idx = 0; i < faceCount; i++, idx += 3)
				for (t = 0; t < 3; t++)
					vertIndex[idx + t] = 0;

			idx = 3;
			for (i = 0; i < obj.vertCount; i++) {
				for (t = 0; t < vgUniqs[i]; t++) { // for every unique normal and face of this vertex
					for (m = 0; m < 8; m++)
						newVertexBuffer[newIndex*8 + m] = obj.vertexBuffer[i*8 + m]; // duplicate all vertex data (including zero normals) 

					g = vertUGroup[vgNum[i] + t]; // unique group mask for this vertex
					for (m = vgNum[i]; m < vgNum[i + 1]; m++) // for every NON-unique normal and face of this vertex
						if ((vertGroup[m] & g) != 0 || vertGroup[m] == g) // also works for zero group
							VecAdd(newVertexBuffer, idx, vgNorm, m*3); // add normal to vertex
	
					vertIndex[vgNum[i] + t] = newIndex;
					newIndex++;
					idx += 8;
				}
			}

			int fg, vi;
			for (i = 0, idx = 0; i < faceCount; i++, idx += 3) {
				fg = faceGroup[i];
				for (m = 0; m < 3; m++) {
					vi = faceBuffer[idx + m]; // face vertex
					for (t = 0; t < vgUniqs[vi]; t++) // for every unique group of this vertex
						if (fg == vertUGroup[vgNum[vi] + t]) { // found the right one
							faceBuffer[idx + m] = vertIndex[vgNum[vi] + t];
							break;
						}
				}
			}

			Log.i("Load3DS", String.format("Resized object %s from %d to %d vertices", obj.name, obj.vertCount, newVertCount));
			obj.vertCount = newVertCount;
			obj.vertexBuffer = newVertexBuffer;
		}
		else // nothing changed, no need to recalculate anything
			for (i = 0, idx = 3; i < obj.vertCount; i++, idx += 8) // just copy all the normals
				for (m = vgNum[i]; m < vgNum[i + 1]; m++) // for every NON-unique normal and face of this vertex
					VecAdd(obj.vertexBuffer, idx, vgNorm, m*3); // add normal to vertex

		for (i = 0, k = 3; i < obj.vertCount; i++, k += 8)
			VecNormalize(obj.vertexBuffer, k);

		for (m = 0; m < obj.faceMats.size(); m++) {
			FaceMat mat = obj.faceMats.get(m);
			k = 0;
			for (i = 0; i < mat.indCount; i++)
				for (t = 0; t < 3; t++) {
					j = 3 * (int) mat.indexBuffer[k];
					mat.indexBuffer[k++] = (short) faceBuffer[j + t];
				}
		}

		if (unused > 0) {
			FaceMat mat = new FaceMat();
			mat.indexBuffer = new short[3*unused];
			mat.bufOffset = obj.indCount;
			obj.indCount += 3*unused;
			k = 0;
			for (i = 0; i < faceCount; i++)
				if (!faceUsed[i]) {
					faceUsed[i] = true;
					j = i * 3;
					for (t = 0; t < 3; t++)
						mat.indexBuffer[k++] = (short) faceBuffer[j + t];
				}
			obj.faceMats.add(mat);
		}
	}

	private Light3D ChunkLight(int len, String name) throws IOException
	{
		long end = filePos + len;

		Light3D light = new Light3D();
		light.name = name;
		light.pos = new float[3];
		ChunkVector(light.pos, 0);

		while (filePos < end) {
			int chunkID = ReadUnsignedShort();
			int chunkLen = ReadInt() - 6;

			switch (chunkID) {
			case CHUNK_RGBC:
				light.color = new float[4];
				ChunkRGBC(light.color);
				break;

			case CHUNK_RGB24:
				light.color = new float[4];
				ChunkRGB24(light.color);
				break;

			case CHUNK_SPOTL:
				light.dir = new float[4];
				ChunkVector(light.dir, 0);
				light.theta = (float) (ReadFloat() * Math.PI / 180.0f);
				light.phi = (float) (ReadFloat() * Math.PI / 180.0f);
				break;

			case CHUNK_ONOFF:
			default:
				Skip(chunkLen);
			}
		}
		Seek(end);

		return light;
	}

	private Material3D ChunkMaterial(int len) throws IOException
	{
		long end = filePos + len;

		Material3D mat = new Material3D();

		while (filePos < end) {
			int chunkID = ReadUnsignedShort();
			int chunkLen = ReadInt() - 6;

			switch (chunkID) {
			case CHUNK_TEXTURE:
				mat.texture = ChunkMap(chunkLen);
				break;

			case CHUNK_BUMPMAP:
			case CHUNK_REFLMAP:
				ChunkMap(chunkLen);
				break;

			case CHUNK_AMBIENT:
				mat.ambient = new float[4];
				ChunkColor(chunkLen, mat.ambient);
				break;

			case CHUNK_DIFFUSE:
				mat.diffuse = new float[4];
				ChunkColor(chunkLen, mat.diffuse);
				break;

			case CHUNK_SPECULAR:
				mat.specular = new float[4];
				ChunkColor(chunkLen, mat.specular);
				break;

			case CHUNK_MATNAME:
				mat.name = ChunkName(chunkLen);
				break;

			case CHUNK_MTLTYPE:
				mat.type = ReadUnsignedShort();
				break;

			case CHUNK_SHININES:
				mat.shininess = 100 - ChunkPercent(chunkLen);
				break;

			case CHUNK_SHINSTRN:
				mat.shinStren = ChunkPercent(chunkLen);
				break;

			case CHUNK_TRANSP:
				mat.transparency = ChunkPercent(chunkLen);
				break;

			case CHUNK_SELFILL:
				mat.selfIllum = ChunkPercent(chunkLen);
				break;

			default:
				Skip(chunkLen);
			}
		}
		Seek(end);

		return mat;
	}

	private String ChunkMap(int len) throws IOException
	{
		long end = filePos + len;

		String name = null;

		while (filePos < end) {
			int chunkID = ReadUnsignedShort();
			int chunkLen = ReadInt() - 6;

			switch (chunkID) {
			case CHUNK_MAPFILE:
				name = ChunkName(chunkLen);
				break;

			case CHUNK_MAPPARAM:
			default:
				Skip(chunkLen);
			}
		}
		Seek(end);

		return name;
	}

	private void ChunkKeyframer(Scene3D scene, int len) throws IOException
	{
		int fstart = 0, fend = 100;

		long end = filePos + len;
		while (filePos < end) {
			int chunkID = ReadUnsignedShort();
			int chunkLen = ReadInt() - 6;

			switch (chunkID) {
			case CHUNK_FRAMES:
				fstart = ReadInt();
				fend = ReadInt();
				break;

			case CHUNK_TRACKINFO:
				Animation anim = ChunkMeshTrack(chunkLen, scene);
				if (anim != null)
					scene.animations.add(anim);
				break;

			case CHUNK_SPOTINFO:
			default:
				Skip(chunkLen);
			}
		}

		if (fstart < fend)
			for (int i = 0; i < scene.animations.size(); i++) {
				Animation anim = scene.animations.get(i);
				if (anim.position != null)
					for (int j = 0; j < anim.position.length; j++)
						anim.position[j].time = (anim.position[j].time - fstart) / (fend - fstart);
				if (anim.rotation != null)
					for (int j = 0; j < anim.rotation.length; j++)
						anim.rotation[j].time = (anim.rotation[j].time - fstart) / (fend - fstart);
				if (anim.scaling != null)
					for (int j = 0; j < anim.scaling.length; j++)
						anim.scaling[j].time = (anim.scaling[j].time - fstart) / (fend - fstart);
			}

		Seek(end);
	}

	private Animation ChunkMeshTrack(int len, Scene3D scene) throws IOException
	{
		Animation anim = new Animation();
		int num, i, j, k;

		anim.result = new float[16];
		Matrix.setIdentityM(anim.result, 0);

		anim.world = new float[16];
		Matrix.setIdentityM(anim.world, 0);

		long end = filePos + len;
		while (filePos < end) {
			int chunkID = ReadUnsignedShort();
			int chunkLen = ReadInt() - 6;

			switch (chunkID) {
			case CHUNK_HIERARCHY:
				anim.id = ReadUnsignedShort();
				break;

			case CHUNK_OBJNAME:
				String name = ChunkName(0);
				anim.light = scene.FindLight(name);
				anim.object = scene.FindObject(name);
				Skip(4);
				anim.parent = scene.FindAnimation(ReadUnsignedShort());
				break;

			case CHUNK_PIVOT:
				anim.pivot = new float[3];
				ChunkVector(anim.pivot, 0);
				break;

			case CHUNK_TRACKPOS:
				Skip(10);
				num = ReadInt();
				anim.position = new AnimKey[num];
				for (i = 0; i < num; i++) {
					anim.position[i] = new AnimKey();
					anim.position[i].time = ReadInt();
					k = ReadUnsignedShort();
					for (j = 0; j < 5; j++)
						if ((k & (1 << j)) != 0)
							Skip(4);
					anim.position[i].data = new float[3];
					ChunkVector(anim.position[i].data, 0);
				}
				break;

			case CHUNK_TRACKROT:
				Skip(10);
				num = ReadInt();
				anim.rotation = new AnimKey[num];
				for (i = 0; i < num; i++) {
					anim.rotation[i] = new AnimKey();
					anim.rotation[i].time = ReadInt();
					k = ReadUnsignedShort();
					for (j = 0; j < 5; j++)
						if ((k & (1 << j)) != 0)
							Skip(4);
					anim.rotation[i].data = new float[4];
					anim.rotation[i].data[3] = ReadFloat();
					ChunkVector(anim.rotation[i].data, 0);
				}
				break;

			case CHUNK_TRACKSCL:
				Skip(10);
				num = ReadInt();
				anim.scaling = new AnimKey[num];
				for (i = 0; i < num; i++) {
					anim.scaling[i] = new AnimKey();
					anim.scaling[i].time = ReadInt();
					k = ReadUnsignedShort();
					for (j = 0; j < 5; j++)
						if ((k & (1 << j)) != 0)
							Skip(4);
					anim.scaling[i].data = new float[3];
					ChunkVector(anim.scaling[i].data, 0);
				}
				break;

			default:
				Skip(chunkLen);
			}
		}
		Seek(end);

		return anim;
	}

	private void ChunkColor(int len, float[] color) throws IOException
	{
		long end = filePos + len;
		while (filePos < end) {
			int chunkID = ReadUnsignedShort();
			int chunkLen = ReadInt() - 6;

			switch (chunkID) {
			case CHUNK_RGBC:
				ChunkRGBC(color);
				break;

			case CHUNK_RGB24:
				ChunkRGB24(color);
				break;

			default:
				Skip(chunkLen);
			}
		}
		Seek(end);
	}

	private float ChunkPercent(int len) throws IOException
	{
		float v = 0;

		long end = filePos + len;
		while (filePos < end) {
			int chunkID = ReadUnsignedShort();
			int chunkLen = ReadInt() - 6;

			switch (chunkID) {
			case CHUNK_SHORT:
				v = ReadUnsignedShort() / 100.0f;
				break;

			default:
				Skip(chunkLen);
			}
		}
		Seek(end);

		return v;
	}

	private String ChunkName(int len) throws IOException
	{
		long end = filePos + len;
		int slen = 0;
		byte[] buffer = new byte[128];
		byte c;

		do {
			c = ReadByte();
			if (c != 0)
				buffer[slen++] = c;
		} while (c != 0);

		if (len != 0)
			Seek(end);

		return new String(buffer, 0, slen);
	}

	private void ChunkVector(float[] vec, int offset) throws IOException
	{
		vec[offset + 0] = ReadFloat();
		vec[offset + 2] = ReadFloat();
		vec[offset + 1] = ReadFloat();
	}

	private void ChunkRGBC(float[] c) throws IOException
	{
		c[0] = ReadFloat();
		c[1] = ReadFloat();
		c[2] = ReadFloat();
		c[3] = 1;
	}

	private void ChunkRGB24(float[] c) throws IOException
	{
		c[0] = ReadUnsignedByte() / 255.0f;
		c[1] = ReadUnsignedByte() / 255.0f;
		c[2] = ReadUnsignedByte() / 255.0f;
		c[3] = 1;
	}
}

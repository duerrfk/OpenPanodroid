/* 
 * Copyright 2012 Frank Dürr
 * 
 * This file is part of OpenPanodroid.
 *
 * OpenPanodroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenPanodroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenPanodroid.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openpanodroid;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.openpanodroid.panoutils.android.CubicPanoNative;


import junit.framework.Assert;

import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.util.Log;

public class PanodroidVortexRenderer implements GLSurfaceView.Renderer {
	private static final String LOG_TAG = PanodroidVortexRenderer.class.getSimpleName();
	
    private final static float COORD = (float) Math.sin(Math.PI/4.0);
    
    private final static float ROTATION_ACCELERATION = 20; // [deg/s^2]
    
    private final static float backColorRed = 0.0f;
    private final static float backColorGreen = 0.0f;
    private final static float backColorBlue = 0.0f;
    
    private CubicPanoNative cubicPano;
    
    private PanodroidGLView view;
    
	private int[] textureIds = new int[6];
	
    private FloatBuffer cubeVertexBuffer;
    private ShortBuffer[] faceVertexIndices = new ShortBuffer[6];
    private FloatBuffer[] faceTextureCoordinates = new FloatBuffer[6];
    
	private float fovDeg; // diagonal field of view
    
	// Rotation around x axis in degrees (to a certain latitude circle).
	private float rotationLatitudeDeg = 0.0f;
	// Rotation around y axis in degrees (to a certain longitude circle).
	private float rotationLongitudeDeg = 0.0f;
	// While accessing this matrix, the PanodroidVortexRenderer object has to be locked.
	private float[] rotationMatrix = new float[16];
	
	private int surfaceWidth, surfaceHeight;
    
    private boolean isKineticRotationActive = false;
    private float rotationSpeedLongitude0, rotationSpeedLatitude0; // [deg/s]
    private float latitude0, longitude0;
    private long t0; // [ms]
    
    public PanodroidVortexRenderer(PanodroidGLView view, CubicPanoNative cubicPano) {
    	super();
    	
    	this.view = view;
    	this.cubicPano = cubicPano;
    	this.fovDeg = GlobalConstants.DEFAULT_FOV_DEG;
    	
    	setRotation(0.0f, 0.0f);
    }
    
    public synchronized void startKineticRotation(float rotationSpeedLatitude, float rotationSpeedLongitude) {
    	isKineticRotationActive = true;
    	rotationSpeedLongitude0 = rotationSpeedLongitude;
    	rotationSpeedLatitude0 = rotationSpeedLatitude;
    	latitude0 = rotationLatitudeDeg;
    	longitude0 = rotationLongitudeDeg;
    	t0 = System.currentTimeMillis();
    }
    
    public synchronized void stopKineticRotation() {
    	isKineticRotationActive = false;
    }
    
    private synchronized void doKineticRotation() {
    	if (!isKineticRotationActive) {
    		return;
    	}
    	
    	long currentTime = System.currentTimeMillis();
    	float t = (currentTime-t0)/1000.0f;
    	if (t == 0.0f) {
    		return;
    	}
    	
    	float rLat = t*ROTATION_ACCELERATION;
    	float rLon = t*ROTATION_ACCELERATION;
    	
    	if (Math.abs(rotationSpeedLatitude0) < rLat && Math.abs(rotationSpeedLongitude0) < rLon) {
    		stopKineticRotation();
    	}
    	
    	float deltaLat, deltaLon;
    
    	if (Math.abs(rotationSpeedLatitude0) >= rLat) {
    		float rotSpeedLat_t = rotationSpeedLatitude0 - Math.signum(rotationSpeedLatitude0)*rLat;
    		deltaLat = (rotationSpeedLatitude0+rotSpeedLat_t)/2.0f*t;
    	} else {
    		float tMax = Math.abs(rotationSpeedLatitude0)/ROTATION_ACCELERATION;
    		deltaLat = 0.5f*tMax*rotationSpeedLatitude0;
    	}
    	
    	if (Math.abs(rotationSpeedLongitude0) >= rLon) {
    		float rotSpeedLon_t = rotationSpeedLongitude0 - Math.signum(rotationSpeedLongitude0)*rLon;
    		deltaLon = (rotationSpeedLongitude0+rotSpeedLon_t)/2.0f*t;
    	} else {
    		float tMax = Math.abs(rotationSpeedLongitude0)/ROTATION_ACCELERATION;
    		deltaLon = 0.5f*tMax*rotationSpeedLongitude0;
    	}
    	
    	setRotation(latitude0+deltaLat, longitude0+deltaLon);
    }
    
    private synchronized void calculateRotationMatrix() {
    	// Rotation matrix in column mode.
    	double rotationLongitude = Math.toRadians(rotationLongitudeDeg);
    	double rotationLatitude = Math.toRadians(rotationLatitudeDeg);
    	
    	rotationMatrix[0] = (float) Math.cos(rotationLongitude);
    	rotationMatrix[1] = (float) (Math.sin(rotationLatitude)*Math.sin(rotationLongitude));
    	rotationMatrix[2] = (float) (-1.0*Math.cos(rotationLatitude)*Math.sin(rotationLongitude));
    	rotationMatrix[3] = 0.0f;

    	rotationMatrix[4] = 0.0f;
    	rotationMatrix[5] = (float) Math.cos(rotationLatitude);
    	rotationMatrix[6] = (float) Math.sin(rotationLatitude);
    	rotationMatrix[7] = 0.0f;
    	
    	rotationMatrix[8] = (float) Math.sin(rotationLongitude);
    	rotationMatrix[9] = (float) (-1.0*Math.sin(rotationLatitude)*Math.cos(rotationLongitude));
    	rotationMatrix[10] = (float) (Math.cos(rotationLongitude)*Math.cos(rotationLatitude));
    	rotationMatrix[11] = 0.0f;
    	
    	rotationMatrix[12] = 0.0f;
    	rotationMatrix[13] = 0.0f;
    	rotationMatrix[14] = 0.0f;
    	rotationMatrix[15] = 1.0f;
    }
    
    private void normalizeRotation() {
    	rotationLongitudeDeg %= 360.0f;
    	if (rotationLongitudeDeg < -180.0f) {
    		rotationLongitudeDeg = 360.0f + rotationLongitudeDeg;
    	} else if (rotationLongitudeDeg > 180.0f) {
    		rotationLongitudeDeg = -360.0f + rotationLongitudeDeg;
    	}
    	
    	if (rotationLatitudeDeg < -90.0f) {
			rotationLatitudeDeg = -90.0f;
		} else if (rotationLatitudeDeg > 90.0f) {
			rotationLatitudeDeg = 90.0f;
		}
    	
    	Assert.assertTrue(rotationLongitudeDeg >= -180.0f && rotationLongitudeDeg <= 180.0f);
    	Assert.assertTrue(rotationLatitudeDeg >= -90.0f && rotationLatitudeDeg <= 90.0f);
    }
    
    public void setRotation(float rotationLatitudeDeg, float rotationLongitudeDeg) {
    	this.rotationLongitudeDeg = rotationLongitudeDeg;
    	this.rotationLatitudeDeg = rotationLatitudeDeg;
    	
    	normalizeRotation();
    	
    	calculateRotationMatrix();
    }
    
    public float getRotationLatitudeDeg() {
    	return rotationLatitudeDeg;
    }
    
    public float getRotationLongitudeDeg() {
    	return rotationLongitudeDeg;
    }
	
    public int getSurfaceWidth() {
    	return surfaceWidth;
    }
    
    public int getSurfaceHeight() {
    	return surfaceHeight;
    }
    
    public float getFov() {
    	return fovDeg;
    }
    
    public float getHFovDeg() {
    	float viewDiagonal = (float) Math.sqrt(surfaceHeight*surfaceHeight + surfaceWidth*surfaceWidth);
    	
    	float hFovDeg = (float) surfaceWidth/viewDiagonal * fovDeg;
    	
    	return hFovDeg;
    }
    
    public float getVFovDeg() {
    	float viewDiagonal = (float) Math.sqrt(surfaceHeight*surfaceHeight + surfaceWidth*surfaceWidth);
    	
    	float vFovDeg = (float) surfaceHeight/viewDiagonal * fovDeg;
    	
    	return vFovDeg;
    }
    
    private void initCube(GL10 gl) {
    	calcCubeTriangles();
    }
    
    private void calcCubeTriangles() {
    	// Define the 8 vertices of the cube.
    	// Using 3 dimensions (x, y, z).
    	ByteBuffer vertexByteBuffer = ByteBuffer.allocateDirect(8*3*Float.SIZE/8);
    	vertexByteBuffer.order(ByteOrder.nativeOrder());
    	cubeVertexBuffer = vertexByteBuffer.asFloatBuffer();
    	
    	float coords[] = {
            COORD, COORD, COORD,    // left, top, front
            -COORD, COORD, COORD,   // right, top, front
            -COORD, COORD, -COORD,  // right, top, back
            COORD, COORD, -COORD,   // left, top, back
            COORD, -COORD, COORD,   // left, bottom, front
            -COORD, -COORD, COORD,  // right, bottom, front
    		-COORD, -COORD, -COORD, // right, bottom, back
    		COORD, -COORD, -COORD   // left, bottom, back
    	};
    	
    	cubeVertexBuffer.put(coords);
    	cubeVertexBuffer.position(0);
    	
    	for (CubicPano.TextureFaces face : CubicPano.TextureFaces.values()) {
    		// Define which vertices make up the faces of the cube.
    		// For each of the 6 faces, 4 index entries are required 
    		// (one triangle strip of 2 triangles with 4 vertices per face).
    		int faceNo = face.ordinal();
    		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4*Short.SIZE/8);
    		byteBuffer.order(ByteOrder.nativeOrder());
    		faceVertexIndices[faceNo] = byteBuffer.asShortBuffer();
        
    		// We define triangles such that we are looking onto the inner
    		// side of the cube faces (sitting in the middle of the cube). That is,
    		// vertex order has to be clockwise when looking onto the inner side
    		// of the face. For each face we use a triangle strip (4 vertices).
    		short indices[][] = {
    				{0,1,3,2}, // top
    				{6,5,7,4}, // bottom
    				{0,4,1,5}, // front
    				{2,6,3,7}, // back
    				{1,5,2,6}, // right
    				{3,7,0,4}  // left
    		};
    		
    		switch (face) {
    		case top :
    			faceVertexIndices[faceNo].put(indices[0]);
    			break;
    		case bottom :
    			faceVertexIndices[faceNo].put(indices[1]);
    			break;
    		case front :
    			faceVertexIndices[faceNo].put(indices[2]);
    			break;
    		case back :
    			faceVertexIndices[faceNo].put(indices[3]);
    			break;
    		case right :
    			faceVertexIndices[faceNo].put(indices[4]);
    			break;
    		case left :
    			faceVertexIndices[faceNo].put(indices[5]);
    			break;
    		}
    		
    		faceVertexIndices[faceNo].position(0);
    	}
    }
    
    void rotate() {
    	float coords[] = {
                COORD, COORD, COORD,    // left, top, front
                -COORD, COORD, COORD,   // right, top, front
                -COORD, COORD, -COORD,  // right, top, back
                COORD, COORD, -COORD,   // left, top, back
                COORD, -COORD, COORD,   // left, bottom, front
                -COORD, -COORD, COORD,  // right, bottom, front
        		-COORD, -COORD, -COORD, // right, bottom, back
        		COORD, -COORD, -COORD   // left, bottom, back
        	};
    	
    	double rotationLongitute = Math.toRadians(rotationLongitudeDeg);
    	
    	for (int i = 0; i < coords.length/3; i++) {
    		double x = coords[i*3];
    		double y = coords[i*3 + 1];
    		double z = coords[i*3 + 2];
    		
    		double x2 = Math.cos(rotationLongitute)*x + Math.sin(rotationLongitute)*z;
    		double y2 = y;
    		double z2 = -Math.sin(rotationLongitute)*x + Math.cos(rotationLongitute)*z;
    		
    		coords[i*3] = (float) x2;
    		coords[i*3 + 1] = (float) y2;
    		coords[i*3 + 2] = (float) z2;
    	}
    	
    	cubeVertexBuffer.put(coords);
    	cubeVertexBuffer.position(0);
    }
    
	@Override
	public void onDrawFrame(GL10 gl) {
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
        
    	gl.glMatrixMode(GL10.GL_MODELVIEW);
    	gl.glLoadIdentity();
    	
    	doKineticRotation();
    	
    	synchronized (this) {
    		gl.glLoadMatrixf(rotationMatrix, 0);
    	}
        
        //gl.glRotatef(rotationLongitude, 0.0f, 1.0f, 0.0f);
        
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, cubeVertexBuffer);
        
        for (CubicPano.TextureFaces face : CubicPano.TextureFaces.values()) {
        	int faceNo = face.ordinal();
        	
         	gl.glBindTexture(GL10.GL_TEXTURE_2D, textureIds[faceNo]);
        	gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, faceTextureCoordinates[faceNo]);
        	
           	// For each face, we have to draw 4 vertices
        	// (triangle strip with two triangles).
        	gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, 4, GL10.GL_UNSIGNED_SHORT, faceVertexIndices[faceNo]);
        }
	}

	private void setProjection(GL10 gl) {
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		
	    float aspect = (float) surfaceWidth/(float) surfaceHeight;
	    float dNear = 0.1f;
	    float dFar = 2.0f;
	    float fovYDeg = getVFovDeg();
	    
	    GLU.gluPerspective(gl, fovYDeg, aspect, dNear, dFar);
	}
	
	void setupTextures(GL10 gl) {
		gl.glMatrixMode(GL10.GL_TEXTURE);
		gl.glLoadIdentity();
		
		gl.glGenTextures(6, textureIds, 0);
		
		for (CubicPanoNative.TextureFaces face : CubicPanoNative.TextureFaces.values()) {
			int faceNo = face.ordinal();
			
			gl.glBindTexture(GL10.GL_TEXTURE_2D, textureIds[faceNo]);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
			//gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
			//gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);
			
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
			//gl.glTexEnvf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_DECAL);
			gl.glBlendFunc(GL10.GL_ONE, GL10.GL_SRC_COLOR);
			
			Bitmap bm = null;
			
			Assert.assertTrue(cubicPano != null);
			bm = cubicPano.getFace(face);
			
			//bm =  BitmapFactory.decodeResource(view.getActivity().getResources(), R.drawable.texture);
			//Assert.assertTrue(isPowerOfTwo(bm.getWidth()));
			/*
			switch (face) {
			case top :
				bm =  BitmapFactory.decodeResource(view.getActivity().getResources(), R.drawable.top);
				break;
			case bottom :
				bm =  BitmapFactory.decodeResource(view.getActivity().getResources(), R.drawable.bottom);
				break;
			case left :
				bm =  BitmapFactory.decodeResource(view.getActivity().getResources(), R.drawable.left);
				break;
			case right :
				bm =  BitmapFactory.decodeResource(view.getActivity().getResources(), R.drawable.right);
				break;
			case front :
				bm =  BitmapFactory.decodeResource(view.getActivity().getResources(), R.drawable.front);
				break;
			case back :
				bm =  BitmapFactory.decodeResource(view.getActivity().getResources(), R.drawable.back);
				break;
			default :
				Assert.fail();
			}
			*/
			GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bm, 0);
			
			// If we save the state of the activity, we should not delete the
			// original bitmaps. Otherwise, we have to reload them from the network.
			//bm.recycle();
			
			// For each face, we have to define 8 coordinates although only 4 are used 
			// at a time -- glDrawElements() uses the same indices as for the vertex array
			// to select texture coordinates. Coordinates that are not used are marked
			// as dummy entries.
			float coordinates[][] = {
					{   // top (vertices 0, 1, 3, 2)
						1.0f, 1.0f,
    					0.0f, 1.0f,
    					0.0f, 0.0f,
    					1.0f, 0.0f,
    					0.0f, 0.0f, // dummy
    					0.0f, 0.0f, // dummy
    					0.0f, 0.0f, // dummy
    					0.0f, 0.0f  // dummy
    				},
    				{   // bottom (vertices 6, 5, 7, 4)
						0.0f, 0.0f, // dummy
    					0.0f, 0.0f, // dummy
    					0.0f, 0.0f, // dummy
    					0.0f, 0.0f, // dummy
    					1.0f, 1.0f,
    					0.0f, 1.0f,
    					0.0f, 0.0f,
    					1.0f, 0.0f
    				},
    				{   // front (vertices 0, 4, 1, 5)
						0.0f, 1.0f,
    					1.0f, 1.0f,
    					0.0f, 0.0f, // dummy
    					0.0f, 0.0f, // dummy
    					0.0f, 0.0f,
    					1.0f, 0.0f,
    					0.0f, 0.0f, // dummy
    					0.0f, 0.0f  // dummy
    				},
    				{   // back (vertices 2, 6, 3, 7)
						0.0f, 0.0f, // dummy
    					0.0f, 0.0f, // dummy
    					0.0f, 1.0f,
    					1.0f, 1.0f,
    					0.0f, 0.0f, // dummy
    					0.0f, 0.0f, // dummy
    					0.0f, 0.0f,
    					1.0f, 0.0f
    				},
    				{   // right (vertices 1, 5, 2, 6)
						0.0f, 0.0f, // dummy
    					0.0f, 1.0f, 
    					1.0f, 1.0f,
    					0.0f, 0.0f, // dummy
    					0.0f, 0.0f, // dummy
    					0.0f, 0.0f,
    					1.0f, 0.0f,
    					0.0f, 0.0f  // dummy
    				}, 				  				
    				{   // left (vertices 3, 7, 0, 4)
						1.0f, 1.0f, 
    					0.0f, 0.0f, // dummy
    					0.0f, 0.0f, // dummy
    					0.0f, 1.0f,
    					1.0f, 0.0f,
    					0.0f, 0.0f, // dummy
    					0.0f, 0.0f, // dummy
    					0.0f, 0.0f
    				}
    		};
    		
			ByteBuffer byteBuffer = ByteBuffer.allocateDirect(8*2*Float.SIZE/8);
    		byteBuffer.order(ByteOrder.nativeOrder());
    		faceTextureCoordinates[faceNo] = byteBuffer.asFloatBuffer();
    		
    		switch (face) {
    		case top :
    			faceTextureCoordinates[faceNo].put(coordinates[0]);
    			break;
    		case bottom :
    			faceTextureCoordinates[faceNo].put(coordinates[1]);
    			break;
    		case front :
    			faceTextureCoordinates[faceNo].put(coordinates[2]);
    			break;
    		case back :
    			faceTextureCoordinates[faceNo].put(coordinates[3]);
    			break;
    		case right :
    			faceTextureCoordinates[faceNo].put(coordinates[4]);
    			break;
    		case left :
    			faceTextureCoordinates[faceNo].put(coordinates[5]);
    			break;
    		}
    		
    		faceTextureCoordinates[faceNo].position(0);
		}
		
	}
	
	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		Log.i(LOG_TAG, "Surface changed: width = " + Integer.toString(width) + "; height = " + Integer.toString(height));
	
		surfaceWidth = width;
		surfaceHeight = height;
		
		setProjection(gl);
		
		gl.glViewport(0, 0, width, height);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		gl.glClearColor(backColorRed, backColorGreen, backColorBlue, 1.0f);
		
		// Don't draw back sides.
	    gl.glEnable(GL10.GL_CULL_FACE);
	    gl.glFrontFace(GL10.GL_CCW);
	    gl.glCullFace(GL10.GL_BACK);
	    
	    // Don't need depth tests since we only have one object.
	    //gl.glEnable(GL10.GL_DEPTH_TEST);
	    
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		gl.glEnable(GL10.GL_BLEND);
		gl.glShadeModel(GL10.GL_SMOOTH);
		
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
		
		initCube(gl);
		
		setupTextures(gl);
	}

}

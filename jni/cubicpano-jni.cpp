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

#include "org_openpanodroid_panoutils_android_CubicPanoNative.h"

#include <android/log.h>
#include <android/bitmap.h>
#include <math.h>

/* Set to 1 to enable debug log traces. */
#define DEBUG 0

#if DEBUG != 0
# define  LOG_TAG    "libcubicpano-jni"
# define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
# define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#else
# define  LOGI(...)
# define  LOGE(...)
#endif

#define FRONT_FACE 0
#define BACK_FACE 1
#define TOP_FACE 2
#define BOTTOM_FACE 3
#define LEFT_FACE 4
#define RIGHT_FACE 5

#define M_2PI (2.0*M_PI)

#define RED(x) ((x&0x00ff0000) >> 16)
#define GREEN(x) ((x&0x0000ff00) >> 8)
#define BLUE(x) (x&0x000000ff)

struct CubeCoordinates {
	float x;
	float y;
};
	
struct SphericalCoordinates {
	float latitude;
	float longitude;
};

static const float widthCube = 2.0*cos(M_PI/4.0);
static const float widthCube_2 = widthCube/2.0f;
static const float widthCube_2_pow2 = widthCube_2*widthCube_2;

float deltaCube;
float deltaCube_2;

uint32_t createRGBAPixel(uint32_t red, uint32_t green, uint32_t blue, uint32_t alpha) {
	return ((alpha << 24) | (red << 16) | (green << 8) | (blue));
}

uint32_t getPixelRGBA(const void *pixels, int stride, int x, int y) {
	uint32_t *pixel = (uint32_t *) ((uint8_t *) pixels + y*stride + x*sizeof(uint32_t));
	return (*pixel);
}

void setPixelRGBA(void *pixels, int stride, int x, int y, uint32_t pixel) {
	uint32_t *targetPixel = (uint32_t *) ((uint8_t *) pixels + y*stride + x*sizeof(uint32_t));
	*targetPixel = pixel;
}

uint32_t getBilinearInterpolatedPixel(const void *pixels, int width, int height, int stride, float x, float y) {
	int x1 = (int) x;
	int x2 = x1+1;
	int y1 = (int) y;
	int y2 = y1+1;
	
	uint32_t pixel1 = getPixelRGBA(pixels, stride, x1%width, y1%height);
	uint32_t pixel2 = getPixelRGBA(pixels, stride, x2%width, y1%height);
	uint32_t pixel3 = getPixelRGBA(pixels, stride ,x1%width, y2%height);
	uint32_t pixel4 = getPixelRGBA(pixels, stride, x2%width, y2%height);
	
	int red1 = RED(pixel1);
	int green1 = GREEN(pixel1);
	int blue1 = BLUE(pixel1);
	
	int red2 = RED(pixel2);
	int green2 = GREEN(pixel2);
	int blue2 = BLUE(pixel2);
	
	int red3 = RED(pixel3);
	int green3 = GREEN(pixel3);
	int blue3 = BLUE(pixel3);
	
	int red4 = RED(pixel4);
	int green4 = GREEN(pixel4);
	int blue4 = BLUE(pixel4);
	
	float w1 = x2-x;
	float w2 = x-x1; 
	
	float red1_2 = w1*red1 + w2*red2;
	float red2_3 = w1*red3 + w2*red4;
	
	float green1_2 = w1*green1 + w2*green2;
	float green2_3 = w1*green3 + w2*green4;
	
	float blue1_2 = w1*blue1 + w2*blue2;
	float blue2_3 = w1*blue3 + w2*blue4;
	
	w1 = y2-y;
	w2 = y-y1;
	
	int red = (int) (w1*red1_2 + w2*red2_3 + 0.5);
	int green = (int) (w1*green1_2 + w2*green2_3 + 0.5);
	int blue = (int) (w1*blue1_2 + w2*blue2_3 + 0.5);

	return createRGBAPixel(red, green, blue, 0);
}

uint32_t getEquirectangularPixel(const void *pixels, int width, int height, int stride, const SphericalCoordinates *sphericalCoordinates) {
	float xPixel = (sphericalCoordinates->longitude + M_PI) / M_2PI * width; 
	float yPixel = (sphericalCoordinates->latitude + M_PI_2) / M_PI * height;
	yPixel = height-yPixel;
	
	return getBilinearInterpolatedPixel(pixels, width, height, stride, xPixel, yPixel);
}

// This function just serves as an optimization to the 
// function textureToCubeCoordinates(), so we don't have
// to calculate these (for one texture size) static values
// over and over again.
void initTextureToCubeCoordinates(int widthTexture) {
	deltaCube = widthCube/widthTexture;
	deltaCube_2 = deltaCube/2.0f;
}

void textureToCubeCoordinates(int xTexture, int yTexture, CubeCoordinates *cubeCoordinates) {
	cubeCoordinates->x = (deltaCube*xTexture) - widthCube_2 + deltaCube_2;
	cubeCoordinates->y = (deltaCube*yTexture) - widthCube_2 + deltaCube_2;       
}

void normalizeSphericalCoordinates(SphericalCoordinates *coords) {
	while (coords->latitude < -M_PI) {
		coords->latitude += M_2PI;
	}
	
	while (coords->latitude > M_PI) {
		coords->latitude -= M_2PI;
	}
	
	// latitude is now between -180 and +180 degrees
	
	if (coords->latitude < -M_PI_2) {
		coords->latitude = -1.0f*(M_PI + coords->latitude);
		coords->longitude += M_PI;
	}
	
	if (coords->latitude > M_PI_2) {
		coords->latitude = M_PI - coords->latitude;
		coords->longitude += M_PI;
	}
	
       // latitude is now between -90 and +90 degrees

	while (coords->longitude < -M_PI) {
		coords->longitude += M_2PI;
	}
	
	while (coords->longitude > M_PI) {
		coords->longitude -= M_2PI;
	}

	// longitude is now between -180 and +180 degrees
}

void cubeToSphericalCoordinates1(const CubeCoordinates *cubeCoordinates, int face, SphericalCoordinates *sphericalCoordinates) {
	float d = sqrt((cubeCoordinates->x * cubeCoordinates->x) + widthCube_2_pow2);
	sphericalCoordinates->latitude = atan(cubeCoordinates->y / d);
	
	sphericalCoordinates->longitude = atan(cubeCoordinates->x / widthCube_2);
	
	switch (face) {
	case FRONT_FACE:
		break;
	case BACK_FACE:
		sphericalCoordinates->longitude += M_PI;
		break;
	case LEFT_FACE:
		sphericalCoordinates->longitude -= M_PI_2;
		break;
	case RIGHT_FACE:
		sphericalCoordinates->longitude += M_PI_2;
		break;
	default:
		LOGE("cubeToSphericalCoordinates1(): invalid face");
	}
}

void cubeToSphericalCoordinates2(const CubeCoordinates *cubeCoordinates, int face, SphericalCoordinates *sphericalCoordinates) {
	if (cubeCoordinates->x == 0.0f) {
		if (cubeCoordinates->y > 0.0f) {
			sphericalCoordinates->longitude = 0.0f;
		} else {
			sphericalCoordinates->longitude = M_PI;
		}
	} else {
		float beta = atan(fabs(cubeCoordinates->y) / fabs(cubeCoordinates->x));
		
		if (cubeCoordinates->x >= 0.0f && cubeCoordinates->y >= 0.0f) {
			sphericalCoordinates->longitude = -M_PI_2 + beta;
		} else if (cubeCoordinates->x < 0.0f && cubeCoordinates->y >= 0.0f) {
			sphericalCoordinates->longitude = M_PI_2 - beta;
		} else if (cubeCoordinates->x < 0.0f && cubeCoordinates->y < 0.0f) {
			sphericalCoordinates->longitude = M_PI_2 + beta;
		} else if (cubeCoordinates->x >= 0.0f && cubeCoordinates->y < 0.0f) {
			sphericalCoordinates->longitude = -M_PI_2 - beta;
		}
	}
	
	float z = sqrt((cubeCoordinates->x * cubeCoordinates->x) + (cubeCoordinates->y * cubeCoordinates->y));
	float alpha =  atan(z/widthCube_2);
	
	switch (face) {
	case TOP_FACE:
		sphericalCoordinates->latitude = M_PI_2 - alpha;
		break;
	case BOTTOM_FACE:
		sphericalCoordinates->latitude = -M_PI_2 + alpha;
		break;
	default:
		LOGE("cubeToSphericalCoordinates2(): Invalid face.");
	}	
}

void cubeToSphericalCoordinates(const CubeCoordinates *cubeCoordinates, int face, SphericalCoordinates *sphericalCoordinates) {
	if (face == TOP_FACE || face == BOTTOM_FACE) {
		cubeToSphericalCoordinates2(cubeCoordinates, face, sphericalCoordinates);
	} else {
		cubeToSphericalCoordinates1(cubeCoordinates, face, sphericalCoordinates);
	}
	
	normalizeSphericalCoordinates(sphericalCoordinates);
}	

JNIEXPORT void JNICALL Java_org_openpanodroid_panoutils_android_CubicPanoNative_calculateCubeSide(JNIEnv *env, jclass obj, jobject panoBmp, jobject faceBmp, jint face) {

	LOGI("Entering native calculateCubeSide()");

	AndroidBitmapInfo panoBmpInfo;
	AndroidBitmapInfo faceBmpInfo;
	void *panoPixels;
	void *facePixels;
	CubeCoordinates cubeCoordinates;
	SphericalCoordinates sphericalCoordinates;

	if (AndroidBitmap_getInfo(env, panoBmp, &panoBmpInfo) < 0) {
		LOGE("Could not get info for pano bitmap");
		return;
	}

	if (AndroidBitmap_getInfo(env, faceBmp, &faceBmpInfo) < 0) {
		LOGE("Could not get info for face bitmap");
		return;
	}

	if (panoBmpInfo.width != 2*panoBmpInfo.height) {
		LOGE("Invalid pano bitmap size (width != 2*height)");
		return;
	}

	if (faceBmpInfo.width != faceBmpInfo.height) {
		LOGE("Invalid face bitmap size (width != height)");
		return;
	}

	if (faceBmpInfo.format != panoBmpInfo.format) {
		LOGE("Format of pano bitmap and face bitmap not equal");
		return;
	}

	if (panoBmpInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
		LOGE("Invalid bitmap format != RGBA_8888");
		return;
	}

	if (AndroidBitmap_lockPixels(env, panoBmp, &panoPixels) < 0) {
		LOGE("Could not lock pixels of pano bitmap");
		return;
	}

	if (AndroidBitmap_lockPixels(env, faceBmp, &facePixels) < 0) {
		LOGE("Could not lock pixels of face bitmap");
		AndroidBitmap_unlockPixels(env, panoBmp);
		return;
	}

	initTextureToCubeCoordinates(faceBmpInfo.width);

	for (int xTexture = 0; xTexture < faceBmpInfo.width; xTexture++) {
		LOGI("Texture row: %d", xTexture);
		for (int yTexture = 0; yTexture < faceBmpInfo.height; yTexture++) {
			textureToCubeCoordinates(xTexture, yTexture, &cubeCoordinates);
			
			cubeToSphericalCoordinates(&cubeCoordinates, face, &sphericalCoordinates);
			
			uint32_t pixel = getEquirectangularPixel(panoPixels, panoBmpInfo.width, panoBmpInfo.height, panoBmpInfo.stride, &sphericalCoordinates);
			
			setPixelRGBA(facePixels, faceBmpInfo.stride, xTexture, yTexture, pixel);
		}
	}

	AndroidBitmap_unlockPixels(env, panoBmp);
	AndroidBitmap_unlockPixels(env, faceBmp);

	LOGI("Leaving native calculateCubeSide()");
}



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

import junit.framework.Assert;
import android.graphics.Bitmap;

public class CubicPano {
	public enum TextureFaces {front, back, top, bottom, left, right};
	
	private Bitmap front, back, top, bottom, left, right;
	
	public CubicPano(Bitmap front, Bitmap back, Bitmap top, Bitmap bottom, Bitmap left, Bitmap right) {
		Assert.assertFalse(front == null);
		Assert.assertFalse(back == null);
		Assert.assertFalse(top == null);
		Assert.assertFalse(bottom == null);
		Assert.assertFalse(left == null);
		Assert.assertFalse(right == null);
		
		Assert.assertTrue(front.getWidth() == front.getHeight());
		Assert.assertTrue(back.getWidth() == back.getHeight());
		Assert.assertTrue(left.getWidth() == left.getHeight());
		Assert.assertTrue(right.getWidth() == right.getHeight());
		Assert.assertTrue(top.getWidth() == top.getHeight());
		Assert.assertTrue(bottom.getWidth() == bottom.getHeight());
		
		Assert.assertTrue(front.getWidth() == back.getWidth());
		Assert.assertTrue(front.getWidth() == left.getWidth());
		Assert.assertTrue(front.getWidth() == right.getWidth());
		Assert.assertTrue(front.getWidth() == top.getWidth());
		Assert.assertTrue(front.getWidth() == bottom.getWidth());
		
		this.front = front;
		this.back = back;
		this.top = top;
		this.bottom = bottom;
		this.left = left;
		this.right = right;
	}
	
	public Bitmap getFace(TextureFaces face) {
		Bitmap bmp = null;
		
		switch (face) {
		case front :
			bmp = front;
			break;
		case back :
			bmp =  back;
			break;
		case left :
			bmp = left;
			break;
		case right :
			bmp = right;
			break;
		case top :
			bmp = top;
			break;
		case bottom :
			bmp = bottom;
			break;
		default:
			Assert.fail();
		}
		
		return bmp;
	}
}

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

package org.openpanodroid.ioutils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;

import junit.framework.Assert;

public class Pipe {
	private PipedInputStream is;
	private PipedOutputStream os;
	
	private byte[] dataBuffer;
	private int availableDataCnt;
	private int availableBufferSpace;
	private int readPos;
	private int writePos;
	
	class PipedInputStream extends InputStream {

		private byte[] oneByteBuffer = new byte[1];
		private boolean isClosed;
		
		public PipedInputStream() {
			isClosed = false;
		}
	
		protected void finalize() {
			close();
		}
		
		@Override
		public void close() {
			synchronized (dataBuffer) {
				isClosed = true;
				dataBuffer.notify();
			}
		}
		
		public boolean isClosed() {
			return isClosed;
		}
		
		@Override
		public int read() throws IOException {
			int readCnt = read(oneByteBuffer, 0, 1);
			if (readCnt == 1) {
				return oneByteBuffer[0];
			} else {
				Assert.assertTrue(readCnt == -1);
				return readCnt;
			}
		}
		
		@Override
		public int read(byte[] b) throws IOException {
			return read(b, 0, b.length);
		}
		
		@Override
		public int read(byte[] buffer, int offset, int length) throws IOException {
			if (offset < 0) {
				throw new IndexOutOfBoundsException("Offset must be >= 0.");
			}
			
			if (length <= 0) {
				throw new IndexOutOfBoundsException("Length must be > 0.");
			}
			
			if (offset + length > buffer.length) {
				throw new IndexOutOfBoundsException("Offset + length must be <= buffer.length");
			}
			
			// We never return more than the buffer size bytes in one turn.
			length = length <= dataBuffer.length ? length : dataBuffer.length;
			
			synchronized (dataBuffer) {
				int readCnt;
				
				while (availableDataCnt <= 0) {
					if (os.isClosed()) {
						// No more data available and pipe output stream (source end) is closed. 
						// --> EOF
						return -1;
					}
					
					try {
						dataBuffer.wait();
					} catch (InterruptedException e) {
						throw new IOException("Interrupted during wait for data.");
					}
				}
				
				if (availableDataCnt < length) {
					// return #availableCnt bytes
					readCircular(buffer, offset, availableDataCnt);
					readCnt = availableDataCnt;
				} else {
					// return #length bytes
					readCircular(buffer, offset, length);
					readCnt = length;
				}
				
				readPos += readCnt;
				readPos %= dataBuffer.length;
				
				availableDataCnt -= readCnt;
				availableBufferSpace += readCnt;
				Assert.assertTrue(availableDataCnt+availableBufferSpace == dataBuffer.length);
				
				// Notify waiting writer: we have free buffer space.
				dataBuffer.notify();
				return readCnt;
			}
		}
		
		private void readCircular(byte[] buffer, int offset, int length) {
			Assert.assertTrue(length <= dataBuffer.length);
			
			int distToEnd = dataBuffer.length-readPos;
			
			if (distToEnd < length) {
				int len1 = distToEnd;
				int len2 = length-len1;
				System.arraycopy(dataBuffer, readPos, buffer, offset, len1);
				System.arraycopy(dataBuffer, 0, buffer, offset+len1, len2);
			} else {
				System.arraycopy(dataBuffer, readPos, buffer, offset, length);
			}
		}
		
	}
	
	class PipedOutputStream extends OutputStream {

		private byte[] oneByteBuffer = new byte[1];
		private boolean isClosed;
		
		public PipedOutputStream() {
			isClosed = false;
		}
		
		protected void finalize() {
			close();
		}
		
		public boolean isClosed() {
			return isClosed;
		}
		
		@Override
		public void write(int oneByte) throws IOException {
			oneByteBuffer[0] = (byte) oneByte;
			write(oneByteBuffer, 0, 1);	
		}
		
		@Override
		public void write(byte[] buffer) throws IOException {
			write(buffer, 0, buffer.length);
		}
		
		@Override
		public void write(byte[] buffer, int offset, int count) throws IOException {
			if (offset < 0) {
				throw new IndexOutOfBoundsException("Offset must be >= 0.");
			}
			
			if (count <= 0) {
				throw new IndexOutOfBoundsException("Count must be > 0.");
			}
			
			if (offset+count > buffer.length) {
				throw new IndexOutOfBoundsException("Offset + count must be <= buffer.length");
			}
			
			int writeCnt;
			
			while (count > 0) {
				synchronized (dataBuffer) {			
					while (availableBufferSpace <= 0 && !is.isClosed()) {
						try {
							dataBuffer.wait();
						} catch (InterruptedException e) {
							throw new IOException("Interrupted while waiting for buffer space to become available.");
						}
					}
				
					if (is.isClosed()) {
						// Pipe input stream (sink) is closed, i.e., no one will read this data anymore.
						throw new IOException("Pipe closed.");
					}
					
					// Data is available.
					
					Assert.assertTrue(availableBufferSpace > 0);
					
					if (count <= availableBufferSpace) {
						// write #count
						writeCircular(buffer, offset, count);
						writeCnt = count;
					} else {
						// write #availableBufferSpace
						writeCircular(buffer, offset, availableBufferSpace);
						writeCnt = availableBufferSpace;
					}
				
					writePos += writeCnt;
					writePos %= dataBuffer.length;
				
					availableBufferSpace -= writeCnt;
					availableDataCnt += writeCnt;
					Assert.assertTrue(availableDataCnt+availableBufferSpace == dataBuffer.length);
					
					// Notify waiting reader. There's new data to be read.
					dataBuffer.notify();
				}
				
				count -= writeCnt;
				offset += writeCnt;
			}
		}
		
		private void writeCircular(byte[] buffer, int offset, int length) {
			Assert.assertTrue(length <= dataBuffer.length);
			
			int distToEnd = dataBuffer.length-writePos;
			
			if (distToEnd < length) {
				int len1 = distToEnd;
				int len2 = length-len1;
				
				System.arraycopy(buffer, offset, dataBuffer, writePos, len1);
				System.arraycopy(buffer, offset+len1, dataBuffer, 0, len2);
			} else {
				System.arraycopy(buffer, offset, dataBuffer, writePos, length);
			}
		}
		
		@Override
		public void close() {
			synchronized (dataBuffer) {
				isClosed = true;
				dataBuffer.notify();
			}
		}
	}
	
	public Pipe(int bufferSize) {
		if (bufferSize <= 0) {
			throw new InvalidParameterException("Buffer must be greater than 0.");
		}
		
		dataBuffer = new byte[bufferSize];
		readPos = 0;
		writePos = 0;
		availableDataCnt = 0;
		availableBufferSpace = dataBuffer.length;
		
		is = new PipedInputStream();
		os = new PipedOutputStream();
	}
	
	public InputStream getInputStream() {
		return is;
	}
	
	public OutputStream getOutputStream() {
		return os;
	}
	
}

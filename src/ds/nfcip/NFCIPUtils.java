/*
 * Util - Some useful small methods for NFCIPConnection 
 * 
 * Copyright (C) 2009  François Kooman <F.Kooman@student.science.ru.nl>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package ds.nfcip;

import java.util.Vector;

/**
 * Some small abstract methods used by NFCIPConnection implementations and the
 * tests
 * 
 * @author F. Kooman <F.Kooman@student.science.ru.nl>
 * 
 */
public class NFCIPUtils {

	private NFCIPUtils() {
	}

	/**
	 * Converts a byte array to readable string
	 * 
	 * @param a
	 *            array to print
	 * @return readable byte array string
	 */
	public static String byteArrayToString(byte[] a) {
		if (a == null)
			return "[null]";
		if (a.length == 0)
			return "[empty]";
		String result = "";
		for (int i = 0; i < a.length; i++) {
			result += byteToString(a[i]) + " ";
		}
		return result;
	}

	/**
	 * Append a byte array to another byte array
	 * 
	 * @param first
	 *            the byte array to append to
	 * @param second
	 *            the byte array to append
	 * @return the appended array
	 */
	public static byte[] appendToByteArray(byte[] first, byte[] second) {
		int secondLength = (second != null) ? second.length : 0;
		return appendToByteArray(first, second, 0, secondLength);
	}

	/**
	 * Append a byte array to another byte array specifying which part of the
	 * second byte array should be appended to the first
	 * 
	 * @param first
	 *            the byte array to append to
	 * @param second
	 *            the byte array to append
	 * @param offset
	 *            offset in second array to start appending from
	 * @param length
	 *            number of bytes to append from second to first array
	 * @return the appended array
	 */
	public static byte[] appendToByteArray(byte[] first, byte[] second,
			int offset, int length) {
		if (second == null || second.length == 0) {
			// if (first == null)
			// return new byte[0];
			return first;
		}
		int firstLength = (first != null) ? first.length : 0;

		if (length < 0 || offset < 0 || second.length < length + offset)
			throw new ArrayIndexOutOfBoundsException();
		byte[] result = new byte[firstLength + length];
		if (firstLength > 0)
			System.arraycopy(first, 0, result, 0, firstLength);
		System.arraycopy(second, offset, result, firstLength, length);
		return result;
	}

	/**
	 * Return a specific part of a byte array starting from <code>offset</code>
	 * with <code>length</code>
	 * 
	 * @param array
	 *            the byte array
	 * @param offset
	 *            the offset in the array from where to start in bytes
	 * @param length
	 *            the number of bytes to get
	 * @return the sub byte array
	 */
	public static byte[] subByteArray(byte[] array, int offset, int length) {
		return appendToByteArray(null, array, offset, length);
	}

	/**
	 * Convert a byte to a human readable representation
	 * 
	 * @param b
	 *            the byte
	 * @return the human readable representation
	 */
	public static String byteToString(int b) {
		String s = Integer.toHexString(b);
		if (s.length() == 1)
			s = "0" + s;
		else
			s = s.substring(s.length() - 2);
		s = "0x" + s.toUpperCase();
		return s;
	}

	/**
	 * Add padding to a data block
	 * 
	 * @param data
	 *            the block to be padded
	 * @return the padded block
	 */
	public static byte[] addPadding(byte[] data) {
		int dataLength = (data == null) ? 0 : data.length;
		if (dataLength >= 23) {
			return NFCIPUtils.appendToByteArray(new byte[] { 0x00 }, data);
		} else if (dataLength == 22) {
			return NFCIPUtils.appendToByteArray(new byte[] { 0x02 }, data);
		} else {
			byte[] padding = new byte[23 - dataLength];
			padding[0] = 0x01;
			padding[23 - dataLength - 1] = 0x01;
			return NFCIPUtils.appendToByteArray(padding, data);
		}
	}

	/**
	 * Remove padding from a data block
	 * 
	 * @param data
	 *            the data from which to remove the padding
	 * @return unpadded data
	 */
	public static byte[] removePadding(byte[] data) {
		int dataLength = (data == null) ? 0 : data.length;
		if (data[0] == 0x00 || data[0] == 0x02) {
			return NFCIPUtils.subByteArray(data, 1, dataLength - 1);
		} else {
			int i = 1;
			while (data[i] == 0x00)
				i++;
			return NFCIPUtils.subByteArray(data, i + 1, dataLength - i - 1);
		}
	}

	/**
	 * Determine whether the received block is the expected block
	 * 
	 * @param chainingIndicator
	 *            whether or not custom chaining is used
	 * @param expectedBlock
	 *            the expected block number
	 * @return whether or not this is the expected block
	 */
	public static boolean isExpectedBlock(byte chainingIndicator,
			byte expectedBlock) {
		/* bit 1 seen from the right shows what the expected number is */
		if ((chainingIndicator & 0x02) == 0x02) {
			/* bit 1 is set, so we expect expectedBlock to be 1 */
			return expectedBlock == 0x01;
		} else {
			/* bit 1 is not set, so we expect expectedBlock to be 0 */
			return expectedBlock == 0x00;
		}
	}

	public static Vector dataToBlockVector(byte[] data, int blockSize) {
		return dataToBlockVector(data, blockSize, true, true);
	}

	/**
	 * Convert a byte array to a Vector of byte arrays with a block size with
	 * optionally a chaining indicator
	 * 
	 * @param data
	 *            the byte array to convert to blocks of byte arrays
	 * @param blockSize
	 *            the size of one block (including the optional chaining
	 *            indicator)
	 * @param chainingIndicator
	 *            whether or not to use a chaining indicator to indicate that
	 *            more blocks are following this one. This is denoted by bit 0
	 *            set in the first byte of each block. The bit set to one means
	 *            there is more data coming, set to 0 means there is no more
	 *            data after this block
	 * @param addBlockNumbers
	 *            whether or not to use block numbers in the chaining indicator
	 *            byte
	 * @return vector of byte arrays containing the split byte array
	 */
	public static Vector dataToBlockVector(byte[] data, int blockSize,
			boolean chainingIndicator, boolean addBlockNumbers) {
		Vector v = new Vector();
		int dataPointer = 0;
		int dataLength = (data == null) ? 0 : data.length;
		if (dataLength == 0 && chainingIndicator) {
			// v.add(new byte[] { 0x00 });
			v.addElement(new byte[] { 0x00 });
			return v;
		}
		if (dataLength == 0) {
			// v.add(new byte[0]);
			v.addElement(new byte[0]);
			return v;
		}
		if (chainingIndicator & blockSize < 2)
			throw new IllegalArgumentException(
					"block size should be >= 2 when using the chaining indicator");
		else if (blockSize < 1)
			throw new IllegalArgumentException("block size should be >= 1");

		while (dataLength > 0) {
			int blkSize, blkDataSize;
			if (chainingIndicator) {
				blkSize = (dataLength >= blockSize) ? blockSize
						: dataLength + 1;
				blkDataSize = blkSize - 1;
			} else {
				blkSize = (dataLength >= blockSize) ? blockSize : dataLength;
				blkDataSize = blkSize;
			}
			byte[] blk = new byte[blkSize];
			System.arraycopy(data, dataPointer, blk, blkSize - blkDataSize,
					blkDataSize);
			dataPointer += blkDataSize;
			dataLength -= blkDataSize;
			if (chainingIndicator)
				blk[0] = 0x01;
			// v.add(blk);
			v.addElement(blk);
		}
		if (chainingIndicator) {
			((byte[]) v.lastElement())[0] = 0x00;
			if (addBlockNumbers) {
				for (int i = 0; i < v.size(); i++)
					((byte[]) v.elementAt(i))[0] = (byte) (((byte[]) v
							.elementAt(i))[0] | ((i % 2) << 1));
			}
		}
		return v;
	}

	/**
	 * Converts a Vector of byte arrays back to a byte array of data in case the
	 * chaining byte is present
	 * 
	 * @param bv
	 *            the Vector
	 * @return the data
	 */
	public static byte[] blockVectorToData(Vector bv) {
		return blockVectorToData(bv, true);
	}

	/**
	 * Converts a Vector of byte arrays back to a byte array of data
	 * 
	 * @param bv
	 *            the Vector
	 * @param chainingIndicator
	 *            indicates whether or not chaining byte is present as the first
	 *            byte of the byte arrays in the Vector
	 * @return the data
	 */
	public static byte[] blockVectorToData(Vector bv, boolean chainingIndicator) {
		if (bv == null || bv.size() == 0)
			throw new IllegalArgumentException("invalid block vector");
		byte[] data = new byte[0];
		for (int i = 0; i < bv.size(); i++) {
			byte[] block = (byte[]) bv.elementAt(i);
			if (chainingIndicator)
				block = NFCIPUtils.subByteArray(block, 1, block.length - 1);
			data = NFCIPUtils.appendToByteArray(data, block);
		}
		return data;
	}

	/**
	 * Gets the block number of a block, that is, bit 1 is checked, if bit 1 is
	 * 1 then the block number of 1, otherwise it is zero
	 * 
	 * @param data
	 *            the data to analyze
	 * @return the block number
	 */
	public static int getBlockNumber(byte[] data) {
		if (data == null || data.length == 0)
			return -1;
		if ((data[0] & 0x02) == 0x02)
			return 1;
		return 0;
	}

	/**
	 * Checks whether or not a data block is chained, that is, the first byte
	 * has the chained flag set
	 * 
	 * @param data
	 *            the data to analyze
	 * @return whether or not the flag was set
	 */
	public static boolean isChained(byte[] data) {
		if (data == null || data.length == 0)
			return false;
		return (data[0] & 0x01) == 0x01;
	}

	/**
	 * Checks whether this block is the END block, this is necessary to make
	 * sure that the initiator received all the data from the target
	 * 
	 * @param data
	 *            the data to analyze
	 * @return whether or not this is an end block
	 */
	public static boolean isEndBlock(byte[] data) {
		if (data == null || data.length == 0)
			return false;
		return (data[0] & 0x04) == 0x04;
	}

	/**
	 * Checks whether this block is an "empty" block, generally this is used by
	 * the sender to ask for more data
	 * 
	 * @param data
	 *            the data to analyze
	 * @return whether or not this is an "empty" block
	 */
	public static boolean isEmptyBlock(byte[] data) {
		if (data == null || data.length == 0)
			return false;
		return (data[0] & 0x08) == 0x08;
	}

	/**
	 * Check whether or not this is an "null" block meaning that there is no
	 * actual data in the block, which is a situation that should never occur.
	 * This does happen sometimes on the Nokia 6131 NFC so we have to check for
	 * this
	 * 
	 * @param data
	 *            the data to analyze
	 * @return whether or not this is an "null" block
	 */
	public static boolean isNullBlock(byte[] data) {
		return data == null || data.length == 0;
	}

	/**
	 * Checks whether this block is a "dummy" block, this is used for the
	 * <code>FAKE_INITIATOR</code> and <code>FAKE_TARGET</code> modes to switch
	 * to send respectively receive mode
	 * 
	 * @param data
	 *            the data to analyze
	 * @return whether or not this is a "dummy" block
	 */
	public static boolean isDummyBlock(byte[] data) {
		if (data == null || data.length == 0)
			return false;
		return (data[0] & 0x88) == 0x88;
	}

	// Taken from OpenJDK sources (GPLv2 only)

	/**
	 * Returns <tt>true</tt> if the two specified arrays of bytes are
	 * <i>equal</i> to one another. Two arrays are considered equal if both
	 * arrays contain the same number of elements, and all corresponding pairs
	 * of elements in the two arrays are equal. In other words, two arrays are
	 * equal if they contain the same elements in the same order. Also, two
	 * array references are considered equal if both are <tt>null</tt>.
	 * <p>
	 * 
	 * @param a
	 *            one array to be tested for equality
	 * @param a2
	 *            the other array to be tested for equality
	 * @return <tt>true</tt> if the two arrays are equal
	 */
	public static boolean arrayCompare(byte[] a, byte[] a2) {
		if (a == a2)
			return true;
		if (a == null || a2 == null)
			return false;

		int length = a.length;
		if (a2.length != length)
			return false;

		for (int i = 0; i < length; i++)
			if (a[i] != a2[i])
				return false;

		return true;
	}

	/**
	 * Converts the mode to human readable format
	 * 
	 * @param mode
	 *            the mode to convert
	 * @return the human readable mode
	 */
//	public static String modeToString(int mode) {
//		switch (mode) {
//		case NFCIPInterface.INITIATOR:
//			return "INITIATOR";
//		case NFCIPInterface.FAKE_INITIATOR:
//			return "FAKE_INITIATOR";
//		case NFCIPInterface.TARGET:
//			return "TARGET";
//		case NFCIPInterface.FAKE_TARGET:
//			return "FAKE_TARGET";
//		default:
//			return "UNKNOWN";
//		}
//	}
        /**
     * Convert the byte array to an int starting from the given offset.
     *
     * @param b The byte array
     * @param offset The array offset
     * @return The integer
     */
    public static int byteArrayToInt(byte[] b, int offset ) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (b[i + offset] & 0x000000FF) << shift;
        }
        return value;
    }
}

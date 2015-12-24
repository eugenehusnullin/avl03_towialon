package moni.avl03.encode;

import java.util.zip.Checksum;

public class CRC16 implements Checksum {

	/** CCITT polynomial: x^16 + x^12 + x^5 + 1 -> 0x1021 (1000000100001) */
	private static final int poly = 0x1021;
	private static final int[] table = new int[256];
	private int value = 0;

	static { // initialize static lookup table
		for (int i = 0; i < 256; i++) {
			int crc = i << 8;
			for (int j = 0; j < 8; j++) {
				if ((crc & 0x8000) == 0x8000) {
					crc = (crc << 1) ^ poly;
				} else {
					crc = (crc << 1);
				}
			}
			table[i] = crc & 0xffff;
		}
	}

	private int update(int crc, byte[] bytes, int off, int len) {
		for (int i = off; i < (off + len); i++) {
			int b = (bytes[i] & 0xff);
			crc = (table[((crc >> 8) & 0xff) ^ b] ^ (crc << 8)) & 0xffff;
		}
		return crc;
	}

	@Override
	public long getValue() {
		return value;
	}

	@Override
	public void reset() {
		value = 0;
	}

	@Override
	public void update(int b) {
		byte[] ba = { (byte) (b & 0xff) };
		value = update(value, ba, 0, 1);
	}

	public void update(byte[] b) {
		value = update(value, b, 0, b.length);
	}

	@Override
	public void update(byte[] b, int off, int len) {
		value = update(value, b, off, len);
	}
}

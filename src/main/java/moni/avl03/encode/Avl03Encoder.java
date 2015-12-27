package moni.avl03.encode;

import java.nio.charset.Charset;
import java.util.Formatter;
import java.util.Locale;

import moni.avl03.domain.InfoMessage;

public class Avl03Encoder {
	private Charset asciiCharset = Charset.forName("ASCII");

	// src:
	// $$B2359772031093009|AA$GPRMC,210006.000,A,5542.2011,N,03741.1583,E,0.00,,111215,,,A*78|01.7|01.0|01.3|000000000000|20151211210006|14291311|00000000|1E5177FE|0000|0.0000|0375|5920
	// dst:
	// $$B6359772031093009|AA$GPRMC,210006.000,A,5542.2012,N,3741.1582,E,0.00,0.00,111215,0,N,A*2A|1.7|1.0|1.3|000000000000|20151211210006|14291311|00000000|1E5177FE|0000|0.0000|0375|||7B3A
	public String encode(InfoMessage mes) {
		StringBuilder sb = new StringBuilder();
		Formatter formatter = new Formatter(sb, Locale.US);

		sb.append("$$");
		formatter.format("%015d", mes.getImei());
		sb.append('|');
		sb.append(mes.getAlarmType());
		sb.append(buildNmea(mes));
		sb.append('|');
		formatter.format("%04.1f", mes.getPdop());
		sb.append('|');
		formatter.format("%04.1f", mes.getHdop());
		sb.append('|');
		formatter.format("%04.1f", mes.getVdop());
		sb.append('|');
		sb.append(mes.getStatus());
		sb.append('|');
		formatter.format("%1$tY%1$tm%1$td%1$tH%1$tM%1$tS", mes.getRtcDate());
		sb.append('|');
		sb.append(mes.getVoltage());
		sb.append('|');
		sb.append(mes.getAdc());
		sb.append('|');
		sb.append(mes.getLacci());
		sb.append('|');
		sb.append(mes.getTemperature());
		sb.append('|');
		formatter.format("%.4f", mes.getOdometer());
		sb.append('|');
		formatter.format("%04d", mes.getSerialId());
		sb.append("|||");
		sb.insert(2, String.format("%X", sb.length() + 6));

		long crc = createCheckSum(sb.toString());
		formatter.format("%04X", crc);

		formatter.close();
		return sb.toString();
	}

	private long createCheckSum(String s) {
		byte[] bytes = s.getBytes(asciiCharset);

		CRC16 crc16 = new CRC16();
		crc16.update(bytes);
		return crc16.getValue();
	}

	private String buildNmea(InfoMessage mes) {
		StringBuilder sb = new StringBuilder();
		Formatter formatter = new Formatter(sb, Locale.US);

		if (mes.getState() == 'Z') {
			formatter.format("%060d", 0);
		} else {
			sb.append("$GPRMC,");
			formatter.format("%1$tH%1$tM%1$tS.%1$tL", mes.getNavDate());
			sb.append(',');
			sb.append(mes.getState());
			sb.append(',');

			double lat = convertToGprmcCoord(mes.getLat());
			formatter.format("%.4f", lat);
			sb.append(',');
			sb.append(mes.getLatLetter());
			sb.append(',');
			double lon = convertToGprmcCoord(mes.getLon());
			formatter.format("%010.4f", lon);
			sb.append(',');
			sb.append(mes.getLonLetter());
			sb.append(',');
			formatter.format("%.2f", mes.getSpeed());
			sb.append(',');
			formatter.format("%.2f", mes.getCourse());
			sb.append(',');
			formatter.format("%1$td%1$tm%1$ty", mes.getNavDate());
			sb.append(",,,A");

			int chs = createNmeaChecksum(sb.toString().substring(1));
			sb.append('*');
			formatter.format("%x", chs);
		}

		formatter.close();
		return sb.toString();
	}

	public int createNmeaChecksum(String s) {
		byte[] bytes = s.getBytes(asciiCharset);

		int crc = 0x0;
		for (byte t : bytes) {
			crc = (int) (crc ^ t);
		}
		return crc;
	}

	private double convertToGprmcCoord(Double coord) {
		double degrees = Math.floor(coord);
		double result = degrees * 100 + 60 * (coord - degrees);
		return result;
	}
}

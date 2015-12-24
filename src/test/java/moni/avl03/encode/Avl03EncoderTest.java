package moni.avl03.encode;

import java.util.Calendar;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import moni.avl03.domain.InfoMessage;
import moni.avl03.domain.ProtocolType;

public class Avl03EncoderTest {

	@Test
	public void testEncode() {
		// src:
		// $$B2359772031093009|AA$GPRMC,210006.000,A,5542.2011,N,03741.1583,E,0.00,,111215,,,A*78|01.7|01.0|01.3|000000000000|20151211210006|14291311|00000000|1E5177FE|0000|0.0000|0375|5920
		// dst:
		// $$B6359772031093009|AA$GPRMC,210006.000,A,5542.2012,N,3741.1582,E,0.00,0.00,111215,0,N,A*2A|1.7|1.0|1.3|000000000000|20151211210006|14291311|00000000|1E5177FE|0000|0.0000|0375|||7B3A
		InfoMessage mes = new InfoMessage(ProtocolType.glonass);
		mes.setImei(359772031093009L);
		mes.setAlarmType("AA");
		mes.setState('A');
		mes.setNavDate(new Date());
		mes.setLat(55.70335166666667);
		mes.setLatLetter('N');
		mes.setLon(37.68597166666667);
		mes.setLonLetter('E');
		mes.setSpeed(0);
		mes.setCourse(0);
		Calendar cal = Calendar.getInstance();
		cal.set(2015, 12 - 1, 11, 21, 0, 6);
		cal.set(Calendar.MILLISECOND, 0);
		mes.setNavDate(cal.getTime());
		mes.setPdop(1.7);
		mes.setHdop(1.0);
		mes.setVdop(1.3);
		mes.setStatus("000000000000");
		mes.setRtcDate(cal.getTime());
		mes.setVoltage("14291311");
		mes.setAdc("00000000");
		mes.setLacci("1E5177FE");
		mes.setTemperature("0000");
		mes.setOdometer(0);
		mes.setSerialId(375);

		Avl03Encoder avl03Encoder = new Avl03Encoder();

		String encoded = avl03Encoder.encode(mes);
		//System.out.println(encoded);
		Assert.assertEquals("359772031093009", encoded.substring(4, 19));
		Assert.assertEquals(
				"$$B8359772031093009|AA$GPRMC,210006.000,A,5542.2011,N,03741.1583,E,0.00,0.00,111215,,,A*66|01.7|01.0|01.3|000000000000|20151211210006|14291311|00000000|1E5177FE|0000|0.0000|0375|||2486",
				encoded);
	}

	@Test
	public void testNmeaCheckSum() {
		String s = "GPRMC,210006.000,A,5542.2011,N,03741.1583,E,0.00,,111215,,,A";
		Avl03Encoder avl03Encoder = new Avl03Encoder();

		int a = avl03Encoder.createNmeaChecksum(s);
		Assert.assertEquals(0x78, a);
	}

}

package moni.avl03.income;

import java.nio.charset.Charset;

import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import moni.avl03.domain.DisconnectMessage;
import moni.avl03.domain.InfoMessage;
import moni.avl03.domain.Message;
import moni.avl03.domain.ResponseMessage;
import moni.avl03.encode.Avl03Encoder;
import moni.avl03.state.ChannelKeeper;
import moni.avl03.state.JmsManager;
import moni.avl03.state.NotConnectedException;

public class FromDeviceListener implements MessageListener {
	private static final Logger logger = LoggerFactory.getLogger(FromDeviceListener.class);
	private static final Logger packetsLogger = LoggerFactory.getLogger("packets");
	private static final Logger responsesLogger = LoggerFactory.getLogger("responses");
	private ChannelKeeper channelKeeper;
	private JmsManager jmsManager;
	private Gson gson = new GsonBuilder().setDateFormat("yyyy.MM.dd HH:mm:ss z").create();
	private Avl03Encoder avl03Encoder = new Avl03Encoder();
	private Charset asciiCharset = Charset.forName("ASCII");

	public void setChannelKeeper(ChannelKeeper channelKeeper) {
		this.channelKeeper = channelKeeper;
	}

	public void setJmsManager(JmsManager jmsManager) {
		this.jmsManager = jmsManager;
	}

	@Override
	public void onMessage(javax.jms.Message message) {
		if (!(message instanceof TextMessage)) {
			logger.error("JMS message is not type of TextMessage.");
			return;
		}

		try {
			TextMessage texmMessage = (TextMessage) message;
			String str = texmMessage.getText();
			logger.debug(str);

			Message superMessage = gson.fromJson(str, Message.class);

			switch (superMessage.getProtocolType()) {
			case response:
				responseMessage(str);
				break;

			case disconnect:
				disconnectMessage(str);
				break;

			case glonass:
			case glonassFuel:
			case glonassImpuls:
			case gprmc:
				infoMessage(str);
				break;

			default:
				break;
			}

		} catch (JMSException e) {
			logger.error("JMSException.", e);
		}
	}

	private void infoMessage(String str) {
		InfoMessage mes = gson.fromJson(str, InfoMessage.class);
		String encoded = avl03Encoder.encode(mes);
		packetsLogger.debug(encoded);
		logger.debug(encoded);

		send(mes.getDeviceId(), encoded);
	}

	private void disconnectMessage(String str) {
		DisconnectMessage mes = gson.fromJson(str, DisconnectMessage.class);
		channelKeeper.stopChannel(mes.getDeviceId());
	}

	private void responseMessage(String str) {
		ResponseMessage mes = gson.fromJson(str, ResponseMessage.class);
		responsesLogger.debug(mes.getDeviceId().toString() + " - " + mes.getResponse());
		send(mes.getDeviceId(), mes.getResponse());
	}

	private void send(Long deviceId, String str) {
		byte[] bytes = str.getBytes(asciiCharset);
		try {
			ChannelFuture chf = channelKeeper.writeToChannel(deviceId, bytes);
			chf.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if (!future.isSuccess()) {
						jmsManager.stopJms();
						logger.error("Can't write to out. " + str, future.cause());
					}
				}
			});
		} catch (InterruptedException e) {
		} catch (NotConnectedException e) {
			jmsManager.stopJms();
		}
	}
}

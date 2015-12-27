package moni.avl03.income;

import java.nio.charset.Charset;
import java.util.Date;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import moni.avl03.domain.CommandMessage;
import moni.avl03.domain.ProtocolType;
import moni.avl03.state.ChannelKeeper;

@Sharable
public class ToDeviceHandler extends ChannelHandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(ToDeviceHandler.class);
	private static final Logger commandsLogger = LoggerFactory.getLogger("commands");
	private Gson gson = new GsonBuilder().setDateFormat("yyyy.MM.dd HH:mm:ss z").create();
	private JmsTemplate jmsTemplate;
	private Charset asciiCharset = Charset.forName("ASCII");

	public void setJmsTemplate(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ByteBuf b = (ByteBuf) msg;
		byte[] messageBytes = new byte[b.readableBytes()];
		b.readBytes(messageBytes);
		String s = new String(messageBytes, asciiCharset);

		commandsLogger.info(s);

		Long id = ctx.channel().attr(ChannelKeeper.AK_ID).get();

		CommandMessage cm = new CommandMessage(ProtocolType.command);
		cm.setDate(new Date());
		cm.setCommand(s);
		cm.setDeviceId(id);
		String messageStr = gson.toJson(cm);

		jmsTemplate.send(new MessageCreator() {
			@Override
			public javax.jms.Message createMessage(Session session) throws JMSException {
				TextMessage tm = session.createTextMessage(messageStr);
				return tm;
			}
		});
	}

	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		logger.debug("client: " + ctx.channel().remoteAddress().toString());
	}
}

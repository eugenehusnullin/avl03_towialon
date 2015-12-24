package moni.avl03.income;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import moni.avl03.state.ChannelKeeper;

@Sharable
public class ToDeviceHandler extends ChannelHandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(ToDeviceHandler.class);
	private JmsTemplate jmsTemplate;

	public void setJmsTemplate(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ByteBuf b = (ByteBuf) msg;
		byte[] messageBytes = new byte[b.readableBytes()];
		b.readBytes(messageBytes);
		String s = new String(messageBytes, "ASCII");
		
		logger.info(String.format("%s %s", ctx.channel().remoteAddress(), s));

		Long id = ctx.channel().attr(ChannelKeeper.AK_ID).get();
		JSONObject json = new JSONObject();
		json.put("deviceId", id);
		json.put("command", s);

		jmsTemplate.send(new MessageCreator() {
			@Override
			public javax.jms.Message createMessage(Session session) throws JMSException {
				TextMessage tm = session.createTextMessage(json.toString());
				return tm;
			}
		});

		super.channelRead(ctx, msg);
	}
	
	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		logger.debug("client: " + ctx.channel().remoteAddress().toString());
	}
}

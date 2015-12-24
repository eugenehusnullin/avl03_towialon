package moni.avl03.state;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import moni.avl03.income.ToDeviceHandler;

public class ChannelKeeper {
	private static final Logger logger = LoggerFactory.getLogger(ChannelKeeper.class);

	private String host;
	private int port;
	private ToDeviceHandler toDeviceHandler;

	private EventLoopGroup workerGroup;
	private Bootstrap bootstrap;

	private Map<Long, Channel> channelMap = new HashMap<Long, Channel>();
	public final static AttributeKey<Long> AK_ID = AttributeKey.valueOf("id");

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setToDeviceHandler(ToDeviceHandler toDeviceHandler) {
		this.toDeviceHandler = toDeviceHandler;
	}

	public void init() {
		workerGroup = new NioEventLoopGroup();
		bootstrap = new Bootstrap();
		bootstrap.group(workerGroup)
				.channel(NioSocketChannel.class)
				.option(ChannelOption.SO_KEEPALIVE, true)
				.option(ChannelOption.SO_LINGER, 0)
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(toDeviceHandler);
					}
				})
				.remoteAddress(host, port);
	}

	public void stop() {
		Future<?> fw = workerGroup.shutdownGracefully();
		try {
			fw.await();
		} catch (InterruptedException e) {
			logger.error("Stoping failure.", e);
		}

		channelMap.clear();
	}

	private Channel connect(Long id) throws InterruptedException, NotConnectedException {
		ChannelFuture f = bootstrap.connect();
		f = f.sync();
		if (f.isSuccess()) {
			f.channel().attr(AK_ID).set(id);
			return f.channel();
		} else {
			throw new NotConnectedException();
		}
	}

	public boolean checkConnect() {
		try {
			Channel ch = connect(0L);
			if (ch.isActive()) {
				ch.close().sync();
				return true;
			}
		} catch (InterruptedException | NotConnectedException e) {
		}
		return false;
	}

	public ChannelFuture writeToChannel(Long id, byte[] bytes) throws InterruptedException, NotConnectedException {
		Channel channel = getChannel(id);
		if (channel == null || !channel.isActive()) {
			channel = connect(id);
			putChannel(id, channel);
		}

		ByteBuf b = channel.alloc().buffer(bytes.length);
		b.writeBytes(bytes);
		ChannelFuture chf = channel.write(b);
		channel.flush();
		return chf;
	}

	private Channel getChannel(Long id) {
		synchronized (channelMap) {
			Channel channel = channelMap.get(id);
			channelMap.notifyAll();
			return channel;
		}
	}

	private void putChannel(Long id, Channel channel) {
		synchronized (channelMap) {
			channelMap.put(id, channel);
			channelMap.notifyAll();
		}
	}

	public void stopChannel(Long id) {
		Channel channel = removeChannel(id);
		if (channel != null) {
			channel.disconnect();
		}
	}

	private Channel removeChannel(Long id) {
		synchronized (channelMap) {
			return channelMap.remove(id);
		}
	}
}

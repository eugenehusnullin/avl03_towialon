package moni.avl03.income;

import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.Future;

public class DebugListener extends ChannelHandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(DebugListener.class);

	private String host;
	private int port;

	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	private ServerBootstrap serverBootstrap;
	private ChannelFuture channelFuture;

	private int cnt = 0;
	private Charset asciiCharset = Charset.forName("ASCII");

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ByteBuf b = (ByteBuf) msg;
		byte[] messageBytes = new byte[b.readableBytes()];
		b.readBytes(messageBytes);
		String s = new String(messageBytes, asciiCharset);

		logger.info(ctx.channel().remoteAddress().toString() + ": +++++ arriver: " + s);

		byte[] bytes = Integer.toString(cnt).getBytes(asciiCharset);
		ByteBuf b3 = ctx.alloc().buffer(bytes.length);
		b3.writeBytes(bytes);
		ctx.write(b3);
		ctx.flush();
	}

	public void run() throws InterruptedException {
		bossGroup = new NioEventLoopGroup();
		workerGroup = new NioEventLoopGroup();

		serverBootstrap = new ServerBootstrap();
		serverBootstrap.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new DebugListener());
					}
				})
				.option(ChannelOption.SO_BACKLOG, 128)
				.childOption(ChannelOption.SO_KEEPALIVE, true);

		channelFuture = serverBootstrap.bind(host, port).sync();
		logger.info("Debug socket started, on host - " + host + ", port - " + port + ".");
	}

	public void stop() throws InterruptedException {
		ChannelFuture closeFuture = channelFuture.channel().close();
		closeFuture.sync();

		@SuppressWarnings("rawtypes")
		Future fw = workerGroup.shutdownGracefully();
		@SuppressWarnings("rawtypes")
		Future fb = bossGroup.shutdownGracefully();
		try {
			fb.await();
			fw.await();
		} catch (InterruptedException ignore) {
		}

		logger.info("Debug socket stoped.");
	}
	
	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		logger.info("server: " + ctx.channel().remoteAddress().toString());
	}

}

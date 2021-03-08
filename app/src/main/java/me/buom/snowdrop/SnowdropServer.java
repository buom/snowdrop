package me.buom.snowdrop;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public final class SnowdropServer {

    static final boolean SSL = System.getProperty("ssl") != null;
    static final int PORT = Integer.parseInt(System.getProperty("port", SSL? "8443" : "8080"));
    static final long WORKER_ID = Long.parseLong(System.getProperty("workerId", "-1"));
    static final long EPOCH = 1614929768957L; // 2021-05-03

    public static void main(String[] args) throws Exception {
        // Configure SSL.
        final SslContext sslCtx;
        if (SSL) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            sslCtx = null;
        }

        // sanity check for workerId
        if (WORKER_ID > 1023 || WORKER_ID < 0) {
            throw new IllegalArgumentException("Globally unique workerId between 0 and 1023");
        }

        // Configure the server.
        final int bossThreads = Integer.parseInt(System.getProperty("bossThreads", "1"));
        final int workerThreads = Integer.parseInt(System.getProperty("workerThreads", "1"));

        EventLoopGroup bossGroup = new NioEventLoopGroup(bossThreads);
        EventLoopGroup workerGroup = new NioEventLoopGroup(workerThreads);
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024);
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new SnowdropServerInitializer(sslCtx));

            Channel ch = b.bind(PORT).sync().channel();

            System.err.printf("worker starting [id: %d]\n", WORKER_ID);

            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}

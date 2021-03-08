package me.buom.snowdrop;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

import java.nio.charset.StandardCharsets;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpHeaderValues.*;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static me.buom.snowdrop.SnowdropServer.EPOCH;
import static me.buom.snowdrop.SnowdropServer.WORKER_ID;

@Sharable
public class SnowdropServerHandler extends SimpleChannelInboundHandler<HttpObject> {
    private long sequence = 0L;
    private long lastTs = -1L;

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
        if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;

            boolean keepAlive = HttpUtil.isKeepAlive(req);
            long uid = nextId();
            FullHttpResponse response = new DefaultFullHttpResponse(req.protocolVersion(), OK,
                    Unpooled.wrappedBuffer(String.valueOf(uid).getBytes(StandardCharsets.UTF_8)));
            response.headers()
                    .set(CONTENT_TYPE, TEXT_PLAIN)
                    .setInt(CONTENT_LENGTH, response.content().readableBytes());

            if (keepAlive) {
                if (!req.protocolVersion().isKeepAliveDefault()) {
                    response.headers().set(CONNECTION, KEEP_ALIVE);
                }
            } else {
                // Tell the client we're going to close the connection.
                response.headers().set(CONNECTION, CLOSE);
            }

            ChannelFuture f = ctx.write(response);

            if (!keepAlive) {
                f.addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private synchronized long nextId() {
        long ts = System.currentTimeMillis();
        if (ts < lastTs) {
            throw new RuntimeException(
                    String.format("Clock moved backwards. Refusing to generate id for %d milliseconds", lastTs - ts));
        }

        if (lastTs == ts) {
            sequence = (sequence + 1) & 4095L;
            if (sequence == 0) {
                ts = System.currentTimeMillis();
                while (ts <= lastTs) {
                    ts = System.currentTimeMillis();
                }
            }
        } else {
            sequence = 0;
        }

        lastTs = ts;
        
        return ((lastTs - EPOCH) << 22) | WORKER_ID << 12 | sequence;
    }
}

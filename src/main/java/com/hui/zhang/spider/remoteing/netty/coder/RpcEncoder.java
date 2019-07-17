package com.hui.zhang.spider.remoteing.netty.coder;

import com.hui.zhang.spider.common.serialize.HessianSerialize;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

@ChannelHandler.Sharable
public class RpcEncoder extends MessageToByteEncoder {

	private Class<?> genericClass;

    public RpcEncoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }

    @Override
    public void encode(ChannelHandlerContext ctx, Object in, ByteBuf out) throws Exception {
        if (genericClass.isInstance(in)) {
            //byte[] data = SerializationUtils.serialize(in);
            byte[] data= HessianSerialize.serialize(in);
            out.writeInt(data.length);
            out.writeBytes(data);
        }
    }

}
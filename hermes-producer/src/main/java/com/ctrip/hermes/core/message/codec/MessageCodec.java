package com.ctrip.hermes.core.message.codec;

import io.netty.buffer.ByteBuf;

import com.ctrip.hermes.core.message.DecodedMessage;
import com.ctrip.hermes.core.message.ProducerMessage;

public interface MessageCodec {

	public void encode(ProducerMessage<?> msg, ByteBuf buf);

	public DecodedMessage decode(ByteBuf buf);
}
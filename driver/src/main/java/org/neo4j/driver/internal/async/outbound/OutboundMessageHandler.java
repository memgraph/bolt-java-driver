/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.driver.internal.async.outbound;

import static io.netty.buffer.ByteBufUtil.hexDump;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.util.List;
import java.util.Set;
import org.neo4j.driver.Logger;
import org.neo4j.driver.Logging;
import org.neo4j.driver.internal.async.connection.BoltProtocolUtil;
import org.neo4j.driver.internal.logging.ChannelActivityLogger;
import org.neo4j.driver.internal.messaging.BoltPatchesListener;
import org.neo4j.driver.internal.messaging.Message;
import org.neo4j.driver.internal.messaging.MessageFormat;

public class OutboundMessageHandler extends MessageToMessageEncoder<Message> implements BoltPatchesListener {
    public static final String NAME = OutboundMessageHandler.class.getSimpleName();
    private final ChunkAwareByteBufOutput output;
    private final MessageFormat messageFormat;
    private final Logging logging;

    private MessageFormat.Writer writer;
    private Logger log;

    public OutboundMessageHandler(MessageFormat messageFormat, Logging logging) {
        this.output = new ChunkAwareByteBufOutput();
        this.messageFormat = messageFormat;
        this.logging = logging;
        this.writer = messageFormat.newWriter(output);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        log = new ChannelActivityLogger(ctx.channel(), logging, getClass());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        log = null;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, List<Object> out) {
        log.debug("C: %s", msg);

        ByteBuf messageBuf = ctx.alloc().ioBuffer();
        output.start(messageBuf);
        try {
            writer.write(msg);
            output.stop();
        } catch (Throwable error) {
            output.stop();
            // release buffer because it will not get added to the out list and no other handler is going to handle it
            messageBuf.release();
            throw new EncoderException("Failed to write outbound message: " + msg, error);
        }

        if (log.isTraceEnabled()) {
            log.trace("C: %s", hexDump(messageBuf));
        }

        BoltProtocolUtil.writeMessageBoundary(messageBuf);
        out.add(messageBuf);
    }

    @Override
    public void handle(Set<String> patches) {
        if (patches.contains(DATE_TIME_UTC_PATCH)) {
            messageFormat.enableDateTimeUtc();
            writer = messageFormat.newWriter(output);
        }
    }
}

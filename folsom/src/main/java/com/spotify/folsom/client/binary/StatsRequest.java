package com.spotify.folsom.client.binary;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.spotify.folsom.MemcachedStats;
import com.spotify.folsom.client.AllRequest;
import com.spotify.folsom.client.OpCode;
import com.spotify.folsom.client.Request;
import com.spotify.folsom.guava.HostAndPort;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public class StatsRequest extends BinaryRequest<Map<String, MemcachedStats>>
    implements AllRequest<Map<String, MemcachedStats>> {

  public static final byte[] NO_KEY = new byte[0];

  public StatsRequest() {
    super(NO_KEY);
  }

  @Override
  public ByteBuf writeRequest(final ByteBufAllocator alloc, final ByteBuffer dst) {
    writeHeader(dst, OpCode.STAT, 0, 0, 0);

    return toBuffer(alloc, dst);
  }

  @Override
  public void handle(final BinaryResponse replies, final HostAndPort server) throws IOException {
    final Map<String, String> stats = Maps.newHashMap();
    final int expectedOpaque = opaque;
    for (final ResponsePacket reply : replies) {
      if (OpCode.getKind(reply.opcode) != OpCode.STAT) {
        throw new IOException("Unmatched response");
      }
      final int opaque = reply.opaque;
      if (opaque != expectedOpaque) {
        throw new IOException("messages out of order for " + getClass().getSimpleName());
      }
      for (ResponsePacket responsePacket : replies) {
        final String name = new String(responsePacket.key, Charsets.US_ASCII);
        final String value = new String(responsePacket.value, Charsets.US_ASCII);
        stats.put(name, value);
      }
    }
    succeed(
        ImmutableMap.of(server.getHostText() + ":" + server.getPort(), new MemcachedStats(stats)));
  }

  @Override
  public Map<String, MemcachedStats> merge(List<Map<String, MemcachedStats>> results) {
    return AllRequest.mergeStats(results);
  }

  @Override
  public Request<Map<String, MemcachedStats>> duplicate() {
    return new StatsRequest();
  }
}

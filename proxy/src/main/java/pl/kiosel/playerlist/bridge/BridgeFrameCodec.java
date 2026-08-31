package pl.kiosel.playerlist.bridge;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class BridgeFrameCodec {

    private static final int FRAME_MAGIC = 0x41504C46;
    private static final int MAX_PLUGIN_MESSAGE = 30_000;
    private static final int FRAME_HEADER_SIZE = 30;
    private static final long ASSEMBLY_TIMEOUT_MILLIS = 30_000L;
    private static final AtomicLong MESSAGE_IDS = new AtomicLong(System.nanoTime());

    private BridgeFrameCodec() {
    }

    public static List<byte[]> frame(byte[] packet) throws IOException {
        if (packet == null || packet.length == 0 || packet.length > BridgeProtocol.MAX_PACKET_SIZE) {
            throw new IOException("Invalid bridge packet size");
        }
        int chunkSize = MAX_PLUGIN_MESSAGE - FRAME_HEADER_SIZE;
        int count = (packet.length + chunkSize - 1) / chunkSize;
        long messageId = MESSAGE_IDS.incrementAndGet();
        List<byte[]> frames = new ArrayList<>(count);

        for (int index = 0; index < count; index++) {
            int offset = index * chunkSize;
            int length = Math.min(chunkSize, packet.length - offset);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(FRAME_HEADER_SIZE + length);
            DataOutputStream output = new DataOutputStream(bytes);
            output.writeInt(FRAME_MAGIC);
            output.writeShort(BridgeProtocol.VERSION);
            output.writeLong(messageId);
            output.writeInt(index);
            output.writeInt(count);
            output.writeInt(packet.length);
            output.writeInt(length);
            output.write(packet, offset, length);
            output.flush();
            frames.add(bytes.toByteArray());
        }
        return frames;
    }

    public static final class Reassembler {
        private final Map<String, Assembly> assemblies = new HashMap<>();

        public synchronized byte[] accept(String source, byte[] frame) throws IOException {
            cleanup();
            if (frame == null || frame.length < FRAME_HEADER_SIZE || frame.length > MAX_PLUGIN_MESSAGE) {
                throw new IOException("Invalid bridge frame size");
            }
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(frame));
            if (input.readInt() != FRAME_MAGIC) {
                throw new IOException("Invalid bridge frame magic");
            }
            int version = input.readUnsignedShort();
            if (version != BridgeProtocol.VERSION) {
                throw new IOException("Unsupported bridge frame version " + version);
            }
            long messageId = input.readLong();
            int index = input.readInt();
            int count = input.readInt();
            int totalLength = input.readInt();
            int chunkLength = input.readInt();
            if (count > 1_000 || index < 0 || index >= count
					|| totalLength < 1 || totalLength > BridgeProtocol.MAX_PACKET_SIZE
					|| chunkLength < 0 || chunkLength != input.available()) {
                throw new IOException("Invalid bridge frame metadata");
            }

            String key = String.valueOf(source) + ':' + messageId;
            Assembly assembly = assemblies.get(key);
            if (assembly == null) {
                assembly = new Assembly(count, totalLength);
                assemblies.put(key, assembly);
            } else if (assembly.parts.length != count || assembly.totalLength != totalLength) {
                assemblies.remove(key);
                throw new IOException("Conflicting bridge frame metadata");
            }

            if (assembly.parts[index] == null) {
                byte[] chunk = new byte[chunkLength];
                input.readFully(chunk);
                assembly.parts[index] = chunk;
                assembly.received++;
            }
            if (assembly.received != count) {
                return null;
            }

            ByteArrayOutputStream packet = new ByteArrayOutputStream(totalLength);
            for (byte[] part : assembly.parts) {
                if (part == null) {
                    throw new EOFException("Missing bridge frame");
                }
                packet.write(part);
            }
            assemblies.remove(key);
            if (packet.size() != totalLength) {
                throw new IOException("Invalid reassembled bridge packet size");
            }
            return packet.toByteArray();
        }

        private void cleanup() {
            long expired = System.currentTimeMillis() - ASSEMBLY_TIMEOUT_MILLIS;
            assemblies.entrySet().removeIf(entry -> entry.getValue().createdAt < expired);
        }
    }

    private static final class Assembly {
        private final byte[][] parts;
        private final int totalLength;
        private final long createdAt = System.currentTimeMillis();
        private int received;

        private Assembly(int count, int totalLength) {
            this.parts = new byte[count][];
            this.totalLength = totalLength;
        }
    }
}

package pl.kiosel.playerlist.bridge;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class BridgeProtocol {

    public static final String MODERN_CHANNEL = "advancedplayerlist:bridge";
    public static final String LEGACY_CHANNEL = "APLBridge";
    public static final int VERSION = 1;

    public static final int SNAPSHOT = 1;
    public static final int NETWORK_STATE = 2;
    public static final int REMOTE_REQUEST = 3;
    public static final int REMOTE_RESPONSE = 4;

    private static final int PACKET_MAGIC = 0x41504C50;
    static final int MAX_PACKET_SIZE = 4 * 1024 * 1024;
    private static final int MAX_STRING_SIZE = 32_767;
    private static final int MAX_ENTRIES = 10_000;

    private BridgeProtocol() {
    }

    public static byte[] encodeSnapshot(BridgeMessages.Snapshot snapshot) throws IOException {
        PacketWriter writer = writer(SNAPSHOT);
        writer.output.writeInt(snapshot.getMaxPlayers());
        writeWorlds(writer.output, snapshot.getWorlds());
        return writer.finish();
    }

    public static BridgeMessages.Snapshot decodeSnapshot(byte[] packet) throws IOException {
        DataInputStream input = reader(packet, SNAPSHOT);
        BridgeMessages.Snapshot snapshot = new BridgeMessages.Snapshot(input.readInt(), readWorlds(input));
        requireFinished(input);
        return snapshot;
    }

    public static byte[] encodeNetworkState(BridgeMessages.NetworkState state) throws IOException {
        PacketWriter writer = writer(NETWORK_STATE);
        writeSize(writer.output, state.getServers().size());
        for (BridgeMessages.ServerState server : state.getServers()) {
            writeString(writer.output, server.getName());
            writer.output.writeBoolean(server.isOnline());
            writer.output.writeInt(server.getPlayerCount());
            writer.output.writeInt(server.getMaxPlayers());
            writeWorlds(writer.output, server.getWorlds());
        }
        writeSize(writer.output, state.getPlayers().size());
        for (BridgeMessages.PlayerData player : state.getPlayers()) {
            writeString(writer.output, player.getName());
            writeString(writer.output, player.getUniqueId());
            writeString(writer.output, player.getServer());
            writer.output.writeInt(player.getPing());
        }
        return writer.finish();
    }

    public static BridgeMessages.NetworkState decodeNetworkState(byte[] packet) throws IOException {
        DataInputStream input = reader(packet, NETWORK_STATE);
        int serverCount = readSize(input);
        List<BridgeMessages.ServerState> servers = new ArrayList<>(serverCount);
        for (int index = 0; index < serverCount; index++) {
            servers.add(new BridgeMessages.ServerState(
                    readString(input), input.readBoolean(), input.readInt(), input.readInt(), readWorlds(input)));
        }
        int playerCount = readSize(input);
        List<BridgeMessages.PlayerData> players = new ArrayList<>(playerCount);
        for (int index = 0; index < playerCount; index++) {
            players.add(new BridgeMessages.PlayerData(
                    readString(input), readString(input), readString(input), input.readInt()));
        }
        requireFinished(input);
        return new BridgeMessages.NetworkState(servers, players);
    }

    public static byte[] encodeRemoteRequest(BridgeMessages.RemoteRequest request) throws IOException {
        PacketWriter writer = writer(REMOTE_REQUEST);
        writeString(writer.output, request.getRequestId());
        writeString(writer.output, request.getTargetServer());
        writeString(writer.output, request.getHandler());
        return writer.finish();
    }

    public static BridgeMessages.RemoteRequest decodeRemoteRequest(byte[] packet) throws IOException {
        DataInputStream input = reader(packet, REMOTE_REQUEST);
        BridgeMessages.RemoteRequest request = new BridgeMessages.RemoteRequest(
                readString(input), readString(input), readString(input));
        requireFinished(input);
        return request;
    }

    public static byte[] encodeRemoteResponse(BridgeMessages.RemoteResponse response) throws IOException {
        PacketWriter writer = writer(REMOTE_RESPONSE);
        writeString(writer.output, response.getRequestId());
        writeString(writer.output, response.getError());
        writeSize(writer.output, response.getLines().size());
        for (BridgeMessages.RemoteLine line : response.getLines()) {
            writeString(writer.output, line.getText());
            writeString(writer.output, line.getPing());
            writeString(writer.output, line.getSkin());
            writeString(writer.output, line.getOpacity());
        }
        return writer.finish();
    }

    public static BridgeMessages.RemoteResponse decodeRemoteResponse(byte[] packet) throws IOException {
        DataInputStream input = reader(packet, REMOTE_RESPONSE);
        String requestId = readString(input);
        String error = readString(input);
        int size = readSize(input);
        List<BridgeMessages.RemoteLine> lines = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            lines.add(new BridgeMessages.RemoteLine(
                    readString(input), readString(input), readString(input), readString(input)));
        }
        requireFinished(input);
        return new BridgeMessages.RemoteResponse(requestId, error, lines);
    }

    public static int packetType(byte[] packet) throws IOException {
        return readHeader(new DataInputStream(new ByteArrayInputStream(packet)));
    }

    private static PacketWriter writer(int type) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        output.writeInt(PACKET_MAGIC);
        output.writeShort(VERSION);
        output.writeByte(type);
        return new PacketWriter(bytes, output);
    }

    private static DataInputStream reader(byte[] packet, int expectedType) throws IOException {
        if (packet == null || packet.length > MAX_PACKET_SIZE) {
            throw new IOException("Invalid bridge packet size");
        }
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(packet));
        int type = readHeader(input);
        if (type != expectedType) {
            throw new IOException("Unexpected bridge packet type " + type);
        }
        return input;
    }

    private static int readHeader(DataInputStream input) throws IOException {
        if (input.readInt() != PACKET_MAGIC) {
            throw new IOException("Invalid bridge packet magic");
        }
        int version = input.readUnsignedShort();
        if (version != VERSION) {
            throw new IOException("Unsupported bridge protocol version " + version);
        }
        return input.readUnsignedByte();
    }

    private static void writeWorlds(DataOutputStream output, List<BridgeMessages.WorldData> worlds)
            throws IOException {
        writeSize(output, worlds.size());
        for (BridgeMessages.WorldData world : worlds) {
            writeString(output, world.getName());
            output.writeInt(world.getPlayerCount());
        }
    }

    private static List<BridgeMessages.WorldData> readWorlds(DataInputStream input) throws IOException {
        int size = readSize(input);
        List<BridgeMessages.WorldData> worlds = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            worlds.add(new BridgeMessages.WorldData(readString(input), input.readInt()));
        }
        return worlds;
    }

    private static void writeSize(DataOutputStream output, int size) throws IOException {
        if (size < 0 || size > MAX_ENTRIES) {
            throw new IOException("Invalid bridge collection size " + size);
        }
        output.writeInt(size);
    }

    private static int readSize(DataInputStream input) throws IOException {
        int size = input.readInt();
        if (size < 0 || size > MAX_ENTRIES) {
            throw new IOException("Invalid bridge collection size " + size);
        }
        return size;
    }

    private static void writeString(DataOutputStream output, String value) throws IOException {
        if (value == null) {
            output.writeInt(-1);
            return;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_STRING_SIZE) {
            throw new IOException("Bridge string is too long");
        }
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    private static String readString(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length == -1) {
            return null;
        }
        if (length < 0 || length > MAX_STRING_SIZE) {
            throw new IOException("Invalid bridge string size " + length);
        }
        byte[] bytes = new byte[length];
        input.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void requireFinished(DataInputStream input) throws IOException {
        if (input.read() != -1) {
            throw new IOException("Bridge packet contains trailing data");
        }
    }

    private static final class PacketWriter {
        private final ByteArrayOutputStream bytes;
        private final DataOutputStream output;

        private PacketWriter(ByteArrayOutputStream bytes, DataOutputStream output) {
            this.bytes = bytes;
            this.output = output;
        }

        private byte[] finish() throws IOException {
            output.flush();
            byte[] packet = bytes.toByteArray();
            if (packet.length > MAX_PACKET_SIZE) {
                throw new IOException("Bridge packet exceeds the size limit");
            }
            return packet;
        }
    }
}

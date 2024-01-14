package org.yirankuma.npcskin;

public class EncodedPacketData {

    private final int packetId;
    private final byte[] data;

    public EncodedPacketData(int packetId, byte[] data) {
        this.packetId = packetId;
        this.data = data;
    }

    public int getPacketId() {
        return packetId;
    }

    public byte[] getData() {
        return data;
    }
}

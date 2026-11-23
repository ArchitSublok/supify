package com.supify.io;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Reads and writes binary audit records framed with a magic SPFY header. */
public class AuditByteStreamHandler {
    private static final int MAGIC_HEADER = 0x53504659; // "SPFY"

    public record BinaryAuditRecord(long timestamp, String action, String payload) {}

    public static void appendBinaryRecord(File file, String action, String payload) throws IOException {
        boolean isNewFile = !file.exists() || file.length() == 0;

        try (FileOutputStream fos = new FileOutputStream(file, true);
             DataOutputStream dos = new DataOutputStream(fos)) {

            if (isNewFile) {
                dos.writeInt(MAGIC_HEADER);
            }

            dos.writeLong(System.currentTimeMillis());
            byte[] actionBytes = action.getBytes(StandardCharsets.UTF_8);
            dos.writeShort(actionBytes.length);
            dos.write(actionBytes);

            byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
            dos.writeInt(payloadBytes.length);
            dos.write(payloadBytes);

            dos.flush();
        }
    }

    public static List<BinaryAuditRecord> readBinaryRecords(File file) throws IOException {
        List<BinaryAuditRecord> records = new ArrayList<>();
        if (!file.exists()) return records;

        try (FileInputStream fis = new FileInputStream(file);
             DataInputStream dis = new DataInputStream(fis)) {

            int magic = dis.readInt();
            if (magic != MAGIC_HEADER) {
                throw new IOException("Invalid binary audit file: header mismatch (0x" + Integer.toHexString(magic) + ")");
            }

            while (dis.available() > 0) {
                long timestamp = dis.readLong();
                short actionLen = dis.readShort();
                byte[] actionBytes = new byte[actionLen];
                dis.readFully(actionBytes);
                String action = new String(actionBytes, StandardCharsets.UTF_8);

                int payloadLen = dis.readInt();
                byte[] payloadBytes = new byte[payloadLen];
                dis.readFully(payloadBytes);
                String payload = new String(payloadBytes, StandardCharsets.UTF_8);

                records.add(new BinaryAuditRecord(timestamp, action, payload));
            }
        }
        return records;
    }
}

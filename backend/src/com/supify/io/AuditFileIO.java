package com.supify.io;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Byte stream (FileOutputStream/FileInputStream) audit log and StreamTokenizer rule parser. */
public class AuditFileIO {

    public static void appendAuditByteLog(File file, String eventText) throws IOException {
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        String line = "[" + LocalDateTime.now().withNano(0) + "] " + eventText + System.lineSeparator();
        try (FileOutputStream fos = new FileOutputStream(file, true)) {
            fos.write(line.getBytes(StandardCharsets.UTF_8));
        }
    }

    public static String readAuditByteLog(File file) throws IOException {
        if (!file.exists()) {
            return "(Audit file is empty)";
        }
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toString(StandardCharsets.UTF_8);
        }
    }

    public static List<String> tokenizeRule(String ruleExpression) throws IOException {
        StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(ruleExpression));
        tokenizer.wordChars('_', '_');
        tokenizer.wordChars('-', '-');
        tokenizer.ordinaryChar('>');
        tokenizer.ordinaryChar('<');
        tokenizer.ordinaryChar('=');

        List<String> tokens = new ArrayList<>();
        int tokenType;
        while ((tokenType = tokenizer.nextToken()) != StreamTokenizer.TT_EOF) {
            if (tokenType == StreamTokenizer.TT_WORD) {
                tokens.add("WORD(" + tokenizer.sval + ")");
            } else if (tokenType == StreamTokenizer.TT_NUMBER) {
                tokens.add("NUM(" + tokenizer.nval + ")");
            } else {
                tokens.add("OP('" + (char) tokenType + "')");
            }
        }
        return tokens;
    }
}

package com.ask.ats.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.DeflaterOutputStream;

public class CompressionUtil {
    public static String compress(String data) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(byteArrayOutputStream)) {
            deflaterOutputStream.write(data.getBytes());
        }
        return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
    }
}

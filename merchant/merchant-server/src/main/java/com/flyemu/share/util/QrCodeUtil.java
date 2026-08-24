package com.flyemu.share.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

public class QrCodeUtil {

    public static byte[] toPngBytes(String text, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size,
                    Map.of(EncodeHintType.MARGIN, 1));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", bos);
            return bos.toByteArray();
        } catch (WriterException | IOException e) {
            throw new RuntimeException("生成二维码失败: " + e.getMessage(), e);
        }
    }
}

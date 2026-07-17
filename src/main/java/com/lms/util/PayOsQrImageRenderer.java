package com.lms.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.lms.exception.DataProcessingException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class PayOsQrImageRenderer {
    private static final int QR_SIZE = 360;
    private static final int SYSTEM_BROWN = 0xFF4A3B32;
    private static final int WHITE = 0xFFFFFFFF;

    private PayOsQrImageRenderer() {
    }

    public static byte[] render(String qrContent, String unsupportedPngMessage, String renderFailedMessage) {
        try {
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name(),
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H,
                    EncodeHintType.MARGIN, 2
            );
            BitMatrix matrix = new MultiFormatWriter().encode(
                    qrContent, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);
            BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < matrix.getWidth(); x++) {
                for (int y = 0; y < matrix.getHeight(); y++) {
                    image.setRGB(x, y, matrix.get(x, y) ? SYSTEM_BROWN : WHITE);
                }
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (!ImageIO.write(image, "PNG", output)) {
                throw new DataProcessingException(unsupportedPngMessage);
            }
            return output.toByteArray();
        } catch (WriterException | IOException ex) {
            throw new DataProcessingException(renderFailedMessage, ex);
        }
    }
}

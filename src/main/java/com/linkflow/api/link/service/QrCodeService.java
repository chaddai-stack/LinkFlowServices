package com.linkflow.api.link.service;

import com.linkflow.api.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

@Service
public class QrCodeService {

    private static final int VERSION = 6;
    private static final int SIZE = 17 + VERSION * 4;
    private static final int DATA_CODEWORDS = 136;
    private static final int DATA_CODEWORDS_PER_BLOCK = 68;
    private static final int ECC_CODEWORDS_PER_BLOCK = 18;
    private static final int TOTAL_CODEWORDS = 172;
    private static final int QUIET_ZONE = 4;
    private static final int MASK = 0;

    public byte[] generatePng(String value, int imageSize) {
        byte[] payload = value.getBytes(StandardCharsets.UTF_8);
        if (payload.length > 134) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "QR_PAYLOAD_TOO_LONG",
                    "Short URL is too long to encode in the QR code.",
                    Map.of("max_bytes", 134, "actual_bytes", payload.length)
            );
        }

        boolean[][] modules = encode(payload);
        return renderPng(modules, imageSize);
    }

    private boolean[][] encode(byte[] payload) {
        boolean[][] modules = new boolean[SIZE][SIZE];
        boolean[][] functionModules = new boolean[SIZE][SIZE];

        drawFunctionPatterns(modules, functionModules);
        byte[] codewords = addErrorCorrection(encodeDataCodewords(payload));
        drawCodewords(modules, functionModules, codewords);
        return modules;
    }

    private void drawFunctionPatterns(boolean[][] modules, boolean[][] functionModules) {
        drawFinderPattern(modules, functionModules, 3, 3);
        drawFinderPattern(modules, functionModules, SIZE - 4, 3);
        drawFinderPattern(modules, functionModules, 3, SIZE - 4);

        for (int i = 8; i < SIZE - 8; i++) {
            setFunctionModule(modules, functionModules, 6, i, i % 2 == 0);
            setFunctionModule(modules, functionModules, i, 6, i % 2 == 0);
        }

        drawAlignmentPattern(modules, functionModules, 34, 34);
        drawFormatBits(modules, functionModules);
    }

    private void drawFinderPattern(boolean[][] modules, boolean[][] functionModules, int centerX, int centerY) {
        for (int dy = -4; dy <= 4; dy++) {
            for (int dx = -4; dx <= 4; dx++) {
                int x = centerX + dx;
                int y = centerY + dy;
                if (x < 0 || x >= SIZE || y < 0 || y >= SIZE) {
                    continue;
                }

                int distance = Math.max(Math.abs(dx), Math.abs(dy));
                setFunctionModule(modules, functionModules, x, y, distance != 2 && distance != 4);
            }
        }
    }

    private void drawAlignmentPattern(boolean[][] modules, boolean[][] functionModules, int centerX, int centerY) {
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                int distance = Math.max(Math.abs(dx), Math.abs(dy));
                setFunctionModule(modules, functionModules, centerX + dx, centerY + dy, distance != 1);
            }
        }
    }

    private void drawFormatBits(boolean[][] modules, boolean[][] functionModules) {
        int bits = getFormatBits();

        for (int i = 0; i <= 5; i++) {
            setFunctionModule(modules, functionModules, 8, i, bit(bits, i));
        }
        setFunctionModule(modules, functionModules, 8, 7, bit(bits, 6));
        setFunctionModule(modules, functionModules, 8, 8, bit(bits, 7));
        setFunctionModule(modules, functionModules, 7, 8, bit(bits, 8));
        for (int i = 9; i < 15; i++) {
            setFunctionModule(modules, functionModules, 14 - i, 8, bit(bits, i));
        }

        for (int i = 0; i < 8; i++) {
            setFunctionModule(modules, functionModules, SIZE - 1 - i, 8, bit(bits, i));
        }
        for (int i = 8; i < 15; i++) {
            setFunctionModule(modules, functionModules, 8, SIZE - 15 + i, bit(bits, i));
        }
        setFunctionModule(modules, functionModules, 8, SIZE - 8, true);
    }

    private int getFormatBits() {
        int data = (1 << 3) | MASK;
        int remainder = data;
        for (int i = 0; i < 10; i++) {
            remainder = (remainder << 1) ^ (((remainder >>> 9) & 1) * 0x537);
        }
        return ((data << 10) | remainder) ^ 0x5412;
    }

    private byte[] encodeDataCodewords(byte[] payload) {
        BitBuffer bits = new BitBuffer();
        bits.append(0b0100, 4);
        bits.append(payload.length, 8);
        for (byte b : payload) {
            bits.append(b & 0xFF, 8);
        }

        int capacityBits = DATA_CODEWORDS * 8;
        bits.append(0, Math.min(4, capacityBits - bits.length()));
        while (bits.length() % 8 != 0) {
            bits.append(0, 1);
        }

        byte[] result = bits.toByteArray(DATA_CODEWORDS);
        int index = bits.length() / 8;
        for (int pad = 0xEC; index < result.length; index++, pad ^= 0xEC ^ 0x11) {
            result[index] = (byte) pad;
        }
        return result;
    }

    private byte[] addErrorCorrection(byte[] dataCodewords) {
        byte[][] dataBlocks = new byte[2][DATA_CODEWORDS_PER_BLOCK];
        byte[][] eccBlocks = new byte[2][ECC_CODEWORDS_PER_BLOCK];
        byte[] generator = reedSolomonGenerator(ECC_CODEWORDS_PER_BLOCK);

        for (int block = 0; block < 2; block++) {
            System.arraycopy(dataCodewords, block * DATA_CODEWORDS_PER_BLOCK, dataBlocks[block], 0, DATA_CODEWORDS_PER_BLOCK);
            eccBlocks[block] = reedSolomonRemainder(dataBlocks[block], generator);
        }

        byte[] result = new byte[TOTAL_CODEWORDS];
        int index = 0;
        for (int i = 0; i < DATA_CODEWORDS_PER_BLOCK; i++) {
            for (int block = 0; block < 2; block++) {
                result[index++] = dataBlocks[block][i];
            }
        }
        for (int i = 0; i < ECC_CODEWORDS_PER_BLOCK; i++) {
            for (int block = 0; block < 2; block++) {
                result[index++] = eccBlocks[block][i];
            }
        }
        return result;
    }

    private void drawCodewords(boolean[][] modules, boolean[][] functionModules, byte[] codewords) {
        int bitIndex = 0;
        for (int right = SIZE - 1; right >= 1; right -= 2) {
            if (right == 6) {
                right = 5;
            }

            for (int vert = 0; vert < SIZE; vert++) {
                for (int j = 0; j < 2; j++) {
                    int x = right - j;
                    boolean upward = ((right + 1) & 2) == 0;
                    int y = upward ? SIZE - 1 - vert : vert;
                    if (functionModules[y][x]) {
                        continue;
                    }

                    boolean dark = false;
                    if (bitIndex < codewords.length * 8) {
                        dark = ((codewords[bitIndex >>> 3] >>> (7 - (bitIndex & 7))) & 1) != 0;
                        bitIndex++;
                    }
                    if ((x + y) % 2 == 0) {
                        dark = !dark;
                    }
                    modules[y][x] = dark;
                }
            }
        }
    }

    private byte[] reedSolomonGenerator(int degree) {
        byte[] result = new byte[degree];
        result[degree - 1] = 1;
        int root = 1;
        for (int i = 0; i < degree; i++) {
            for (int j = 0; j < degree; j++) {
                result[j] = (byte) multiply(result[j] & 0xFF, root);
                if (j + 1 < degree) {
                    result[j] ^= result[j + 1];
                }
            }
            root = multiply(root, 0x02);
        }
        return result;
    }

    private byte[] reedSolomonRemainder(byte[] data, byte[] generator) {
        byte[] result = new byte[generator.length];
        for (byte value : data) {
            int factor = (value ^ result[0]) & 0xFF;
            System.arraycopy(result, 1, result, 0, result.length - 1);
            result[result.length - 1] = 0;
            for (int i = 0; i < result.length; i++) {
                result[i] ^= (byte) multiply(generator[i] & 0xFF, factor);
            }
        }
        return result;
    }

    private int multiply(int x, int y) {
        int result = 0;
        for (int i = 7; i >= 0; i--) {
            result = (result << 1) ^ ((result >>> 7) * 0x11D);
            result ^= ((y >>> i) & 1) * x;
        }
        return result & 0xFF;
    }

    private byte[] renderPng(boolean[][] modules, int imageSize) {
        BufferedImage image = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, imageSize, imageSize);
            graphics.setColor(Color.BLACK);

            int qrModules = SIZE + QUIET_ZONE * 2;
            double scale = (double) imageSize / qrModules;
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    if (!modules[y][x]) {
                        continue;
                    }
                    int left = (int) Math.floor((x + QUIET_ZONE) * scale);
                    int top = (int) Math.floor((y + QUIET_ZONE) * scale);
                    int right = (int) Math.ceil((x + QUIET_ZONE + 1) * scale);
                    int bottom = (int) Math.ceil((y + QUIET_ZONE + 1) * scale);
                    graphics.fillRect(left, top, right - left, bottom - top);
                }
            }
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("failed to render QR code", ex);
        }
    }

    private void setFunctionModule(boolean[][] modules, boolean[][] functionModules, int x, int y, boolean dark) {
        modules[y][x] = dark;
        functionModules[y][x] = true;
    }

    private boolean bit(int value, int index) {
        return ((value >>> index) & 1) != 0;
    }

    private static final class BitBuffer {
        private boolean[] bits = new boolean[128];
        private int length;

        void append(int value, int bitCount) {
            if (bitCount < 0 || bitCount > 31 || (value >>> bitCount) != 0) {
                throw new IllegalArgumentException("value does not fit in bitCount");
            }
            ensureCapacity(length + bitCount);
            for (int i = bitCount - 1; i >= 0; i--) {
                bits[length++] = ((value >>> i) & 1) != 0;
            }
        }

        int length() {
            return length;
        }

        byte[] toByteArray(int byteCount) {
            byte[] result = new byte[byteCount];
            for (int i = 0; i < length; i++) {
                if (bits[i]) {
                    result[i >>> 3] |= (byte) (1 << (7 - (i & 7)));
                }
            }
            return result;
        }

        private void ensureCapacity(int capacity) {
            if (capacity > bits.length) {
                bits = Arrays.copyOf(bits, Math.max(capacity, bits.length * 2));
            }
        }
    }
}

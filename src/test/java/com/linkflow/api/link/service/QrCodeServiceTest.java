package com.linkflow.api.link.service;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QrCodeServiceTest {

    @Test
    void generatePngReturnsReadableImageAtRequestedSize() throws Exception {
        QrCodeService service = new QrCodeService();

        byte[] png = service.generatePng("http://localhost:8080/r/promo2026", 256);

        assertTrue(png.length > 100);
        assertEquals((byte) 0x89, png[0]);
        assertEquals(0x50, png[1]);
        assertEquals(0x4E, png[2]);
        assertEquals(0x47, png[3]);

        var image = ImageIO.read(new ByteArrayInputStream(png));
        assertNotNull(image);
        assertEquals(256, image.getWidth());
        assertEquals(256, image.getHeight());
    }
}

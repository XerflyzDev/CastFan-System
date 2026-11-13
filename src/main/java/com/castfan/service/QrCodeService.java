package com.castfan.service;

import java.io.ByteArrayOutputStream;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

@Service
public class QrCodeService {
	public byte[] png(String value, int size) {
		try {
			var matrix = new QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, size, size);
			var output = new ByteArrayOutputStream();
			MatrixToImageWriter.writeToStream(matrix, "PNG", output);
			return output.toByteArray();
		} catch (Exception exception) {
			throw new IllegalStateException("Unable to generate store QR code.", exception);
		}
	}
}

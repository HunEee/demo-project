package com.example.authapp.domain.mfa.service;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;

@Service
public class TotpService {

    private static final String ISSUER = "AuthApp";

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());

    public String generateSecret() {
        return secretGenerator.generate();
    }

    public boolean verify(String secret, String code) {
        if (secret == null || secret.isBlank() || code == null || code.isBlank()) {
            return false;
        }
        return codeVerifier.isValidCode(secret, code.trim());
    }

    public String otpAuthUri(String username, String secret) {
        String label = encodePathSegment(ISSUER) + ":" + encodePathSegment(username);
        return "otpauth://totp/" + label
                + "?secret=" + encodeQueryValue(secret)
                + "&issuer=" + encodeQueryValue(ISSUER);
    }

    public String qrCodeDataUri(String otpAuthUri) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(otpAuthUri, BarcodeFormat.QR_CODE, 240, 240);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate MFA QR code.", e);
        }
    }

    private String encodeQueryValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}

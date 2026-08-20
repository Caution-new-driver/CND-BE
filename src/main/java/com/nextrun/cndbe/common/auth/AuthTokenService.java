package com.nextrun.cndbe.common.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// MCM 관계자 전용 공유 비밀번호 검증 + 세션 토큰 발급/검증.
// 세션을 DB에 저장하지 않고, 토큰 자체에 만료시각+서명을 담아 서버가 그때그때 서명만 재검증한다.
@Component
public class AuthTokenService {

	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final long TOKEN_VALID_DAYS = 1;

	private final String accessPassword;
	private final SecretKeySpec signingKey;

	public AuthTokenService(
			@Value("${app.auth.access-password}") String accessPassword,
			@Value("${app.auth.session-secret}") String sessionSecret) {
		this.accessPassword = accessPassword;
		this.signingKey = new SecretKeySpec(sessionSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
	}

	public boolean isCorrectPassword(String password) {
		if (password == null) {
			return false;
		}
		return MessageDigest.isEqual(
				password.getBytes(StandardCharsets.UTF_8),
				accessPassword.getBytes(StandardCharsets.UTF_8));
	}

	public String issueToken() {
		long expiry = Instant.now().plus(TOKEN_VALID_DAYS, ChronoUnit.DAYS).getEpochSecond();
		String payload = String.valueOf(expiry);
		return payload + "." + sign(payload);
	}

	public boolean isValidToken(String token) {
		if (token == null) {
			return false;
		}
		String[] parts = token.split("\\.", 2);
		if (parts.length != 2) {
			return false;
		}
		long expiry;
		try {
			expiry = Long.parseLong(parts[0]);
		} catch (NumberFormatException e) {
			return false;
		}
		if (Instant.now().getEpochSecond() > expiry) {
			return false;
		}
		String expectedSignature = sign(parts[0]);
		return MessageDigest.isEqual(
				expectedSignature.getBytes(StandardCharsets.UTF_8),
				parts[1].getBytes(StandardCharsets.UTF_8));
	}

	private String sign(String payload) {
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(signingKey);
			byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
		} catch (Exception e) {
			throw new IllegalStateException("토큰 서명 생성 실패", e);
		}
	}
}

package com.nextrun.cndbe.common;

import com.nextrun.cndbe.common.auth.InvalidCredentialsException;
import com.nextrun.cndbe.domain.drop.IntroTextGenerationFailedException;
import java.util.Map;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	// 존재하지 않는 리소스 참조 (예: 잘못된 dropId)
	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
	}

	// 잘못된 요청 값 (예: @RequestParam enum 변환 실패 - Enum.valueOf가 IllegalArgumentException을 던짐)
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
	}

	// 서버 설정/데이터 문제로 요청을 처리할 수 없는 상태 (예: 시드 데이터 누락)
	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
	}

	// b14 AI 소개문 재생성 실패 — 시도 횟수는 이미 차감된 뒤이므로, 프론트가 보여주는
	// 남은 횟수가 어긋나지 않도록 최신 값을 응답 본문에 같이 실어 보낸다.
	@ExceptionHandler(IntroTextGenerationFailedException.class)
	public ResponseEntity<Map<String, Object>> handleIntroTextGenerationFailed(
			IntroTextGenerationFailedException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
				"message", e.getMessage(),
				"regenerationsRemaining", e.getRegenerationsRemaining()
		));
	}

	// @Valid 요청 본문 검증 실패도 다른 에러와 동일한 {"message": "..."} 형식으로 반환한다.
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidation(
			MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(error -> error.getDefaultMessage() == null
						? "요청 값이 올바르지 않습니다."
						: error.getDefaultMessage())
				.orElse("요청 값이 올바르지 않습니다.");
		return ResponseEntity.badRequest().body(Map.of("message", message));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<Map<String, String>> handleTypeMismatch(
			MethodArgumentTypeMismatchException e) {
		return ResponseEntity.badRequest().body(Map.of(
				"message",
				e.getName() + " 값의 형식이 올바르지 않습니다."
		));
	}

	// 로그인 비밀번호 불일치
	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<Map<String, String>> handleInvalidCredentials(InvalidCredentialsException e) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, String>> handleUnreadableBody(
			HttpMessageNotReadableException e) {
		return ResponseEntity.badRequest().body(Map.of(
				"message",
				"요청 본문 형식이 올바르지 않습니다."
		));
	}

	// 업로드 파일/요청 용량이 spring.servlet.multipart 제한(파일당·요청 전체)을 초과한 경우
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<Map<String, String>> handleMaxUploadSizeExceeded(
			MaxUploadSizeExceededException e) {
		return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(Map.of(
				"message",
				"업로드 파일 또는 요청 전체 용량이 허용된 최대 크기를 초과했습니다."
		));
	}

	// 위에서 명시적으로 처리하지 않은 예외(DB 제약 위반 등)를 잡는 최후 방어선.
	// 안 잡으면 Spring 기본 에러 응답이 나가는데, message 필드가 영어 예외 메시지이거나
	// 아예 없어서 프론트가 "API error 500: ..." 같은 영어 문구를 그대로 보여주게 된다.
	// 내부 예외 내용은 로그로만 남기고, 사용자에게는 한국어 안내만 보낸다.
	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, String>> handleUnexpected(Exception e) {
		log.error("처리되지 않은 예외가 발생했습니다.", e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
				"message",
				"일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
		));
	}
}

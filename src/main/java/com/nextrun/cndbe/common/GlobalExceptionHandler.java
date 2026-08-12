package com.nextrun.cndbe.common;

import java.util.Map;
import java.util.NoSuchElementException;
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

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, String>> handleUnreadableBody(
			HttpMessageNotReadableException e) {
		return ResponseEntity.badRequest().body(Map.of(
				"message",
				"요청 본문 형식이 올바르지 않습니다."
		));
	}

	// 업로드 파일/요청 용량이 spring.servlet.multipart 제한을 초과한 경우
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<Map<String, String>> handleMaxUploadSizeExceeded(
			MaxUploadSizeExceededException e) {
		return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of(
				"message",
				"업로드 파일 용량이 허용된 최대 크기를 초과했습니다."
		));
	}
}

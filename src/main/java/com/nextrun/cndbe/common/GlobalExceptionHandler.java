package com.nextrun.cndbe.common;

import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
}

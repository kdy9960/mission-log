package com.sparta.missionreport.domain.column.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ColumnsExceptionCode {

    DUPLICATE_COLUMN_NAME(HttpStatus.CONFLICT, "COLUMN-002", "해당 칼럼 이름이 이미 존재합니다."),
    NOT_CHANGE_COLUMN_SEQUENCE(HttpStatus.BAD_REQUEST, "COLUMN-003", "칼럼 순서 변화가 없습니다."),
    NOT_FOUND_COLUMNS(HttpStatus.NOT_FOUND, "COLUMN-001", "해당 칼럼은 존재하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String errorCode;
    private final String message;
}

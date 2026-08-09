package com.side.project.web.exception.member;

import com.side.project.web.exception.ApplicationException;
import com.side.project.web.exception.ErrorCode;

public class DuplicateMemberException extends ApplicationException {
    public DuplicateMemberException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}

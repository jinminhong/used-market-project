package com.side.project.web.exception.member;

import com.side.project.web.exception.ApplicationException;
import com.side.project.web.exception.ErrorCode;

public class MemberException extends ApplicationException {
    public MemberException(String message) {
        super(ErrorCode.NOT_FOUND_MEMBER, message);
    }
}

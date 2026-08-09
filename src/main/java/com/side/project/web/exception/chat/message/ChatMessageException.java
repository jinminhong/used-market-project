package com.side.project.web.exception.chat.message;

import com.side.project.web.exception.ApplicationException;
import com.side.project.web.exception.ErrorCode;

public class ChatMessageException extends ApplicationException {
    public ChatMessageException(String message) {
        super(ErrorCode.INVALID_REQUEST, message);
    }
}

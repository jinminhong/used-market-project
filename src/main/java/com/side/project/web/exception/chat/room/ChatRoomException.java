package com.side.project.web.exception.chat.room;

import com.side.project.web.exception.ApplicationException;
import com.side.project.web.exception.ErrorCode;

public class ChatRoomException extends ApplicationException {
    public ChatRoomException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}

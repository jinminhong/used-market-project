package com.side.project.web.exception.item;

import com.side.project.web.exception.ApplicationException;
import com.side.project.web.exception.ErrorCode;

public class ItemException extends ApplicationException {
    public ItemException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}

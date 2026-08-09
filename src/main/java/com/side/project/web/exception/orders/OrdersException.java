package com.side.project.web.exception.orders;

import com.side.project.web.exception.ApplicationException;
import com.side.project.web.exception.ErrorCode;

public class OrdersException extends ApplicationException {
    public OrdersException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}

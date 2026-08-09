package com.side.project.web.exception.wishlist;

import com.side.project.web.exception.ApplicationException;
import com.side.project.web.exception.ErrorCode;

public class WishListException extends ApplicationException {
    public WishListException(String message) {
        super(ErrorCode.CONFLICT_STATE, message);
    }
}

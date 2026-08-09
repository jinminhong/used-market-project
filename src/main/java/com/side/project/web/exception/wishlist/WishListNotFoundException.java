package com.side.project.web.exception.wishlist;

import com.side.project.web.exception.ApplicationException;
import com.side.project.web.exception.ErrorCode;

public class WishListNotFoundException extends ApplicationException {
    public WishListNotFoundException(String message) {
        super(ErrorCode.NOT_FOUND_WISHLIST, message);
    }
}

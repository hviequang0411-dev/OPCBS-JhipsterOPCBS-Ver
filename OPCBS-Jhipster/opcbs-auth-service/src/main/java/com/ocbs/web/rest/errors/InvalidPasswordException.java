package com.ocbs.web.rest.errors;

import java.io.Serial;

@SuppressWarnings("java:S110") // Inheritance tree of classes should not be too deep
public class InvalidPasswordException extends BadRequestAlertException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidPasswordException() {
        super(ErrorConstants.INVALID_PASSWORD_TYPE, "Mật khẩu không chính xác hoặc không hợp lệ.", "userManagement", "invalidPassword");
    }
}

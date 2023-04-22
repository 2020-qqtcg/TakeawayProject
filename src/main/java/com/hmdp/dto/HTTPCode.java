package com.hmdp.dto;

/**
 * @author : mj
 * @since 2023/4/22 18:29
 */
public class HTTPCode {

    public static final int success = 200;
    public static final int NOT_CONTENT = 204;
    public static final int PARTIAL_CONTENT = 206;
    public static final int MOVED_PERMANENTLY = 301;
    public static final int FOUND = 302;
    public static final int SEE_OTHER = 303;
    public static final int TEMPORARY_REDIRECT = 307;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int INTERNAL_SERVER_ERROR = 500;
    public static final int SERVICE_UNAVAILABLE = 503;

    private HTTPCode() {

    }
}

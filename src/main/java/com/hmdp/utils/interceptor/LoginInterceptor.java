package com.hmdp.utils.interceptor;


import cn.hutool.http.HttpStatus;
import com.hmdp.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * @author : mj
 * @since 2023/4/22 21:11
 */
public class LoginInterceptor implements HandlerInterceptor {


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 判断有没有这个用户
        if (UserHolder.getUser() == null){
            response.setStatus(HttpStatus.HTTP_UNAUTHORIZED);
            return false;
        }

        return true;
    }

}

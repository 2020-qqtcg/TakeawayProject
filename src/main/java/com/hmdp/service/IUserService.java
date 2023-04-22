package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;

import javax.servlet.http.HttpSession;

/**
* 
* @author : mj
* @since 2023/4/22 17:53
*/
public interface IUserService extends IService<User> {

    /**
     * 发送手机验证码
     * @param phone 手机号
     * @param session 会话存储验证码
     * @return 发送结果
     */
    Result sendVerificationCode(String phone, HttpSession session);

    /**
     * 登录功能
     * @param loginForm 登录信息
     * @param session 会话
     * @return
     */
    Result login(LoginFormDTO loginForm, HttpSession session);
}

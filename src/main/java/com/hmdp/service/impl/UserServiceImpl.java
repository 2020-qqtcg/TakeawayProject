package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

/**
* 
* @author : mj
* @since 2023/4/22 17:38
*/
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {


    @Override
    public Result sendVerificationCode(String phone, HttpSession session) {
        // 1.校验手机号
        if (RegexUtils.isPhoneInvalid(phone)){
            // 2.不符合返回错误信息
            return Result.fail("手机号格式错误");
        }

        // 3.符合生成验证码
        String verificationCode = RandomUtil.randomNumbers(6);

        // 4. 保存验证码到session
        session.setAttribute("verificationCode", verificationCode);

        // 5. 发送验证码(模拟发送)
        log.debug("发送成功，验证码{}", verificationCode);

        return Result.success();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {

        // 1.校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式错误");
        }

        // 2.校验验证码
        String trueVerificationCode = (String) session.getAttribute("verificationCode");
        String verificationCode = loginForm.getCode();
        if (trueVerificationCode == null || !trueVerificationCode.equals(verificationCode)){
            return Result.fail("验证码错误");
        }

        // 4.根据手机号查询用户
        User user = query().eq("phone", phone).one();
        // 5.用户不存在，创建用户并保存
        if (user == null){
            user = createUserWithPhone(phone);
        }
        // 6.保存用户信息dto到session中
        UserDTO userDTO = new UserDTO(user.getId(), user.getNickName(), user.getIcon());
        session.setAttribute("user", userDTO);

        return Result.success();
    }

    private User createUserWithPhone(String phone) {
        // 1. 创建用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);
        return user;
    }
}
package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* 
* @author : mj
* @since 2023/5/17 21:49
*/
public interface IFollowService extends IService<Follow> {

    /**
     * 实现关注/取消关注
     * @param followUserId 关注者id
     * @param isFollow 是否关注
     */
    void follow(Long followUserId, Boolean isFollow);

    /**
     * 判断用户对id用户是否关注
     * @param followUserId 关注者id
     * @return 是否已经关注
     */
    boolean isFollow(Long followUserId);

    /**
     * 查询共同关注
     * @param id 用户id
     * @return 共同关注列表
     */
    List<UserDTO> followCommons(Long id);
}

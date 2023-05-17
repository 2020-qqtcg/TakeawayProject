package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.service.IFollowService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
*
* @author : mj
* @since 2023/5/17 21:37
*/
@RestController
@RequestMapping("/follow")
public class FollowController {

    @Resource
    private IFollowService followService;

    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long followUserId, @PathVariable("isFollow") Boolean isFollow){
        followService.follow(followUserId, isFollow);
        return Result.success();
    }

    @GetMapping("/or/not/{id}")
    public Result follow(@PathVariable("id") Long followUserId){
        return Result.success(followService.isFollow(followUserId));
    }

    @GetMapping("/common/{id}")
    public Result followCommons(@PathVariable("id") Long id){
        List<UserDTO> users = followService.followCommons(id);
        return Result.success(users);
    }
}

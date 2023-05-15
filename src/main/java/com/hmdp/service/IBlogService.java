package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.entity.User;

import java.util.List;

/**
* 
* @author : mj
* @since 2023/5/13 13:16
*/
public interface IBlogService extends IService<Blog> {

    /**
     * 根据id查询blog
     * @param id blog的id
     * @return blog
     */
    Blog queryBlogById(Long id);

    /**
     * 分页查询blog
     * @param current 页
     * @return 当前页的blog列表
     */
    List<Blog> queryHotBlog(Integer current);

    /**
     * 点赞
     * @param id 要点赞的blog的id
     */
    void likeBlog(Long id);

    /**
     * 查询top5的点赞用户
     * @param id blog id
     * @return 用户列表
     */
    List<UserDTO> queryBlogLikes(Long id);
}

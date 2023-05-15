package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

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
     *
     * @param current
     * @return
     */
    List<Blog> queryHotBlog(Integer current);
}

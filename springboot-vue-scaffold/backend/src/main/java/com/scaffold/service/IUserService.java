package com.scaffold.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scaffold.common.PageResult;
import com.scaffold.entity.User;

public interface IUserService extends IService<User> {

    PageResult<User> page(String keyword, String role, int pageNum, int pageSize);

    User create(User user);

    User update(Long id, User user);

    void delete(Long id);

    User findByUsername(String username);
}

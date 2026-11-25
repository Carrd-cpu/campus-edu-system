package com.scaffold.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scaffold.common.PageResult;
import com.scaffold.entity.Admin;

public interface IAdminService extends IService<Admin> {

    PageResult<Admin> page(String keyword, int pageNum, int pageSize);

    Admin create(Admin admin);

    Admin update(Long id, Admin admin);

    void delete(Long id);

    Admin findByUsername(String username);
}

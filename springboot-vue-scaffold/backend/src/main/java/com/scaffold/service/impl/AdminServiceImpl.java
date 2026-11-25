package com.scaffold.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scaffold.common.BusinessException;
import com.scaffold.common.PageResult;
import com.scaffold.entity.Admin;
import com.scaffold.entity.User;
import com.scaffold.mapper.AdminMapper;
import com.scaffold.mapper.UserMapper;
import com.scaffold.service.IAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;

@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements IAdminService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 分页查询
     */
    @Override
    public PageResult<Admin> page(String keyword, int pageNum, int pageSize) {
        LambdaQueryWrapper<Admin> queryWrapper = new LambdaQueryWrapper<Admin>().orderByDesc(Admin::getId);
        queryWrapper.and(StrUtil.isNotBlank(keyword), w -> w.like(Admin::getUsername, keyword)
                .or().like(Admin::getNickname, keyword)
                .or().like(Admin::getPhone, keyword));
        Page<Admin> page = page(new Page<>(pageNum, pageSize), queryWrapper);
        page.getRecords().forEach(admin -> admin.setPassword(null));
        return PageResult.of(page);
    }

    /**
     * 获取详情
     */
    @Override
    public Admin getById(Serializable id) {
        Admin admin = super.getById(id);
        if (admin == null) {
            throw new BusinessException(404, "管理员不存在");
        }
        admin.setPassword(null);
        return admin;
    }

    /**
     * 新增
     */
    @Override
    public Admin create(Admin admin) {
        if (StrUtil.isBlank(admin.getUsername())) {
            throw new BusinessException("用户名不能为空");
        }
        if (StrUtil.isBlank(admin.getPassword())) {
            throw new BusinessException("密码不能为空");
        }
        if (findByUsername(admin.getUsername()) != null || findUser(admin.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        if (admin.getStatus() == null) {
            admin.setStatus(1);
        }
        save(admin);
        return getById(admin.getId());
    }

    /**
     * 修改
     */
    @Override
    public Admin update(Long id, Admin admin) {
        getById(id);
        admin.setId(id);
        admin.setUsername(null);
        admin.setPassword(null);
        updateById(admin);
        return getById(id);
    }

    /**
     * 删除
     */
    @Override
    public void delete(Long id) {
        getById(id);
        if (count() <= 1) {
            throw new BusinessException("至少保留一名管理员");
        }
        removeById(id);
    }

    @Override
    public Admin findByUsername(String username) {
        return getOne(new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, username).last("LIMIT 1"), false);
    }

    private User findUser(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username).last("LIMIT 1"));
    }
}

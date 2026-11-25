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
import com.scaffold.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Set;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private static final Set<String> ALLOWED_ROLES = Set.of("USER");

    @Autowired
    private AdminMapper adminMapper;

    /**
     * 分页查询
     */
    @Override
    public PageResult<User> page(String keyword, String role, int pageNum, int pageSize) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>().orderByDesc(User::getId);
        queryWrapper.eq(StrUtil.isNotBlank(role), User::getRole, role);
        queryWrapper.and(StrUtil.isNotBlank(keyword), w -> w.like(User::getUsername, keyword)
                .or().like(User::getNickname, keyword)
                .or().like(User::getPhone, keyword));
        Page<User> page = page(new Page<>(pageNum, pageSize), queryWrapper);
        page.getRecords().forEach(user -> user.setPassword(null));
        return PageResult.of(page);
    }

    /**
     * 获取详情
     */
    @Override
    public User getById(Serializable id) {
        User user = super.getById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setPassword(null);
        return user;
    }

    /**
     * 新增
     */
    @Override
    public User create(User user) {
        if (StrUtil.isBlank(user.getUsername())) {
            throw new BusinessException("用户名不能为空");
        }
        if (StrUtil.isBlank(user.getPassword())) {
            throw new BusinessException("密码不能为空");
        }
        if (findByUsername(user.getUsername()) != null || findAdmin(user.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        if (user.getRole() == null) {
            user.setRole("USER");
        }
        if (!ALLOWED_ROLES.contains(user.getRole())) {
            throw new BusinessException("用户角色无效");
        }
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        save(user);
        return getById(user.getId());
    }

    /**
     * 修改
     */
    @Override
    public User update(Long id, User user) {
        getById(id);
        if (StrUtil.isNotBlank(user.getRole()) && !ALLOWED_ROLES.contains(user.getRole())) {
            throw new BusinessException("用户角色无效");
        }
        user.setId(id);
        user.setUsername(null);
        user.setPassword(null);
        updateById(user);
        return getById(id);
    }

    /**
     * 删除
     */
    @Override
    public void delete(Long id) {
        getById(id);
        removeById(id);
    }

    @Override
    public User findByUsername(String username) {
        return getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username).last("LIMIT 1"), false);
    }

    private Admin findAdmin(String username) {
        return adminMapper.selectOne(new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, username).last("LIMIT 1"));
    }
}

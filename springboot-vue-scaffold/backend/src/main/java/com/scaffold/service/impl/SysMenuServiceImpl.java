package com.scaffold.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scaffold.common.BusinessException;
import com.scaffold.common.PageResult;
import com.scaffold.entity.SysMenu;
import com.scaffold.mapper.SysMenuMapper;
import com.scaffold.service.ISysMenuService;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;

@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements ISysMenuService {

    /**
     * 查询全部
     */
    @Override
    public List<SysMenu> listAll() {
        return list(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSortOrder)
                .orderByAsc(SysMenu::getId));
    }

    /**
     * 分页查询
     */
    @Override
    public PageResult<SysMenu> page(int pageNum, int pageSize) {
        Page<SysMenu> page = page(new Page<>(pageNum, pageSize), new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSortOrder)
                .orderByDesc(SysMenu::getId));
        return PageResult.of(page);
    }

    @Override
    public SysMenu getById(Serializable id) {
        SysMenu menu = super.getById(id);
        if (menu == null) {
            throw new BusinessException(404, "菜单不存在");
        }
        return menu;
    }

    /**
     * 新增
     */
    @Override
    public SysMenu create(SysMenu menu) {
        if (menu.getStatus() == null) {
            menu.setStatus(1);
        }
        if (menu.getSortOrder() == null) {
            menu.setSortOrder(0);
        }
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        save(menu);
        return getById(menu.getId());
    }

    /**
     * 修改
     */
    @Override
    public SysMenu update(Long id, SysMenu menu) {
        getById(id);
        menu.setId(id);
        updateById(menu);
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
}

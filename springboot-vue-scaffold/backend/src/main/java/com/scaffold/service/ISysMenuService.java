package com.scaffold.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scaffold.common.PageResult;
import com.scaffold.entity.SysMenu;

import java.util.List;

public interface ISysMenuService extends IService<SysMenu> {

    List<SysMenu> listAll();

    PageResult<SysMenu> page(int pageNum, int pageSize);

    SysMenu create(SysMenu menu);

    SysMenu update(Long id, SysMenu menu);

    void delete(Long id);
}

package com.scaffold.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scaffold.common.PageResult;
import com.scaffold.entity.HomeBanner;

import java.util.List;

public interface IHomeBannerService extends IService<HomeBanner> {

    PageResult<HomeBanner> page(String keyword, int pageNum, int pageSize);

    List<HomeBanner> listEnabled();

    HomeBanner create(HomeBanner banner);

    HomeBanner update(Long id, HomeBanner banner);

    void delete(Long id);
}

package com.scaffold.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scaffold.common.BusinessException;
import com.scaffold.common.PageResult;
import com.scaffold.entity.HomeBanner;
import com.scaffold.mapper.HomeBannerMapper;
import com.scaffold.service.IHomeBannerService;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;

@Service
public class HomeBannerServiceImpl extends ServiceImpl<HomeBannerMapper, HomeBanner> implements IHomeBannerService {

    /**
     * 分页查询
     */
    @Override
    public PageResult<HomeBanner> page(String keyword, int pageNum, int pageSize) {
        LambdaQueryWrapper<HomeBanner> queryWrapper = new LambdaQueryWrapper<HomeBanner>()
                .orderByAsc(HomeBanner::getSortOrder)
                .orderByAsc(HomeBanner::getId);
        queryWrapper.and(StrUtil.isNotBlank(keyword), w -> w.like(HomeBanner::getTitle, keyword)
                .or().like(HomeBanner::getSubtitle, keyword));
        return PageResult.of(page(new Page<>(pageNum, pageSize), queryWrapper));
    }

    @Override
    public List<HomeBanner> listEnabled() {
        return list(new LambdaQueryWrapper<HomeBanner>()
                .eq(HomeBanner::getStatus, 1)
                .orderByAsc(HomeBanner::getSortOrder)
                .orderByAsc(HomeBanner::getId));
    }

    @Override
    public HomeBanner getById(Serializable id) {
        HomeBanner banner = super.getById(id);
        if (banner == null) {
            throw new BusinessException(404, "轮播图不存在");
        }
        return banner;
    }

    /**
     * 新增
     */
    @Override
    public HomeBanner create(HomeBanner banner) {
        normalize(banner);
        save(banner);
        return getById(banner.getId());
    }

    /**
     * 修改
     */
    @Override
    public HomeBanner update(Long id, HomeBanner banner) {
        getById(id);
        banner.setId(id);
        normalize(banner);
        updateById(banner);
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

    private void normalize(HomeBanner banner) {
        if (banner.getSortOrder() == null) {
            banner.setSortOrder(0);
        }
        if (banner.getStatus() == null) {
            banner.setStatus(1);
        }
        if (StrUtil.isBlank(banner.getCtaText())) {
            banner.setCtaText("了解更多");
        }
        if (StrUtil.isBlank(banner.getLinkUrl())) {
            banner.setLinkUrl("/user/home");
        }
    }
}

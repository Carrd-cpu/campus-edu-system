package com.scaffold.controller.admin;

import com.scaffold.common.AuthContext;
import com.scaffold.common.PageResult;
import com.scaffold.common.Result;
import com.scaffold.entity.HomeBanner;
import com.scaffold.service.IHomeBannerService;
import com.scaffold.service.ISysLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/banners")
public class AdminBannerController {

    @Autowired
    private IHomeBannerService homeBannerService;
    @Autowired
    private ISysLogService sysLogService;

    @GetMapping
    public Result<PageResult<HomeBanner>> page(HttpServletRequest request,
                                               @RequestParam(defaultValue = "") String keyword,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        AuthContext.requireAdmin(request);
        return Result.ok(homeBannerService.page(keyword, page, size));
    }

    @PostMapping
    public Result<HomeBanner> create(HttpServletRequest request, @RequestBody HomeBanner banner) {
        AuthContext.requireAdmin(request);
        HomeBanner created = homeBannerService.create(banner);
        sysLogService.logOperation(request, "轮播图管理", "新增轮播",
                "创建轮播：" + created.getTitle() + "（ID=" + created.getId() + "）");
        return Result.ok(created);
    }

    @PutMapping("/{id}")
    public Result<HomeBanner> update(HttpServletRequest request,
                                     @PathVariable Long id,
                                     @RequestBody HomeBanner banner) {
        AuthContext.requireAdmin(request);
        HomeBanner updated = homeBannerService.update(id, banner);
        sysLogService.logOperation(request, "轮播图管理", "修改轮播",
                "更新轮播：" + updated.getTitle() + "（ID=" + id + "）");
        return Result.ok(updated);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(HttpServletRequest request, @PathVariable Long id) {
        AuthContext.requireAdmin(request);
        HomeBanner existing = homeBannerService.getById(id);
        homeBannerService.delete(id);
        sysLogService.logOperation(request, "轮播图管理", "删除轮播",
                "删除轮播：" + existing.getTitle() + "（ID=" + id + "）");
        return Result.ok();
    }
}

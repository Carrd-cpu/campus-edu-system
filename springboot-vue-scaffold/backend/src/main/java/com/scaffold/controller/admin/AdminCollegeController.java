package com.scaffold.controller.admin;

import com.scaffold.common.AuthContext;
import com.scaffold.common.PageResult;
import com.scaffold.common.Result;
import com.scaffold.entity.EduCollege;
import com.scaffold.service.IEduCollegeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/edu/colleges")
public class AdminCollegeController {

    @Autowired
    private IEduCollegeService collegeService;

    @GetMapping
    public Result<PageResult<EduCollege>> page(HttpServletRequest request,
                                               @RequestParam(defaultValue = "") String keyword,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        AuthContext.requireAdmin(request);
        return Result.ok(collegeService.page(keyword, page, size));
    }

    @GetMapping("/options")
    public Result<List<EduCollege>> options(HttpServletRequest request) {
        AuthContext.requireAdmin(request);
        return Result.ok(collegeService.list());
    }

    @PostMapping
    public Result<EduCollege> save(HttpServletRequest request, @RequestBody EduCollege row) {
        AuthContext.requireAdmin(request);
        return Result.ok(collegeService.save(row));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(HttpServletRequest request, @PathVariable Long id) {
        AuthContext.requireAdmin(request);
        collegeService.delete(id);
        return Result.ok();
    }
}

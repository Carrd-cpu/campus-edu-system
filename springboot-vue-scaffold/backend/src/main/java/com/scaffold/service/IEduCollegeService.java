package com.scaffold.service;

import com.scaffold.common.PageResult;
import com.scaffold.entity.EduCollege;

import java.util.List;

public interface IEduCollegeService {

    /**
     * 新增/编辑
     * @param eduCollege
     * @return
     */
    EduCollege save(EduCollege eduCollege);

    /**
     * 查询全部学院
     * @return
     */
    List<EduCollege> list();

    /**
     * 分页查询
     * @param keyword
     * @param pageNum
     * @param pageSize
     * @return
     */
    PageResult<EduCollege> page(String keyword,Integer pageNum,Integer pageSize);

    /**
     * 删除
     * @param id
     */
    void delete(Long id);
}

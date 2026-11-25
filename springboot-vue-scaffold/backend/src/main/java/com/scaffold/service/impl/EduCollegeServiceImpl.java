package com.scaffold.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scaffold.common.BusinessException;
import com.scaffold.common.PageResult;
import com.scaffold.edu.EduText;
import com.scaffold.entity.EduCollege;
import com.scaffold.mapper.EduCollegeMapper;
import com.scaffold.service.IEduCollegeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EduCollegeServiceImpl implements IEduCollegeService {

    @Autowired
    private EduCollegeMapper eduCollegeMapper;

    @Override
    public EduCollege save(EduCollege eduCollege) {
        EduText.requireText(eduCollege.getName(),"学院名称不能为空");
        EduText.requireText(eduCollege.getCode(),"学院编码不能为空");
        if (null == eduCollege.getId()){
            eduCollegeMapper.insert(eduCollege);
        } else {
            eduCollegeMapper.updateById(eduCollege);
        }
        return eduCollegeMapper.selectById(eduCollege.getId());
    }

    @Override
    public List<EduCollege> list() {
        // select * from edu_college where id = xxx;
//        LambdaQueryWrapper<EduCollege> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(EduCollege::getId, "xxx");
        List<EduCollege> eduColleges = eduCollegeMapper.selectList(null);
        return eduColleges;
    }

    @Override
    public PageResult<EduCollege> page(String keyword, Integer pageNum, Integer pageSize) {
        Page<EduCollege> page = new Page<>(pageNum,pageSize);
        LambdaQueryWrapper<EduCollege> queryWrapper = new LambdaQueryWrapper<EduCollege>().orderByAsc(EduCollege::getId);
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.and(w -> w.like(EduCollege::getName, keyword).or().like(EduCollege::getCode, keyword));
        }
        Page<EduCollege> page1 = eduCollegeMapper.selectPage(page, queryWrapper);
        return new PageResult<>(page1.getTotal(),page1.getRecords());
    }

    @Override
    public void delete(Long id) {
        if (null != id){
            eduCollegeMapper.deleteById(id);
        } else {
            throw new BusinessException("id不能为空");
        }
    }
}

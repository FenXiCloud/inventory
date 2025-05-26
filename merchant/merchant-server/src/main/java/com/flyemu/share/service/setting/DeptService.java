package com.flyemu.share.service.setting;

import com.flyemu.share.entity.setting.Dept;
import com.flyemu.share.entity.setting.QDept;
import com.flyemu.share.repository.DeptRepository;
import com.flyemu.share.service.AbsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author: wangwenjia
 * @since: 2025/5/20 17:44
 * @description:
 */
@Service
@Slf4j
@Transactional(readOnly = false)
@RequiredArgsConstructor
public class DeptService extends AbsService{

    private final static QDept qDept = QDept.dept;
    private final DeptRepository deptRepository;

    public Dept selectDeptById(Long deptId) {
        return jqf.selectFrom(qDept).where(qDept.id.eq(deptId)).fetchFirst();
    }

    public void insertDept(Dept sysDept) {
        deptRepository.save(sysDept);
    }

    public Dept checkDeptNameUnique(String deptName) {
        return jqf.selectFrom(qDept).where(qDept.deptName.eq(deptName)).fetchFirst();
    }
}

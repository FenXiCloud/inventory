package com.flyemu.share.service.setting;

import com.flyemu.share.entity.setting.Dept;
import com.flyemu.share.entity.setting.QDept;
import com.flyemu.share.repository.setting.DeptRepository;
import com.flyemu.share.service.BaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional(readOnly = false)
@RequiredArgsConstructor
public class DeptService extends BaseService{

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

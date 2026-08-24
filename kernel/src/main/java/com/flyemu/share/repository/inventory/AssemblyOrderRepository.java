package com.flyemu.share.repository.inventory;

import com.flyemu.share.entity.inventory.AssemblyOrder;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;

public interface AssemblyOrderRepository extends JpaRepositoryImplementation<AssemblyOrder, Long> {
}

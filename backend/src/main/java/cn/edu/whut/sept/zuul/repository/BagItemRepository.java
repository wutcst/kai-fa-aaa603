package cn.edu.whut.sept.zuul.repository;

import cn.edu.whut.sept.zuul.model.BagItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface BagItemRepository extends JpaRepository<BagItemEntity, Long> {
    List<BagItemEntity> findBySaveId(Long saveId);

    @Transactional
    void deleteBySaveId(Long saveId);
}

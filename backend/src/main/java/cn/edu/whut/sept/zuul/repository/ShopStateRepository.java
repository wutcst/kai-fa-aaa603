package cn.edu.whut.sept.zuul.repository;

import cn.edu.whut.sept.zuul.model.ShopStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface ShopStateRepository extends JpaRepository<ShopStateEntity, Long> {
    List<ShopStateEntity> findBySaveId(Long saveId);
    @Transactional
    void deleteBySaveId(Long saveId);
}

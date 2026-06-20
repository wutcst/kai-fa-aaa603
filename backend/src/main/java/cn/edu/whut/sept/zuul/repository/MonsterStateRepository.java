package cn.edu.whut.sept.zuul.repository;

import cn.edu.whut.sept.zuul.model.MonsterStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MonsterStateRepository extends JpaRepository<MonsterStateEntity, Long> {
    List<MonsterStateEntity> findBySaveId(Long saveId);
    void deleteBySaveId(Long saveId);
}

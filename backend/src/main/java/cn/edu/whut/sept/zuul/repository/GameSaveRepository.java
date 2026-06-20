package cn.edu.whut.sept.zuul.repository;

import cn.edu.whut.sept.zuul.model.GameSaveEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameSaveRepository extends JpaRepository<GameSaveEntity, Long> {
}

package com.metrocarpool.user.repository;

import com.metrocarpool.user.entity.DriverEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DriverRepository extends JpaRepository<DriverEntity, Long> {
  Optional<DriverEntity> findByUsername(String username);
}

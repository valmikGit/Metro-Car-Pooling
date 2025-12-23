package com.metrocarpool.user.repository;

import com.metrocarpool.user.entity.RiderEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RiderRepository extends JpaRepository<RiderEntity, Long> {
  Optional<RiderEntity> findByUsername(String username);
}

package com.castfan.repository;

import java.util.Optional;

import com.castfan.entity.CastProfile;
import com.castfan.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CastProfileRepository extends JpaRepository<CastProfile, Long> {

	Optional<CastProfile> findByUserAccountId(Long userAccountId);

	Optional<CastProfile> findByStageNameKey(String stageNameKey);

	Page<CastProfile> findAllByDiscoverableTrueAndUserAccountStatusOrderByStageNameAsc(
		UserStatus status,
		Pageable pageable
	);

	boolean existsByUserAccountId(Long userAccountId);

	boolean existsByStageNameKey(String stageNameKey);
}

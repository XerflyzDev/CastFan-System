package com.castfan.repository;

import java.util.List;
import java.util.Optional;

import com.castfan.entity.CastProfile;
import com.castfan.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CastProfileRepository extends JpaRepository<CastProfile, Long> {

	Optional<CastProfile> findByUserAccountId(Long userAccountId);

	Optional<CastProfile> findByUserAccountLoginIdentifierIgnoreCase(String loginIdentifier);

	Optional<CastProfile> findByStoreIdAndStageNameKey(Long storeId, String stageNameKey);

	Optional<CastProfile> findByStageNameKey(String stageNameKey);

	Page<CastProfile> findAllByUserAccountStatusOrderByStageNameAsc(
		UserStatus status,
		Pageable pageable
	);

	Page<CastProfile> findAllByStoreIdAndUserAccountStatusOrderByStageNameAsc(
		Long storeId, UserStatus status, Pageable pageable);

	List<CastProfile> findAllByUserAccountStatusOrderByStageNameAsc(UserStatus status);

	boolean existsByUserAccountId(Long userAccountId);

	boolean existsByStoreIdAndStageNameKey(Long storeId, String stageNameKey);

	boolean existsByStageNameKey(String stageNameKey);
}

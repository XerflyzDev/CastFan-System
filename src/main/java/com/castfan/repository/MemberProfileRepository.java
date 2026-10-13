package com.castfan.repository;

import java.util.Optional;

import com.castfan.entity.MemberProfile;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberProfileRepository extends JpaRepository<MemberProfile, Long> {

	Optional<MemberProfile> findByUserAccountId(Long userAccountId);

	boolean existsByUserAccountId(Long userAccountId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select profile from MemberProfile profile where profile.id = :id")
	Optional<MemberProfile> findByIdForUpdate(@Param("id") Long id);
}

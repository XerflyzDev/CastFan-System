package com.castfan.repository;

import java.util.List;
import java.util.Optional;

import com.castfan.entity.MembershipStatus;
import com.castfan.entity.TierMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface TierMembershipRepository extends JpaRepository<TierMembership, Long> {

	Optional<TierMembership> findByMemberProfileIdAndMemberTierId(Long memberProfileId, Long memberTierId);

	@Query("""
		select membership
		from TierMembership membership
		join fetch membership.memberTier tier
		join fetch tier.castProfile profile
		where membership.memberProfile.id = :memberProfileId
		  and membership.status = :status
		order by profile.stageName asc
		""")
	List<TierMembership> findAllActiveForMember(
		@Param("memberProfileId") Long memberProfileId,
		@Param("status") MembershipStatus status
	);

	@Query("""
		select membership
		from TierMembership membership
		join fetch membership.memberTier tier
		where membership.memberProfile.id = :memberProfileId
		  and tier.castProfile.id = :castProfileId
		  and membership.status = :status
		""")
	Optional<TierMembership> findActiveForCast(
		@Param("memberProfileId") Long memberProfileId,
		@Param("castProfileId") Long castProfileId,
		@Param("status") MembershipStatus status
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select membership from TierMembership membership where membership.id = :id")
	Optional<TierMembership> findByIdForUpdate(@Param("id") Long id);
}

package com.castfan.repository;

import java.util.Collection;
import java.util.List;

import com.castfan.entity.CastFollow;
import com.castfan.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CastFollowRepository extends JpaRepository<CastFollow, Long> {

	boolean existsByMemberProfileIdAndCastProfileId(Long memberProfileId, Long castProfileId);

	boolean existsByStoreMembershipIdAndCastProfileId(Long storeMembershipId, Long castProfileId);

	long countByCastProfileId(Long castProfileId);

	@Query("""
		select follow.castProfile.id
		from CastFollow follow
		where follow.memberProfile.id = :memberProfileId
		  and follow.castProfile.id in :castProfileIds
		""")
	List<Long> findFollowedCastProfileIds(
		@Param("memberProfileId") Long memberProfileId,
		@Param("castProfileIds") Collection<Long> castProfileIds
	);

	@Query("""
		select follow
		from CastFollow follow
		join fetch follow.castProfile profile
		join fetch profile.userAccount account
		where follow.memberProfile.id = :memberProfileId
		  and account.status = :status
		order by profile.stageName asc
		""")
	List<CastFollow> findVisibleFollowing(
		@Param("memberProfileId") Long memberProfileId,
		@Param("status") UserStatus status
	);

	@Modifying
	long deleteByMemberProfileIdAndCastProfileId(Long memberProfileId, Long castProfileId);

	long deleteByStoreMembershipIdAndCastProfileId(Long storeMembershipId, Long castProfileId);

	@Query("""
		select follow from CastFollow follow
		join fetch follow.castProfile profile
		join fetch profile.userAccount account
		where follow.storeMembership.id = :membershipId and account.status = :status
		order by profile.stageName asc
		""")
	List<CastFollow> findVisibleFollowingForMembership(@Param("membershipId") Long membershipId,
		@Param("status") UserStatus status);
}

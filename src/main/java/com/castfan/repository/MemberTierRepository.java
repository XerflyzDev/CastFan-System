package com.castfan.repository;

import java.util.List;

import com.castfan.entity.MemberTier;
import com.castfan.entity.UserStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberTierRepository extends JpaRepository<MemberTier, Long> {

	boolean existsByCastProfileIdAndNameKey(Long castProfileId, String nameKey);

	boolean existsByCastProfileIdAndRank(Long castProfileId, int rank);

	List<MemberTier> findAllByCastProfileIdOrderByRankAsc(Long castProfileId);

	@Query("""
		select tier
		from MemberTier tier
		join fetch tier.castProfile profile
		join fetch profile.userAccount account
		where tier.active = true
		  and profile.discoverable = true
		  and account.status = :status
		order by profile.stageName asc, tier.rank asc
		""")
	List<MemberTier> findAvailable(@Param("status") UserStatus status, Pageable pageable);
}

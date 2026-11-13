package com.castfan.repository;

import java.util.List;
import java.util.Optional;

import com.castfan.entity.MemberProfile;
import com.castfan.entity.Store;
import com.castfan.entity.StoreMembership;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreMembershipRepository extends JpaRepository<StoreMembership, Long> {
	Optional<StoreMembership> findByStoreAndMemberProfile(Store store, MemberProfile memberProfile);
	Optional<StoreMembership> findByIdAndStore(Long id, Store store);
	@EntityGraph(attributePaths = "store")
	List<StoreMembership> findAllByMemberProfileOrderByJoinedAtAsc(MemberProfile memberProfile);
	List<StoreMembership> findAllByStoreOrderByJoinedAtDesc(Store store);
}

package com.castfan.service;

import com.castfan.entity.StoreMembership;
import com.castfan.entity.UserAccount;
import com.castfan.entity.UserRole;
import com.castfan.repository.MemberProfileRepository;
import com.castfan.repository.StoreMembershipRepository;
import com.castfan.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoreEntryService {
	public static final String SESSION_STORE_SLUG = "verifiedStoreSlug";
	private final StoreRepository stores;
	private final MemberProfileRepository members;
	private final StoreMembershipRepository memberships;

	public StoreEntryService(StoreRepository stores, MemberProfileRepository members, StoreMembershipRepository memberships) {
		this.stores = stores;
		this.members = members;
		this.memberships = memberships;
	}

	public boolean isActiveSlug(String slug) { return stores.findBySlugAndActiveTrue(slug).isPresent(); }

	@Transactional
	public String joinAndHome(UserAccount account, String slug) {
		if (account.getRole() != UserRole.MEMBER) throw new IllegalArgumentException("Only Member accounts can join stores.");
		var store = stores.findBySlugAndActiveTrue(slug).orElseThrow(() -> new IllegalArgumentException("Store not found."));
		var member = members.findByUserAccountId(account.getId()).orElseThrow();
		memberships.findByStoreAndMemberProfile(store, member)
			.orElseGet(() -> memberships.save(new StoreMembership(store, member)));
		return "/s/" + store.getSlug() + "/home";
	}
}

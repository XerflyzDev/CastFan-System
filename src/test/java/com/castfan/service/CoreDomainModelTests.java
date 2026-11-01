package com.castfan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.castfan.entity.UserAccount;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.CastProfileRepository;
import com.castfan.repository.MemberProfileRepository;
import com.castfan.repository.StoreRepository;
import com.castfan.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CoreDomainModelTests {

	@Autowired
	private ProfileService profileService;

	@Autowired
	private StoreService storeService;

	@Autowired
	private PrivilegedAccountProvisioningService privilegedAccountProvisioningService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private MemberProfileRepository memberProfileRepository;

	@Autowired
	private CastProfileRepository castProfileRepository;

	@Autowired
	private StoreRepository storeRepository;

	@Test
	void memberProfileRequiresAnActiveMemberAndIsUniquePerAccount() {
		var member = saveAccount("member@castfan.test", UserRole.MEMBER, UserStatus.ACTIVE);
		var profile = profileService.createMemberProfile(member);

		assertThat(profile.getId()).isNotNull();
		assertThat(memberProfileRepository.findByUserAccountId(member.getId())).contains(profile);
		assertThatThrownBy(() -> profileService.createMemberProfile(member))
			.isInstanceOf(DomainConflictException.class);

		var owner = saveAccount("owner-member@castfan.test", UserRole.OWNER, UserStatus.ACTIVE);
		assertThatThrownBy(() -> profileService.createMemberProfile(owner))
			.isInstanceOf(DomainRuleViolationException.class);
	}

	@Test
	void castStageNamesAreUniqueAfterUnicodeAndCaseNormalization() {
		var firstCast = saveAccount("first-cast@castfan.test", UserRole.CAST, UserStatus.ACTIVE);
		var secondCast = saveAccount("second-cast@castfan.test", UserRole.CAST, UserStatus.ACTIVE);

		var profile = profileService.createCastProfile(firstCast, "Mira", "Main floor Cast", null);

		assertThat(profile.getStageName()).isEqualTo("Mira");
		assertThat(profile.getStageNameKey()).isEqualTo("mira");
		assertThat(castProfileRepository.findByStageNameKey("mira")).contains(profile);
		assertThatThrownBy(() ->
			profileService.createCastProfile(secondCast, "MIRA", null, null)
		).isInstanceOf(DomainConflictException.class);
	}

	@Test
	void castProfileRejectsInactiveOrIncorrectRoleAccounts() {
		var member = saveAccount("wrong-role@castfan.test", UserRole.MEMBER, UserStatus.ACTIVE);
		var suspendedCast = saveAccount("suspended-cast@castfan.test", UserRole.CAST, UserStatus.SUSPENDED);

		assertThatThrownBy(() ->
			profileService.createCastProfile(member, "Role Error", null, null)
		).isInstanceOf(DomainRuleViolationException.class);
		assertThatThrownBy(() ->
			profileService.createCastProfile(suspendedCast, "Status Error", null, null)
		).isInstanceOf(DomainRuleViolationException.class);
	}

	@Test
	void ownerCanCreateAStoreWithNormalizedSlugAndValidTimezone() {
		var owner = saveAccount("store-owner@castfan.test", UserRole.OWNER, UserStatus.ACTIVE);

		var store = storeService.createStore(
			owner,
			"Hoshi Cafe",
			"  HOSHI-CAFE  ",
			"A Cast cafe in Bangkok.",
			"Bangkok, Thailand",
			"Asia/Bangkok"
		);

		assertThat(store.getSlug()).isEqualTo("hoshi-cafe");
		assertThat(store.getTimezone()).isEqualTo("Asia/Bangkok");
		assertThat(store.isActive()).isTrue();
		assertThat(storeRepository.findAllByOwnerIdOrderByNameAsc(owner.getId())).containsExactly(store);
	}

	@Test
	void storeCreationRejectsDuplicateSlugInvalidTimezoneAndNonOwner() {
		var owner = saveAccount("valid-owner@castfan.test", UserRole.OWNER, UserStatus.ACTIVE);
		var otherOwner = saveAccount("other-owner@castfan.test", UserRole.OWNER, UserStatus.ACTIVE);
		var member = saveAccount("not-owner@castfan.test", UserRole.MEMBER, UserStatus.ACTIVE);

		storeService.createStore(owner, "First Store", "first-store", null, null, "Asia/Bangkok");

		assertThatThrownBy(() ->
			storeService.createStore(otherOwner, "Duplicate", "FIRST-STORE", null, null, "Asia/Bangkok")
		).isInstanceOf(DomainConflictException.class);
		assertThatThrownBy(() ->
			storeService.createStore(owner, "Bad Zone", "bad-zone", null, null, "Bangkok")
		).isInstanceOf(DomainRuleViolationException.class);
		assertThatThrownBy(() ->
			storeService.createStore(member, "Wrong Owner", "wrong-owner", null, null, "Asia/Bangkok")
		).isInstanceOf(DomainRuleViolationException.class);
	}

	@Test
	void privilegedProvisioningCreatesCastAndOwnerWithoutAPublicRoleChoice() {
		var cast = privilegedAccountProvisioningService.provisionCast(
			"NEW-CAST@castfan.test",
			"long-secure-pass-123",
			"New Cast",
			"Nova"
		);
		var owner = privilegedAccountProvisioningService.provisionOwner(
			"NEW-OWNER@castfan.test",
			"another-secure-pass-123",
			"New Owner"
		);

		assertThat(cast.getEmail()).isEqualTo("new-cast@castfan.test");
		assertThat(cast.getRole()).isEqualTo(UserRole.CAST);
		assertThat(passwordEncoder.matches("long-secure-pass-123", cast.getPasswordHash())).isTrue();
		assertThat(castProfileRepository.findByUserAccountId(cast.getId())).isPresent();
		assertThat(owner.getRole()).isEqualTo(UserRole.OWNER);
		assertThat(castProfileRepository.findByUserAccountId(owner.getId())).isEmpty();
		assertThat(memberProfileRepository.findByUserAccountId(owner.getId())).isEmpty();
	}

	@Test
	void privilegedProvisioningRejectsWeakPasswordsAndDuplicateEmails() {
		privilegedAccountProvisioningService.provisionOwner(
			"provisioned@castfan.test",
			"secure-owner-pass-123",
			"First Owner"
		);

		assertThatThrownBy(() -> privilegedAccountProvisioningService.provisionOwner(
			"weak@castfan.test",
			"too-short",
			"Weak Owner"
		)).isInstanceOf(DomainRuleViolationException.class);
		assertThatThrownBy(() -> privilegedAccountProvisioningService.provisionOwner(
			"PROVISIONED@castfan.test",
			"different-secure-pass-123",
			"Duplicate Owner"
		)).isInstanceOf(DuplicateEmailException.class);
	}

	private UserAccount saveAccount(String email, UserRole role, UserStatus status) {
		return userAccountRepository.saveAndFlush(
			new UserAccount(email, "encoded-password", email, role, status)
		);
	}
}

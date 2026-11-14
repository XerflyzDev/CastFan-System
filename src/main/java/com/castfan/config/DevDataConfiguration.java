package com.castfan.config;

import java.time.ZoneId;

import com.castfan.entity.CastProfile;
import com.castfan.entity.MemberProfile;
import com.castfan.entity.Store;
import com.castfan.entity.StoreCast;
import com.castfan.entity.StoreMembership;
import com.castfan.entity.UserAccount;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.CastProfileRepository;
import com.castfan.repository.MemberProfileRepository;
import com.castfan.repository.StoreCastRepository;
import com.castfan.repository.StoreRepository;
import com.castfan.repository.StoreMembershipRepository;
import com.castfan.repository.UserAccountRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("dev")
public class DevDataConfiguration {

	private static final ZoneId STORE_ZONE = ZoneId.of("Asia/Bangkok");

	@Bean
	ApplicationRunner seedDevHomeData(
		UserAccountRepository userAccountRepository,
		CastProfileRepository castProfileRepository,
		MemberProfileRepository memberProfileRepository,
		StoreRepository storeRepository,
		StoreMembershipRepository storeMembershipRepository,
		StoreCastRepository storeCastRepository,
		PasswordEncoder passwordEncoder
	) {
		return arguments -> {
			if (userAccountRepository.count() > 0) {
				return;
			}

			var password = passwordEncoder.encode("dev-only-password");
			var owner = userAccountRepository.save(new UserAccount(
				"owner@castfan.local", password, "CastFan Owner", UserRole.OWNER, UserStatus.ACTIVE
			));
			var store = storeRepository.save(new Store(
				owner,
				"CastFan",
				"castfan",
				"A Cast cafe in Bangkok.",
				"Bangkok, Thailand",
				STORE_ZONE.getId(),
				true
			));
			var member = userAccountRepository.save(new UserAccount(
				"member@castfan.local", password, "Mina Fan", UserRole.MEMBER, UserStatus.ACTIVE
			));
			var memberProfile = memberProfileRepository.save(new MemberProfile(member, null, null));
			storeMembershipRepository.save(new StoreMembership(store, memberProfile));

			var castAccount = userAccountRepository.save(new UserAccount(
				"mina@castfan.local", password, "Mina", UserRole.CAST, UserStatus.ACTIVE
			));
			memberProfileRepository.save(new MemberProfile(castAccount, null, null));
			var castProfile = castProfileRepository.save(new CastProfile(
				castAccount, store, "Mina", "mina", null, null
			));
			storeCastRepository.save(new StoreCast(store, castProfile, true));
		};
	}
}

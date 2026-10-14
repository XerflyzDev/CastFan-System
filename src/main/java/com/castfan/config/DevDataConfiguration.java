package com.castfan.config;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import com.castfan.entity.CastProfile;
import com.castfan.entity.EventStatus;
import com.castfan.entity.ShiftSchedule;
import com.castfan.entity.Store;
import com.castfan.entity.StoreCast;
import com.castfan.entity.StoreEvent;
import com.castfan.entity.StorePromotion;
import com.castfan.entity.UserAccount;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.CastProfileRepository;
import com.castfan.repository.ShiftScheduleRepository;
import com.castfan.repository.StoreCastRepository;
import com.castfan.repository.StoreEventRepository;
import com.castfan.repository.StorePromotionRepository;
import com.castfan.repository.StoreRepository;
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
		StoreRepository storeRepository,
		StoreCastRepository storeCastRepository,
		ShiftScheduleRepository shiftScheduleRepository,
		StoreEventRepository storeEventRepository,
		StorePromotionRepository storePromotionRepository,
		PasswordEncoder passwordEncoder
	) {
		return arguments -> {
			if (userAccountRepository.count() > 0) {
				return;
			}

			var password = passwordEncoder.encode("dev-only-password");
			var owner = userAccountRepository.save(new UserAccount(
				"owner@castfan.local", password, "Hoshi Owner", UserRole.OWNER, UserStatus.ACTIVE
			));
			var store = storeRepository.save(new Store(
				owner,
				"Hoshi Cafe",
				"hoshi-cafe",
				"A single-store Cast cafe in Bangkok.",
				"Bangkok, Thailand",
				STORE_ZONE.getId(),
				true
			));

			var today = LocalDate.now(STORE_ZONE);
			seedCast(
				"mina@castfan.local", "Mina", "Warm conversations and seasonal desserts.",
				"/images/mina-profile.jpg", LocalTime.of(14, 0), LocalTime.of(20, 0),
				password, owner, store, today, userAccountRepository, castProfileRepository,
				storeCastRepository, shiftScheduleRepository
			);
			seedCast(
				"yuki@castfan.local", "Yuki", "Evening shifts and cafe recommendations.",
				null, LocalTime.of(16, 0), LocalTime.of(22, 0),
				password, owner, store, today, userAccountRepository, castProfileRepository,
				storeCastRepository, shiftScheduleRepository
			);
			seedCast(
				"ami@castfan.local", "Ami", "Late-night events and member updates.",
				null, LocalTime.of(18, 0), LocalTime.of(23, 0),
				password, owner, store, today, userAccountRepository, castProfileRepository,
				storeCastRepository, shiftScheduleRepository
			);

			var eventStartsAt = today.plusDays(17).atTime(16, 0).atZone(STORE_ZONE).toInstant();
			storeEventRepository.save(new StoreEvent(
				store,
				owner,
				"Autumn Sweet Night",
				"Seasonal strawberry desserts and a special evening with the Hoshi Cafe Cast.",
				"/images/autumn-cafe-event.png",
				eventStartsAt,
				eventStartsAt.plusSeconds(5 * 60 * 60),
				EventStatus.PUBLISHED
			));
			storePromotionRepository.save(new StorePromotion(
				store, owner, "First visit welcome", "Show this offer to receive a seasonal welcome treat.",
				"WELCOME10", "/images/autumn-cafe-event.png",
				today.minusDays(1).atStartOfDay(STORE_ZONE).toInstant(),
				today.plusDays(30).atTime(23, 59).atZone(STORE_ZONE).toInstant(), true
			));
		};
	}

	private void seedCast(
		String email,
		String stageName,
		String bio,
		String avatarUrl,
		LocalTime startsAt,
		LocalTime endsAt,
		String password,
		UserAccount owner,
		Store store,
		LocalDate date,
		UserAccountRepository userAccountRepository,
		CastProfileRepository castProfileRepository,
		StoreCastRepository storeCastRepository,
		ShiftScheduleRepository shiftScheduleRepository
	) {
		var account = userAccountRepository.save(new UserAccount(
			email, password, stageName, UserRole.CAST, UserStatus.ACTIVE
		));
		var profile = castProfileRepository.save(new CastProfile(
			account, stageName, stageName.toLowerCase(), bio, avatarUrl, true
		));
		var assignment = storeCastRepository.save(new StoreCast(store, profile, true));
		var shift = new ShiftSchedule(
			assignment,
			date.atTime(startsAt).atZone(STORE_ZONE).toInstant(),
			date.atTime(endsAt).atZone(STORE_ZONE).toInstant()
		);
		shift.approve(owner, "Approved development fixture");
		shiftScheduleRepository.save(shift);
	}
}

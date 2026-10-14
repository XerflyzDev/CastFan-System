package com.castfan.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;

import com.castfan.entity.CastFollow;
import com.castfan.entity.CastProfile;
import com.castfan.entity.EventStatus;
import com.castfan.entity.MemberProfile;
import com.castfan.entity.ShiftSchedule;
import com.castfan.entity.Store;
import com.castfan.entity.StoreCast;
import com.castfan.entity.StoreEvent;
import com.castfan.entity.UserAccount;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.CastFollowRepository;
import com.castfan.repository.CastProfileRepository;
import com.castfan.repository.MemberProfileRepository;
import com.castfan.repository.ShiftScheduleRepository;
import com.castfan.repository.StoreCastRepository;
import com.castfan.repository.StoreEventRepository;
import com.castfan.repository.StoreRepository;
import com.castfan.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class HomeServiceTests {

	private static final ZoneId STORE_ZONE = ZoneId.of("Asia/Bangkok");

	@Autowired private HomeService homeService;
	@Autowired private UserAccountRepository userAccountRepository;
	@Autowired private MemberProfileRepository memberProfileRepository;
	@Autowired private CastProfileRepository castProfileRepository;
	@Autowired private StoreRepository storeRepository;
	@Autowired private StoreCastRepository storeCastRepository;
	@Autowired private ShiftScheduleRepository shiftScheduleRepository;
	@Autowired private StoreEventRepository storeEventRepository;
	@Autowired private CastFollowRepository castFollowRepository;

	@Test
	void homeUsesApprovedPublicDatabaseRecordsAndMemberFollowState() {
		var owner = account("owner-home@test.local", "Owner", UserRole.OWNER);
		var member = account("member-home@test.local", "Aruna", UserRole.MEMBER);
		var memberProfile = memberProfileRepository.save(new MemberProfile(member, null, null));
		var store = storeRepository.save(new Store(
			owner, "Hoshi Cafe", "home-hoshi", null, null, STORE_ZONE.getId(), true
		));

		var mina = cast("mina-home@test.local", "Mina");
		var yuki = cast("yuki-home@test.local", "Yuki");
		var minaAssignment = storeCastRepository.save(new StoreCast(store, mina, true));
		var yukiAssignment = storeCastRepository.save(new StoreCast(store, yuki, true));
		var today = LocalDate.now(STORE_ZONE);
		var approvedShift = new ShiftSchedule(
			minaAssignment,
			today.atTime(14, 0).atZone(STORE_ZONE).toInstant(),
			today.atTime(20, 0).atZone(STORE_ZONE).toInstant()
		);
		approvedShift.approve(owner, null);
		shiftScheduleRepository.save(approvedShift);
		shiftScheduleRepository.save(new ShiftSchedule(
			yukiAssignment,
			today.atTime(16, 0).atZone(STORE_ZONE).toInstant(),
			today.atTime(22, 0).atZone(STORE_ZONE).toInstant()
		));
		castFollowRepository.save(new CastFollow(memberProfile, mina));

		var eventStartsAt = today.plusDays(2).atTime(16, 0).atZone(STORE_ZONE).toInstant();
		storeEventRepository.save(new StoreEvent(
			store, owner, "Published Night", "Published event", "/images/autumn-cafe-event.png",
			eventStartsAt, eventStartsAt.plusSeconds(3600), EventStatus.PUBLISHED
		));
		storeEventRepository.save(new StoreEvent(
			store, owner, "Draft Night", "Draft event", null,
			eventStartsAt, eventStartsAt.plusSeconds(3600), EventStatus.DRAFT
		));

		var home = homeService.load(member.getEmail());

		assertThat(home.memberName()).isEqualTo("Aruna");
		assertThat(home.storeName()).isEqualTo("Hoshi Cafe");
		assertThat(home.workingToday()).singleElement().satisfies(cast -> {
			assertThat(cast.name()).isEqualTo("Mina");
			assertThat(cast.followed()).isTrue();
			assertThat(cast.shiftTime()).isEqualTo("14:00 - 20:00");
		});
		assertThat(home.featuredEvents()).extracting(event -> event.title())
			.containsExactly("Published Night");
	}

	private UserAccount account(String email, String name, UserRole role) {
		return userAccountRepository.save(new UserAccount(email, "encoded", name, role, UserStatus.ACTIVE));
	}

	private CastProfile cast(String email, String stageName) {
		var account = account(email, stageName, UserRole.CAST);
		return castProfileRepository.save(new CastProfile(
			account, stageName, stageName.toLowerCase(), null, null, true
		));
	}
}

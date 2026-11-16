package com.castfan.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;

import com.castfan.entity.CastFollow;
import com.castfan.entity.CastContent;
import com.castfan.entity.CastProfile;
import com.castfan.entity.ContentAudience;
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
import com.castfan.repository.CastContentRepository;
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
	@Autowired private CastContentRepository castContentRepository;

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
		var followedPost = CastContent.forStoreTier(
			mina, "Mina update", "A new update for everyone following Mina", null,
			ContentAudience.FOLLOWERS, null
		);
		followedPost.publish();
		castContentRepository.save(followedPost);
		var unrelatedPost = CastContent.forStoreTier(
			yuki, "Yuki update", "This must not appear in Mina's feed", null,
			ContentAudience.PUBLIC, null
		);
		unrelatedPost.publish();
		castContentRepository.save(unrelatedPost);

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
		assertThat(home.followingContent()).singleElement().satisfies(item -> {
			assertThat(item.castProfileId()).isEqualTo(mina.getId());
			assertThat(item.title()).isEqualTo("Mina update");
			assertThat(item.bodyPreview()).contains("following Mina");
			assertThat(item.accessible()).isTrue();
		});
	}

	@Test
	void storeScopedHomeOnlyUsesTheSelectedStoreEvents() {
		var member = account("member-scoped@test.local", "Scoped Member", UserRole.MEMBER);
		memberProfileRepository.save(new MemberProfile(member, null, null));
		var firstStore = store("First Scope Cafe", "first-scope");
		var secondStore = store("Second Scope Cafe", "second-scope");
		var startsAt = LocalDate.now(STORE_ZONE).plusDays(3).atTime(16, 0).atZone(STORE_ZONE).toInstant();
		storeEventRepository.save(new StoreEvent(
			firstStore, firstStore.getOwner(), "First Scope Event", "First event", null,
			startsAt, startsAt.plusSeconds(3600), EventStatus.PUBLISHED
		));
		storeEventRepository.save(new StoreEvent(
			secondStore, secondStore.getOwner(), "Second Scope Event", "Second event", null,
			startsAt, startsAt.plusSeconds(3600), EventStatus.PUBLISHED
		));

		var home = homeService.load(member.getEmail(), firstStore.getSlug());

		assertThat(home.storeName()).isEqualTo("First Scope Cafe");
		assertThat(home.featuredEvents()).extracting(event -> event.title())
			.containsExactly("First Scope Event");
	}

	@Test
	void castHomeUsesTheCastAssignedStore() {
		var store = store("Cast Home Cafe", "cast-home");
		var castAccount = account("cast-home-rei@test.local", "Rei", UserRole.CAST);
		var castProfile = castProfileRepository.save(new CastProfile(
			castAccount, store, "Rei", "rei-cast-home", null, null
		));
		var assignment = storeCastRepository.save(new StoreCast(store, castProfile, true));
		var today = LocalDate.now(STORE_ZONE);
		var shift = new ShiftSchedule(
			assignment,
			today.atTime(14, 0).atZone(STORE_ZONE).toInstant(),
			today.atTime(20, 0).atZone(STORE_ZONE).toInstant()
		);
		shift.approve(store.getOwner(), null);
		shiftScheduleRepository.save(shift);
		var startsAt = today.plusDays(2).atTime(16, 0).atZone(STORE_ZONE).toInstant();
		storeEventRepository.save(new StoreEvent(
			store, store.getOwner(), "Cast Home Event", "Event", null,
			startsAt, startsAt.plusSeconds(3600), EventStatus.PUBLISHED
		));

		var home = homeService.load(castAccount.getEmail());

		assertThat(home.storeName()).isEqualTo("Cast Home Cafe");
		assertThat(home.workingToday()).extracting(cast -> cast.name()).containsExactly("Rei");
		assertThat(home.featuredEvents()).extracting(event -> event.title()).contains("Cast Home Event");
	}

	@Test
	void homeGreetingUsesLatestMemberDisplayName() {
		var member = account("member-greeting@test.local", "Original Member", UserRole.MEMBER);
		memberProfileRepository.save(new MemberProfile(member, null, null));

		member.updateDisplayName("Aruna Fan");
		userAccountRepository.saveAndFlush(member);

		var home = homeService.load(member.getEmail());

		assertThat(home.memberName()).isEqualTo("Aruna Fan");
	}

	@Test
	void homeGreetingUsesCurrentCastStageName() {
		var castAccount = account("cast-greeting@test.local", "Old Account Name", UserRole.CAST);
		castProfileRepository.saveAndFlush(new CastProfile(
			castAccount, "New Stage Name", "new-stage-name", null, null
		));

		var home = homeService.load(castAccount.getEmail());

		assertThat(home.memberName()).isEqualTo("New Stage Name");
	}

	private Store store(String name, String slug) {
		var owner = account(slug + "-owner@test.local", name + " Owner", UserRole.OWNER);
		return storeRepository.save(new Store(owner, name, slug, null, null, STORE_ZONE.getId(), true));
	}

	private UserAccount account(String email, String name, UserRole role) {
		return userAccountRepository.save(new UserAccount(email, "encoded", name, role, UserStatus.ACTIVE));
	}

	private CastProfile cast(String email, String stageName) {
		var account = account(email, stageName, UserRole.CAST);
		return castProfileRepository.save(new CastProfile(
			account, stageName, stageName.toLowerCase(), null, null
		));
	}
}

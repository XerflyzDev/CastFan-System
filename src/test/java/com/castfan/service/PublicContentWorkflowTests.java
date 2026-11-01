package com.castfan.service;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

import com.castfan.entity.CastProfile;
import com.castfan.entity.EventStatus;
import com.castfan.entity.MemberTier;
import com.castfan.entity.ShiftSchedule;
import com.castfan.entity.Store;
import com.castfan.entity.StoreCast;
import com.castfan.entity.StoreEvent;
import com.castfan.entity.UserAccount;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.CastProfileRepository;
import com.castfan.repository.MemberTierRepository;
import com.castfan.repository.ShiftScheduleRepository;
import com.castfan.repository.StoreCastRepository;
import com.castfan.repository.StoreEventRepository;
import com.castfan.repository.StoreRepository;
import com.castfan.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PublicContentWorkflowTests {

	private static final ZoneId STORE_ZONE = ZoneId.of("Asia/Bangkok");

	@Autowired private MockMvc mockMvc;
	@Autowired private UserAccountRepository userAccountRepository;
	@Autowired private CastProfileRepository castProfileRepository;
	@Autowired private StoreRepository storeRepository;
	@Autowired private StoreCastRepository storeCastRepository;
	@Autowired private ShiftScheduleRepository shiftScheduleRepository;
	@Autowired private StoreEventRepository storeEventRepository;
	@Autowired private MemberTierRepository memberTierRepository;

	@Test
	void publicDetailsExposeAllActiveCastAndOnlyPublishedStoreContent() throws Exception {
		var owner = account("public-owner@test.local", "Owner", UserRole.OWNER);
		var store = storeRepository.save(new Store(
			owner, "Hoshi Cafe", "public-hoshi", null, "Bangkok, Thailand", STORE_ZONE.getId(), true
		));
		var mina = cast("public-mina@test.local", "Mina Public");
		var secondCast = cast("public-second@test.local", "Second Public");
		var assignment = storeCastRepository.save(new StoreCast(store, mina, true));
		storeCastRepository.save(new StoreCast(store, secondCast, true));
		memberTierRepository.save(new MemberTier(
			mina, "Silver", "silver", "Member updates", 1, new BigDecimal("199.00"), "THB"
		));

		var eventDate = LocalDate.now(STORE_ZONE).plusDays(1);
		var eventStartsAt = eventDate.atTime(16, 0).atZone(STORE_ZONE).toInstant();
		var eventEndsAt = eventDate.atTime(21, 0).atZone(STORE_ZONE).toInstant();
		var shift = new ShiftSchedule(assignment, eventStartsAt, eventEndsAt);
		shift.approve(owner, null);
		shiftScheduleRepository.save(shift);
		var published = storeEventRepository.save(new StoreEvent(
			store, owner, "Public Event", "Published event details", null,
			eventStartsAt, eventEndsAt, EventStatus.PUBLISHED
		));
		var draft = storeEventRepository.save(new StoreEvent(
			store, owner, "Draft Event", "Draft details", null,
			eventStartsAt, eventEndsAt, EventStatus.DRAFT
		));

		mockMvc.perform(get("/casts/{id}", mina.getId()))
			.andExpect(status().isOk())
			.andExpect(view().name("public/cast-profile"))
			.andExpect(model().attributeExists("cast"))
			.andExpect(content().string(containsString("Mina Public")))
			.andExpect(content().string(org.hamcrest.Matchers.not(containsString("Share profile"))))
			.andExpect(content().string(org.hamcrest.Matchers.not(containsString("Hoshi Cafe Cast"))))
			.andExpect(content().string(containsString("Silver")));
		mockMvc.perform(get("/casts/{id}", secondCast.getId()))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Second Public")));

		mockMvc.perform(get("/events/{id}", published.getId()))
			.andExpect(status().isOk())
			.andExpect(view().name("public/event-detail"))
			.andExpect(content().string(containsString("Public Event")))
			.andExpect(content().string(containsString("Mina Public")));
		mockMvc.perform(get("/events/{id}", draft.getId()))
			.andExpect(status().isNotFound());

		mockMvc.perform(get("/events/{id}/calendar.ics", published.getId()))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/calendar"))
			.andExpect(content().string(containsString("BEGIN:VCALENDAR")))
			.andExpect(content().string(containsString("SUMMARY:Public Event")));
	}

	private UserAccount account(String email, String name, UserRole role) {
		return userAccountRepository.save(new UserAccount(email, "encoded", name, role, UserStatus.ACTIVE));
	}

	private CastProfile cast(String email, String stageName) {
		return castProfileRepository.save(new CastProfile(
			account(email, stageName, UserRole.CAST),
			stageName,
			stageName.toLowerCase().replace(' ', '-'),
			stageName + " profile",
			null
		));
	}
}

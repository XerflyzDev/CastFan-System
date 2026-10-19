package com.castfan.service;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.castfan.dto.OwnerEventForm;
import com.castfan.dto.OwnerEventView;
import com.castfan.entity.EventStatus;
import com.castfan.entity.Store;
import com.castfan.entity.StoreEvent;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.StoreEventRepository;
import com.castfan.repository.StoreRepository;
import com.castfan.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OwnerEventService {

	private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Bangkok");
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

	private final UserAccountRepository userAccountRepository;
	private final StoreRepository storeRepository;
	private final StoreEventRepository storeEventRepository;
	private final StoreEventImageService storeEventImageService;

	public OwnerEventService(
		UserAccountRepository userAccountRepository,
		StoreRepository storeRepository,
		StoreEventRepository storeEventRepository,
		StoreEventImageService storeEventImageService
	) {
		this.userAccountRepository = userAccountRepository;
		this.storeRepository = storeRepository;
		this.storeEventRepository = storeEventRepository;
		this.storeEventImageService = storeEventImageService;
	}

	@Transactional(readOnly = true)
	public OwnerEventView load(String email, Long editingEventId) {
		var account = requireOwner(email);
		var stores = storeRepository.findAllByOwnerIdOrderByNameAsc(account.getId());
		var events = storeEventRepository.findAllForOwner(email).stream().map(this::toEventItem).toList();
		return new OwnerEventView(
			stores.stream().filter(Store::isActive)
				.map(store -> new OwnerEventView.StoreOption(store.getId(), store.getName(), store.getTimezone()))
				.toList(),
			events,
			editingEventId,
			events.stream().filter(event -> event.id().equals(editingEventId)).map(OwnerEventView.EventItem::imageUrl).findFirst().orElse(null)
		);
	}

	@Transactional(readOnly = true)
	public OwnerEventForm formForEdit(String email, Long eventId) {
		var event = requireOwnedEvent(email, eventId);
		if (event.getStatus() == EventStatus.ARCHIVED) {
			throw new DomainRuleViolationException("Archived events cannot be edited.");
		}
		var zone = safeZone(event.getStore().getTimezone());
		var startsAt = event.getStartsAt().atZone(zone);
		var endsAt = event.getEndsAt().atZone(zone);
		var form = new OwnerEventForm();
		form.setStoreId(event.getStore().getId());
		form.setTitle(event.getTitle());
		form.setDescription(event.getDescription());
		form.setDate(startsAt.toLocalDate());
		form.setStartsAt(startsAt.toLocalTime());
		form.setEndsAt(endsAt.toLocalTime());
		return form;
	}

	@Transactional
	public StoreEvent create(String email, OwnerEventForm form, MultipartFile image) {
		var owner = requireOwner(email);
		var store = requireOwnedStore(owner.getId(), form.getStoreId());
		var range = eventRange(form, store);
		var event = storeEventRepository.saveAndFlush(new StoreEvent(
			store,
			owner,
			cleanRequired(form.getTitle(), "Title", 160),
			cleanRequired(form.getDescription(), "Description", 2000),
			null,
			range.startsAt(),
			range.endsAt(),
			EventStatus.DRAFT
		));
		storeEventImageService.store(event, image);
		return event;
	}

	@Transactional
	public void update(String email, Long eventId, OwnerEventForm form, MultipartFile image) {
		var event = requireOwnedEventForUpdate(email, eventId);
		if (!event.getStore().getId().equals(form.getStoreId())) {
			throw new DomainRuleViolationException("An event cannot be moved to another store.");
		}
		var range = eventRange(form, event.getStore());
		try {
			event.updateDetails(
				cleanRequired(form.getTitle(), "Title", 160),
				cleanRequired(form.getDescription(), "Description", 2000),
				form.isRemoveImage() ? null : event.getImageUrl(),
				range.startsAt(),
				range.endsAt()
			);
		} catch (IllegalArgumentException | IllegalStateException exception) {
			throw new DomainRuleViolationException(exception.getMessage());
		}
		storeEventRepository.saveAndFlush(event);
		if (form.isRemoveImage()) storeEventImageService.remove(event);
		storeEventImageService.store(event, image);
	}

	@Transactional
	public void publish(String email, Long eventId) {
		var event = requireOwnedEventForUpdate(email, eventId);
		if (!event.getEndsAt().isAfter(Instant.now())) {
			throw new DomainRuleViolationException("Past events cannot be published.");
		}
		try {
			event.publish();
		} catch (IllegalStateException exception) {
			throw new DomainRuleViolationException(exception.getMessage());
		}
		storeEventRepository.saveAndFlush(event);
	}

	@Transactional
	public void archive(String email, Long eventId) {
		var event = requireOwnedEventForUpdate(email, eventId);
		try {
			event.archive();
		} catch (IllegalStateException exception) {
			throw new DomainRuleViolationException(exception.getMessage());
		}
		storeEventRepository.saveAndFlush(event);
	}

	private OwnerEventView.EventItem toEventItem(StoreEvent event) {
		var zone = safeZone(event.getStore().getTimezone());
		var startsAt = event.getStartsAt().atZone(zone);
		var endsAt = event.getEndsAt().atZone(zone);
		return new OwnerEventView.EventItem(
			event.getId(), event.getTitle(), event.getDescription(), storeEventImageService.ownerImageUrl(event), event.getStore().getName(),
			DATE.format(startsAt), TIME.format(startsAt) + " - " + TIME.format(endsAt),
			statusLabel(event.getStatus()), event.getStatus().name().toLowerCase(Locale.ENGLISH),
			event.getStatus() != EventStatus.ARCHIVED, event.getStatus() == EventStatus.DRAFT,
			event.getStatus() != EventStatus.ARCHIVED, event.getStatus() == EventStatus.PUBLISHED
		);
	}

	private EventRange eventRange(OwnerEventForm form, Store store) {
		if (form.getEndsAt().equals(form.getStartsAt())) {
			throw new DomainRuleViolationException("Event end time must be different from its start time.");
		}
		var zone = safeZone(store.getTimezone());
		var start = form.getDate().atTime(form.getStartsAt()).atZone(zone).toInstant();
		var endDate = form.getEndsAt().isAfter(form.getStartsAt()) ? form.getDate() : form.getDate().plusDays(1);
		var end = endDate.atTime(form.getEndsAt()).atZone(zone).toInstant();
		return new EventRange(start, end);
	}

	private com.castfan.entity.UserAccount requireOwner(String email) {
		return userAccountRepository.findByEmailIgnoreCase(email)
			.filter(account -> account.getRole() == UserRole.OWNER && account.getStatus() == UserStatus.ACTIVE)
			.orElseThrow(() -> new DomainRuleViolationException("Owner account not found."));
	}

	private Store requireOwnedStore(Long ownerId, Long storeId) {
		return storeRepository.findById(storeId)
			.filter(store -> store.getOwner().getId().equals(ownerId) && store.isActive())
			.orElseThrow(() -> new DomainRuleViolationException("Active store not found."));
	}

	private StoreEvent requireOwnedEvent(String email, Long eventId) {
		return storeEventRepository.findById(eventId)
			.filter(event -> event.getStore().getOwner().getEmail().equalsIgnoreCase(email))
			.orElseThrow(() -> new DomainRuleViolationException("Event not found."));
	}

	private StoreEvent requireOwnedEventForUpdate(String email, Long eventId) {
		return storeEventRepository.findByIdForUpdate(eventId)
			.filter(event -> event.getStore().getOwner().getEmail().equalsIgnoreCase(email))
			.orElseThrow(() -> new DomainRuleViolationException("Event not found."));
	}

	private String cleanRequired(String value, String field, int maximumLength) {
		var clean = value == null ? "" : value.trim();
		if (clean.isEmpty()) throw new DomainRuleViolationException(field + " is required.");
		if (clean.length() > maximumLength) throw new DomainRuleViolationException(field + " is too long.");
		return clean;
	}

	private String statusLabel(EventStatus status) {
		return switch (status) {
			case DRAFT -> "Draft";
			case PUBLISHED -> "Published";
			case ARCHIVED -> "Archived";
		};
	}

	private ZoneId safeZone(String timezone) {
		try { return ZoneId.of(timezone); } catch (DateTimeException exception) { return DEFAULT_ZONE; }
	}

	private record EventRange(Instant startsAt, Instant endsAt) { }
}

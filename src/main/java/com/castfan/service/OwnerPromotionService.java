package com.castfan.service;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.castfan.dto.OwnerPromotionForm;
import com.castfan.dto.OwnerPromotionView;
import com.castfan.entity.PromotionStatus;
import com.castfan.entity.Store;
import com.castfan.entity.StorePromotion;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.StorePromotionRepository;
import com.castfan.repository.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OwnerPromotionService {
	private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Bangkok");
	private static final DateTimeFormatter RANGE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH);

	private final UserAccountRepository userAccountRepository;
	private final SystemStoreService systemStoreService;
	private final StorePromotionRepository storePromotionRepository;
	private final StorePromotionImageService storePromotionImageService;
	private final PromotionLifecycleService promotionLifecycleService;

	public OwnerPromotionService(UserAccountRepository userAccountRepository, SystemStoreService systemStoreService,
		StorePromotionRepository storePromotionRepository, StorePromotionImageService storePromotionImageService,
		PromotionLifecycleService promotionLifecycleService) {
		this.userAccountRepository = userAccountRepository;
		this.systemStoreService = systemStoreService;
		this.storePromotionRepository = storePromotionRepository;
		this.storePromotionImageService = storePromotionImageService;
		this.promotionLifecycleService = promotionLifecycleService;
	}

	@Transactional(readOnly = true)
	public OwnerPromotionView load(String email, Long editingPromotionId) {
		promotionLifecycleService.archiveExpiredPromotions();
		var owner = requireOwner(email);
		return new OwnerPromotionView(
			storePromotionRepository.findAllForOwner(email).stream().map(this::toItem).toList(),
			editingPromotionId,
			editingPromotionId == null ? null : storePromotionRepository.findById(editingPromotionId)
				.filter(value -> value.getStore().getOwner().getId().equals(owner.getId()))
				.map(storePromotionImageService::ownerUrl).orElse(null)
		);
	}

	@Transactional(readOnly = true)
	public OwnerPromotionForm formForEdit(String email, Long promotionId) {
		promotionLifecycleService.archiveExpiredPromotions();
		var promotion = requireOwnedPromotion(email, promotionId);
		if (promotion.getStatus() == PromotionStatus.ARCHIVED) {
			throw new DomainRuleViolationException("Archived promotions cannot be edited.");
		}
		if (!promotion.getEndsAt().isAfter(Instant.now())) {
			throw new DomainRuleViolationException("Expired promotions cannot be edited.");
		}
		var zone = safeZone(promotion.getStore().getTimezone());
		var startsAt = promotion.getStartsAt().atZone(zone);
		var endsAt = promotion.getEndsAt().atZone(zone);
		var form = new OwnerPromotionForm();
		form.setTitle(promotion.getTitle());
		form.setDescription(promotion.getDescription());
		form.setCode(promotion.getCode());
		form.setStartsOn(startsAt.toLocalDate());
		form.setStartsAt(startsAt.toLocalTime());
		form.setEndsOn(endsAt.toLocalDate());
		form.setEndsAt(endsAt.toLocalTime());
		return form;
	}

	@Transactional
	public StorePromotion create(String email, OwnerPromotionForm form, MultipartFile image) {
		var owner = requireOwner(email);
		var store = systemStoreService.requireForOwner(owner);
		var code = cleanCode(form.getCode());
		requireUniqueCode(code, null);
		var range = range(form, store);
		try {
			var promotion = storePromotionRepository.saveAndFlush(new StorePromotion(
				store, 
				owner, 
				cleanRequired(form.getTitle(), "Title", 160),
				cleanRequired(form.getDescription(), "Description", 2000), 
				code,
				null, 
				range.startsAt(), 
				range.endsAt(),
				PromotionStatus.DRAFT
			));
			storePromotionImageService.store(promotion, image);
			return promotion;
		} catch (DataIntegrityViolationException exception) {
			throw new DomainConflictException("Promotion code is already in use.");
		}
	}

	@Transactional
	public void update(String email, Long promotionId, OwnerPromotionForm form, MultipartFile image) {
		promotionLifecycleService.archiveExpiredPromotions();
		var promotion = requireOwnedPromotionForUpdate(email, promotionId);
		if (promotion.getStatus() == PromotionStatus.ARCHIVED) {
			throw new DomainRuleViolationException("Archived promotions cannot be edited.");
		}
		if (!promotion.getEndsAt().isAfter(Instant.now())) {
			throw new DomainRuleViolationException("Expired promotions cannot be edited.");
		}
		var code = cleanCode(form.getCode());
		requireUniqueCode(code, promotionId);
		var range = range(form, promotion.getStore());
		promotion.updateDetails(cleanRequired(form.getTitle(), "Title", 160),
			cleanRequired(form.getDescription(), "Description", 2000), code,
			(form.isRemoveImage() || (image != null && !image.isEmpty())) ? null : promotion.getBannerUrl(),
			range.startsAt(), range.endsAt());
		try {
			storePromotionRepository.saveAndFlush(promotion);
			if (image != null && !image.isEmpty()) storePromotionImageService.store(promotion, image);
			else if (form.isRemoveImage()) storePromotionImageService.remove(promotion);
		} catch (DataIntegrityViolationException exception) {
			throw new DomainConflictException("Promotion code is already in use.");
		}
	}

	@Transactional
	public void publish(String email, Long promotionId) {
		promotionLifecycleService.archiveExpiredPromotions();
		var promotion = requireOwnedPromotionForUpdate(email, promotionId);
		if (!promotion.getEndsAt().isAfter(Instant.now())) {
			throw new DomainRuleViolationException("Past promotions cannot be published.");
		}
		if (!promotion.getStore().isActive()) {
			throw new DomainRuleViolationException("Promotions cannot be published for an inactive store.");
		}
		try {
			promotion.publish();
		} catch (IllegalStateException exception) {
			throw new DomainRuleViolationException(exception.getMessage());
		}
		storePromotionRepository.saveAndFlush(promotion);
	}

	@Transactional
	public void archive(String email, Long promotionId) {
		var promotion = requireOwnedPromotionForUpdate(email, promotionId);
		try {
			promotion.archive();
		} catch (IllegalStateException exception) {
			throw new DomainRuleViolationException(exception.getMessage());
		}
		storePromotionRepository.saveAndFlush(promotion);
	}

	private OwnerPromotionView.PromotionItem toItem(StorePromotion promotion) {
		var now = Instant.now();
		var zone = safeZone(promotion.getStore().getTimezone());
		var status = promotion.getStatus();
		var expired = status != PromotionStatus.ARCHIVED && !now.isBefore(promotion.getEndsAt());
		var state = status == com.castfan.entity.PromotionStatus.ARCHIVED || expired ? "Archived"
			: status == com.castfan.entity.PromotionStatus.DRAFT ? "Draft"
			: now.isBefore(promotion.getStartsAt()) ? "Scheduled"
			: "Active";
		return new OwnerPromotionView.PromotionItem(
			promotion.getId(), promotion.getTitle(), promotion.getDescription(), promotion.getCode(),
			storePromotionImageService.ownerUrl(promotion),
			RANGE_FORMAT.format(promotion.getStartsAt().atZone(zone)) + " - "
				+ RANGE_FORMAT.format(promotion.getEndsAt().atZone(zone)),
			state, state.toLowerCase(Locale.ENGLISH), 
			status != PromotionStatus.ARCHIVED && !expired,
			status == PromotionStatus.DRAFT && promotion.getEndsAt().isAfter(now),
			status != PromotionStatus.ARCHIVED && !expired
		);
	}

	private PromotionRange range(OwnerPromotionForm form, Store store) {
		var zone = safeZone(store.getTimezone());
		var start = form.getStartsOn().atTime(form.getStartsAt()).atZone(zone).toInstant();
		var end = form.getEndsOn().atTime(form.getEndsAt()).atZone(zone).toInstant();
		if (!end.isAfter(start)) throw new DomainRuleViolationException("Promotion end time must be after its start time.");
		return new PromotionRange(start, end);
	}

	private com.castfan.entity.UserAccount requireOwner(String email) {
		return userAccountRepository.findByLoginIdentifierIgnoreCase(email)
			.filter(account -> account.getRole() == UserRole.OWNER && account.getStatus() == UserStatus.ACTIVE)
			.orElseThrow(() -> new DomainRuleViolationException("Owner account not found."));
	}

	private StorePromotion requireOwnedPromotion(String email, Long promotionId) {
		return storePromotionRepository.findById(promotionId)
			.filter(promotion -> promotion.getStore().getOwner().getEmail().equalsIgnoreCase(email))
			.orElseThrow(() -> new DomainRuleViolationException("Promotion not found."));
	}

	private StorePromotion requireOwnedPromotionForUpdate(String email, Long promotionId) {
		return storePromotionRepository.findByIdForUpdate(promotionId)
			.filter(promotion -> promotion.getStore().getOwner().getEmail().equalsIgnoreCase(email))
			.orElseThrow(() -> new DomainRuleViolationException("Promotion not found."));
	}

	private void requireUniqueCode(String code, Long promotionId) {
		if (code == null) return;
		var duplicate = promotionId == null ? storePromotionRepository.existsByCodeIgnoreCase(code)
			: storePromotionRepository.existsByCodeIgnoreCaseAndIdNot(code, promotionId);
		if (duplicate) throw new DomainConflictException("Promotion code is already in use.");
	}

	private String cleanRequired(String value, String field, int maximumLength) {
		var clean = value == null ? "" : value.trim();
		if (clean.isEmpty()) throw new DomainRuleViolationException(field + " is required.");
		if (clean.length() > maximumLength) throw new DomainRuleViolationException(field + " is too long.");
		return clean;
	}

	private String cleanOptional(String value) { return value == null || value.isBlank() ? null : value.trim(); }
	private String cleanCode(String value) {
		var clean = cleanOptional(value);
		return clean == null ? null : clean.toUpperCase(Locale.ENGLISH);
	}
	private ZoneId safeZone(String timezone) {
		try { return ZoneId.of(timezone); } catch (DateTimeException exception) { return DEFAULT_ZONE; }
	}
	private record PromotionRange(Instant startsAt, Instant endsAt) { }
}

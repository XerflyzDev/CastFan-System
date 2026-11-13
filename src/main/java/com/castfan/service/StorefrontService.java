package com.castfan.service;

import java.time.Instant;
import java.util.List;

import com.castfan.entity.EventStatus;
import com.castfan.entity.PromotionStatus;
import com.castfan.repository.StoreCastRepository;
import com.castfan.repository.StoreEventRepository;
import com.castfan.repository.StorePromotionRepository;
import com.castfan.repository.StoreRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StorefrontService {
	private final StoreRepository stores;
	private final StoreCastRepository casts;
	private final StoreEventRepository events;
	private final StorePromotionRepository promotions;

	public StorefrontService(StoreRepository stores, StoreCastRepository casts,
		StoreEventRepository events, StorePromotionRepository promotions) {
		this.stores = stores; this.casts = casts; this.events = events; this.promotions = promotions;
	}

	@Transactional(readOnly = true)
	public StorefrontView load(String slug) {
		var store = stores.findBySlugAndActiveTrue(slug).orElseThrow(() -> new StoreNotFoundException(slug));
		var now = Instant.now();
		var castViews = casts.findAllByStoreIdAndActiveTrueOrderByCastProfileStageNameAsc(store.getId()).stream()
				.map(assignment -> new CastView(assignment.getCastProfile().getId(),
					assignment.getCastProfile().getStageName())).toList();
		var eventViews = events.findAllByStoreIdAndStatusAndEndsAtAfterOrderByStartsAtAsc(
			store.getId(), EventStatus.PUBLISHED, now, PageRequest.of(0, 6)).stream()
			.map(event -> new EventView(event.getId(), event.getTitle(), event.getDescription(),
				event.getStartsAt(), event.getEndsAt())).toList();
		var promotionViews = promotions.findAllByStoreIdAndStatusAndStartsAtLessThanEqualAndEndsAtAfterOrderByStartsAtDesc(
			store.getId(), PromotionStatus.ACTIVE, now, now, PageRequest.of(0, 6)).stream()
			.map(promotion -> new PromotionView(promotion.getId(), promotion.getTitle(), promotion.getDescription(),
				promotion.getStartsAt(), promotion.getEndsAt())).toList();
		return new StorefrontView(StoreView.from(store), castViews, eventViews, promotionViews);
	}

	public record StorefrontView(StoreView store, List<CastView> casts, List<EventView> events,
		List<PromotionView> promotions) { }

	public record StoreView(Long id, String name, String slug, String description, String address) {
		static StoreView from(com.castfan.entity.Store store) {
			return new StoreView(store.getId(), store.getName(), store.getSlug(), store.getDescription(), store.getAddress());
		}
	}
	public record CastView(Long id, String stageName) { }
	public record EventView(Long id, String title, String description, Instant startsAt, Instant endsAt) { }
	public record PromotionView(Long id, String title, String description, Instant startsAt, Instant endsAt) { }

	public static class StoreNotFoundException extends RuntimeException {
		public StoreNotFoundException(String slug) { super("Store not found: " + slug); }
	}
}

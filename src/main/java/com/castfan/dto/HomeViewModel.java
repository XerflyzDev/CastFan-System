package com.castfan.dto;

import java.util.List;

public record HomeViewModel(
	String memberName,
	List<CastSummary> workingToday,
	List<CastSummary> followedCast,
	FeaturedEvent featuredEvent
) {

	public static HomeViewModel preview() {
		var mina = new CastSummary("Mina", "MN", "Hoshi Cafe", "14:00 - 20:00", "rose", true, true);
		var yumi = new CastSummary("Yumi", "YM", "Hoshi Cafe", "14:00 - 20:00", "violet", true, true);
		var luna = new CastSummary("Luna", "LN", "Hoshi Cafe", "16:00 - 21:00", "amber", true, true);
		var pim = new CastSummary("Pim", "PM", "Hoshi Cafe", "12:00 - 18:00", "mint", true, false);

		return new HomeViewModel(
			"Member",
			List.of(mina, yumi, luna, pim),
			List.of(mina, yumi, luna),
			new FeaturedEvent(
				"Autumn Sweet Night",
				"October 31, 2026",
				"16:00 - 21:00",
				"A special evening with strawberry desserts and your favorite Cast members.",
				"/images/autumn-cafe-event.png"
			)
		);
	}

	public record CastSummary(
		String name,
		String initials,
		String cafeName,
		String shiftTime,
		String avatarTone,
		boolean workingNow,
		boolean followed
	) {
	}

	public record FeaturedEvent(
		String title,
		String date,
		String time,
		String description,
		String imageUrl
	) {
	}
}

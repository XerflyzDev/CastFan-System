package com.castfan.dto;

public record PublicContentDetailView(
	Long id,
	Long castProfileId,
	String castName,
	String castInitials,
	String castAvatarUrl,
	String title,
	String body,
	String imageUrl,
	String audience,
	String publishedLabel,
	boolean accessible
) { }

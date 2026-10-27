package com.castfan.controller;

import java.security.Principal;

import com.castfan.service.MemberNotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@PreAuthorize("hasAnyRole('MEMBER', 'CAST')")
public class MemberNotificationController {

	private final MemberNotificationService memberNotificationService;

	public MemberNotificationController(MemberNotificationService memberNotificationService) {
		this.memberNotificationService = memberNotificationService;
	}

	@GetMapping("/member/notifications")
	public String notifications(Principal principal, Model model) {
		model.addAttribute("notifications", memberNotificationService.load(principal.getName()));
		return "member/notifications";
	}
}

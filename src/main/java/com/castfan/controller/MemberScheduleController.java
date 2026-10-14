package com.castfan.controller;

import java.time.LocalDate;

import com.castfan.service.MemberScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@PreAuthorize("hasRole('MEMBER')")
public class MemberScheduleController {

	private final MemberScheduleService memberScheduleService;

	public MemberScheduleController(MemberScheduleService memberScheduleService) {
		this.memberScheduleService = memberScheduleService;
	}

	@GetMapping("/member/schedule")
	public String schedule(
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
		Model model
	) {
		model.addAttribute("pageTitle", "Schedule");
		model.addAttribute("schedule", memberScheduleService.load(date));
		return "member/schedule";
	}
}

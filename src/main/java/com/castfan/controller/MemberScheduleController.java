package com.castfan.controller;

import java.time.LocalDate;

import com.castfan.service.MemberScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Controller
@PreAuthorize("hasAnyRole('MEMBER', 'CAST')")
public class MemberScheduleController {

	private final MemberScheduleService memberScheduleService;

	public MemberScheduleController(MemberScheduleService memberScheduleService) {
		this.memberScheduleService = memberScheduleService;
	}

	@GetMapping("/member/schedule")
	public String schedule(
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
		@RequestParam(defaultValue = "day") String view,
		Model model
	) {
		if (!view.equals("day") && !view.equals("week")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported schedule view");
		}
		model.addAttribute("pageTitle", "Schedule");
		model.addAttribute("schedule", memberScheduleService.load(date, view.equals("week")));
		return "member/schedule";
	}
}

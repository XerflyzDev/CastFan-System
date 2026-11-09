package com.castfan.controller;

import com.castfan.service.PublicPromotionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PublicPromotionController {

    private static final int PAGE_SIZE = 12;

    private final PublicPromotionService publicPromotionService;

    public PublicPromotionController(PublicPromotionService publicPromotionService) {
        this.publicPromotionService = publicPromotionService;
    }

    @GetMapping("/promotions")
    public String promotions(
        @RequestParam(defaultValue = "0") int page,
        Model model
    ) {
        var safePage = Math.max(page, 0);

        model.addAttribute("pageTitle", "Promotions");
        model.addAttribute(
            "promotions",
            publicPromotionService.findCurrentPublicPromotions(
                PageRequest.of(safePage, PAGE_SIZE)
            )
        );

        return "promotions/index";
    }
}
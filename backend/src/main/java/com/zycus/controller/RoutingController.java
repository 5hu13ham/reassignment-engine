package com.zycus.controller;

import com.zycus.service.RoutingService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/routing")
public class RoutingController {
    private final RoutingService routing;
    public RoutingController(RoutingService routing) {this.routing=routing;}

    public record StrategyStatus(String activeStrategy) {}
    public record StrategyRequest(@NotBlank String strategy) {}

    @GetMapping("/strategy")
    public StrategyStatus getStrategy() {return new StrategyStatus(routing.getActiveStrategy());}

    @PatchMapping("/strategy")
    public StrategyStatus setStrategy(@RequestBody StrategyRequest request) {
        return new StrategyStatus(routing.setActiveStrategy(request.strategy()));
    }
}

package com.zycus.controller;

import com.zycus.dto.*;
import com.zycus.entity.OrderStatus;
import com.zycus.entity.TriggerReason;
import com.zycus.exception.NoAvailableAgentException;
import com.zycus.mapper.SuggestionMapper;
import com.zycus.service.OrderService;
import com.zycus.service.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orders;
    private final RecommendationService recommendations;
    private final SuggestionMapper suggestions;
    public OrderController(OrderService orders,RecommendationService recommendations,SuggestionMapper suggestions) {
        this.orders=orders;this.recommendations=recommendations;this.suggestions=suggestions;
    }

    @GetMapping
    public List<OrderResponse> list(@RequestParam(required=false) OrderStatus status) {
        return orders.getOrders(status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        return orders.createOrder(request);
    }

    @PostMapping("/{id}/suggest")
    public SuggestionResponse suggest(@PathVariable Long id) {
        var result=recommendations.generateRecommendation(id,TriggerReason.INITIAL);
        if(result==null) throw new NoAvailableAgentException();
        return suggestions.toResponse(result);
    }
}

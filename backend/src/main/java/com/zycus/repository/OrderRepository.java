package com.zycus.repository;

import com.zycus.entity.Order;
import com.zycus.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByAssignedAgentId(Long agentId);
    List<Order> findByAssignedAgentIdAndStatus(Long agentId, OrderStatus status);
    long countByAssignedAgentIdAndStatusIn(Long agentId, List<OrderStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o join fetch o.assignedAgent where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);
}

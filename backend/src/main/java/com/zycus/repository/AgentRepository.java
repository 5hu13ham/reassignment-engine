package com.zycus.repository;

import com.zycus.entity.Agent;
import com.zycus.entity.AgentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentRepository extends JpaRepository<Agent, Long> {

    List<Agent> findByStatus(AgentStatus status);
}

package com.example.dify2agentscope.adapter.in.a2a;

import com.example.dify2agentscope.application.a2a.A2aService;
import com.example.dify2agentscope.domain.a2a.A2aAgentCard;
import com.example.dify2agentscope.domain.a2a.A2aTask;
import com.example.dify2agentscope.domain.a2a.A2aTaskRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A2A（Agent-to-Agent）控制器 —— 提供 Agent 发现与任务创建/查询接口。
 * A2A (Agent-to-Agent) controller — provides agent discovery and task
 * creation / query endpoints.
 */
@RestController
@RequestMapping("/api/v1/a2a")
public class A2aController {

    private final A2aService a2aService;

    /**
     * 构造 A2A 控制器。
     * Constructs the A2A controller.
     *
     * @param a2aService A2A 服务 / A2A service
     */
    public A2aController(A2aService a2aService) {
        this.a2aService = a2aService;
    }

    /**
     * 获取所有可用 Agent 卡片列表。
     * List all available agent cards.
     *
     * @return Agent 卡片列表 / list of agent cards
     */
    @GetMapping("/agents")
    public List<A2aAgentCard> agents() {
        return a2aService.listAgents();
    }

    /**
     * 根据 ID 获取指定 Agent 的详细信息。
     * Get detailed information for a specific agent by ID.
     *
     * @param agentId Agent ID / agent identifier
     * @return Agent 卡片 / agent card
     */
    @GetMapping("/agents/{agentId}")
    public A2aAgentCard agent(@PathVariable("agentId") String agentId) {
        return a2aService.getAgent(agentId);
    }

    /**
     * 创建一个新的 A2A 任务。
     * Create a new A2A task.
     *
     * @param request 任务请求体 / task request body
     * @return 创建的任务 / created task
     */
    @PostMapping("/tasks")
    public A2aTask createTask(@Valid @RequestBody A2aTaskRequest request) {
        return a2aService.createTask(request);
    }

    /**
     * 根据 ID 查询指定任务的详情。
     * Get task details by task ID.
     *
     * @param taskId 任务 ID / task identifier
     * @return 任务详情 / task details
     */
    @GetMapping("/tasks/{taskId}")
    public A2aTask task(@PathVariable("taskId") String taskId) {
        return a2aService.getTask(taskId);
    }
}

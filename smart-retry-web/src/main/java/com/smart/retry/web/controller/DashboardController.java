package com.smart.retry.web.controller;

import com.smart.retry.web.dto.Result;
import com.smart.retry.web.dto.dashboard.DashboardVO;
import com.smart.retry.web.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仪表盘监控控制器
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    
    private final DashboardService dashboardService;
    
    /**
     * 获取仪表盘监控数据
     */
    @GetMapping("/data")
    public Result<DashboardVO> getDashboardData() {
        DashboardVO data = dashboardService.getDashboardData();
        return Result.success(data);
    }
}

package com.sky.service;

import com.sky.vo.TurnoverReportVO;

import java.time.LocalDate;

public interface ReportService {
    /**
     * 获取营业额数据
     * @param begin
     * @param end
     * @return
     */
    TurnoverReportVO getTurnover(LocalDate begin, LocalDate end);
}

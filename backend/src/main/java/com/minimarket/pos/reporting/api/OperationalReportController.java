package com.minimarket.pos.reporting.api;

import com.minimarket.pos.inventory.domain.InventoryMovementType;
import com.minimarket.pos.reporting.application.OperationalReportService;
import com.minimarket.pos.reporting.domain.CashReport;
import com.minimarket.pos.reporting.domain.InventoryReport;
import com.minimarket.pos.reporting.domain.SalesReport;
import com.minimarket.pos.sales.domain.PaymentMethod;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reports")
public class OperationalReportController {

    private final OperationalReportService service;

    public OperationalReportController(OperationalReportService service) {
        this.service = service;
    }

    @GetMapping("/sales")
    public SalesReport sales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @RequestParam(required = false) PaymentMethod method) {
        return service.sales(from, to, method);
    }

    @GetMapping("/inventory")
    public InventoryReport inventory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @RequestParam(required = false) InventoryMovementType type,
            @RequestParam(required = false) UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.inventory(from, to, type, productId, page, size);
    }

    @GetMapping("/cash")
    public CashReport cash(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.cash(from, to, page, size);
    }
}

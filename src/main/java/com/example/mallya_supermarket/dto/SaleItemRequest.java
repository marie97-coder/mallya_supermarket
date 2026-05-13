package com.example.mallya_supermarket.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleItemRequest {
    private String barcode;
    private Integer quantity;
}

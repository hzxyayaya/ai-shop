package com.mall.controller;

import com.mall.common.response.ApiResponse;
import com.mall.dto.product.ProductDto;
import com.mall.dto.product.ProductListResponse;
import com.mall.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Mono<ApiResponse<ProductListResponse>> getProductList(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder
    ) {
        return productService.getProductList(page, pageSize, category, sortBy, sortOrder)
                .map(ApiResponse::success);
    }

    @GetMapping("/search")
    public Mono<ApiResponse<ProductListResponse>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder
    ) {
        return productService.searchProducts(keyword, page, pageSize, sortBy, sortOrder)
                .map(ApiResponse::success);
    }

    @GetMapping("/{id}")
    public Mono<ApiResponse<ProductDto>> getProductDetail(@PathVariable Long id) {
        return productService.getProductDetail(id)
                .map(ApiResponse::success);
    }
}

package com.mall.service;

import com.mall.common.exception.BusinessException;
import com.mall.dto.product.ProductDto;
import com.mall.dto.product.ProductListResponse;
import com.mall.entity.product.ProductEntity;
import com.mall.repository.product.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Mono<ProductListResponse> getProductList(Integer page, Integer pageSize, String category, String sortBy, String sortOrder) {
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        boolean sortAscending = parseSort(sortBy, sortOrder);
        long offset = (long) (normalizedPage - 1) * normalizedPageSize;

        Mono<Long> totalMono = productRepository.countByCategory(category);
        Mono<List<ProductDto>> listMono = productRepository
                .findProductPage(category, normalizedPageSize, offset, sortAscending)
                .map(this::toDto)
                .collectList();

        return Mono.zip(totalMono, listMono)
                .map(tuple -> new ProductListResponse(normalizedPage, normalizedPageSize, tuple.getT1(), tuple.getT2()));
    }

    public Mono<ProductListResponse> searchProducts(String keyword, Integer page, Integer pageSize, String sortBy, String sortOrder) {
        if (!StringUtils.hasText(keyword)) {
            return Mono.error(new BusinessException(400, "keyword is required"));
        }

        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        boolean sortAscending = parseSort(sortBy, sortOrder);
        long offset = (long) (normalizedPage - 1) * normalizedPageSize;

        Mono<Long> totalMono = productRepository.countByKeyword(keyword);
        Mono<List<ProductDto>> listMono = productRepository
                .searchProductPage(keyword, normalizedPageSize, offset, sortAscending)
                .map(this::toDto)
                .collectList();

        return Mono.zip(totalMono, listMono)
                .map(tuple -> new ProductListResponse(normalizedPage, normalizedPageSize, tuple.getT1(), tuple.getT2()));
    }

    public Mono<ProductListResponse> recommendProductsByQueryEmbedding(String queryEmbedding, Integer pageSize) {
        if (!StringUtils.hasText(queryEmbedding)) {
            return Mono.error(new BusinessException(400, "queryEmbedding is required"));
        }

        int normalizedPageSize = normalizePageSize(pageSize);

        return productRepository.recommendProductPageByQueryEmbedding(queryEmbedding, normalizedPageSize)
                .map(this::toDto)
                .collectList()
                .map(list -> new ProductListResponse(1, normalizedPageSize, (long) list.size(), list));
    }

    public Mono<ProductDto> getProductDetail(Long id) {
        if (id == null || id <= 0) {
            return Mono.error(new BusinessException(400, "id must be greater than 0"));
        }
        return productRepository.findProjectedById(id)
                .switchIfEmpty(Mono.error(new BusinessException(404, "resource not found")))
                .map(this::toDto);
    }

    private ProductDto toDto(ProductEntity entity) {
        return new ProductDto(
                entity.getId(),
                entity.getCategory(),
                entity.getTitle(),
                entity.getPrice(),
                entity.getSales(),
                entity.getImageUrl(),
                entity.getShopName()
        );
    }

    private int normalizePage(Integer page) {
        if (page == null) {
            return 1;
        }
        if (page < 1) {
            throw new BusinessException(400, "page must be greater than or equal to 1");
        }
        return page;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null) {
            return 20;
        }
        if (pageSize < 1) {
            throw new BusinessException(400, "pageSize must be greater than or equal to 1");
        }
        if (pageSize > 100) {
            throw new BusinessException(400, "pageSize must be less than or equal to 100");
        }
        return pageSize;
    }

    private boolean parseSort(String sortBy, String sortOrder) {
        if (!StringUtils.hasText(sortBy)) {
            return true;
        }
        if (!"price".equalsIgnoreCase(sortBy)) {
            throw new BusinessException(400, "sortBy only supports price");
        }
        if (!StringUtils.hasText(sortOrder)) {
            return true;
        }
        if ("asc".equalsIgnoreCase(sortOrder)) {
            return true;
        }
        if ("desc".equalsIgnoreCase(sortOrder)) {
            return false;
        }
        throw new BusinessException(400, "sortOrder must be asc or desc");
    }
}

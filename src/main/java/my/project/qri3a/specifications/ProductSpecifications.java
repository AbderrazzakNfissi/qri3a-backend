package my.project.qri3a.specifications;

import my.project.qri3a.entities.Product;
import my.project.qri3a.enums.ProductCategory;
import my.project.qri3a.enums.ProductCondition;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.UUID;

public class ProductSpecifications {

    public static Specification<Product> hasCategory(ProductCategory category) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("category"), category);
    }

    public static Specification<Product> hasLocation(String location) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("location"), location);
    }

    public static Specification<Product> hasCondition(ProductCondition condition) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("condition"), condition);
    }

    public static Specification<Product> hasSellerId(UUID sellerId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.join("seller").get("id"), sellerId);
    }

    public static Specification<Product> hasMinPrice(BigDecimal minPrice) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Product> hasMaxPrice(BigDecimal maxPrice) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    public static Specification<Product> hasCity(String city) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("city"), city);
    }
}

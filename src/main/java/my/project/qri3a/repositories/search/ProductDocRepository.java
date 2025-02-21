package my.project.qri3a.repositories.search;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import my.project.qri3a.documents.ProductDoc;

public interface ProductDocRepository extends ElasticsearchRepository<ProductDoc, UUID> , ProductDocRepositoryCustom{

    Page<ProductDoc> findAllByOrderByCreatedAtAsc(Pageable pageable);
    Page<ProductDoc> findProductDocByTitleOrDescription(String title,String description, Pageable pageable);
    @Override
    List<ProductDoc> findTop10ByTitleOrDescriptionContainingIgnoreCase(String title);
    
}

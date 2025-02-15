package my.project.qri3a.repositories.search;

import my.project.qri3a.documents.ProductDoc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.UUID;

public interface ProductDocRepository extends ElasticsearchRepository<ProductDoc, UUID> {

    Page<ProductDoc> findProductDocByTitle(String title, Pageable pageable);
}

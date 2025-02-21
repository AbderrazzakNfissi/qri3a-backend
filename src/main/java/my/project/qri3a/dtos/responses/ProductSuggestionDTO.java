package my.project.qri3a.dtos.responses;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ProductSuggestionDTO {
    private UUID id;
    private String title;
}

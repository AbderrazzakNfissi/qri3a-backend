package my.project.qri3a.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private T data;
    private String message;
    private int status;
    private String jsonLd; // Champ pour les métadonnées JSON-LD (SEO)

    public ApiResponse(T data, String message, int status) {
        this.data = data;
        this.message = message;
        this.status = status;
    }

    public ApiResponse(T data, String message, int status, String jsonLd) {
        this.data = data;
        this.message = message;
        this.status = status;
        this.jsonLd = jsonLd;
    }
}
package my.project.qri3a.dtos.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public class SellerInfoDTO {

    private String profile;
    private String name;

    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    private String adresse;

    @JsonProperty("webSite")
    private String website;

    private String about;

    @JsonProperty("reviewsDetail")
    private RatingsResponseDTO reviewsDetail;

    @JsonProperty("productsImages")
    private List<ImageResponseDTO> productsImages;

}
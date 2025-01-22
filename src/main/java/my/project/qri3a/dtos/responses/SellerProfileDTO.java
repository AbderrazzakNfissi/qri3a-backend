package my.project.qri3a.dtos.responses;
import lombok.Data;


@Data
public class SellerProfileDTO {
    private String name;
    private String city;
    private String website;
    private String aboutMe;
    private String phoneNumber;
    private String createdAt;
}

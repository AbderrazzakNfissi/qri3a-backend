package my.project.qri3a.dtos.requests;


import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImageOrderDTO {
    private String id;
    private int order;
}

package my.project.qri3a.mappers;

import my.project.qri3a.dtos.requests.ImageRequestDTO;
import my.project.qri3a.dtos.responses.ImageResponseDTO;
import my.project.qri3a.entities.Image;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper pour convertir entre l'entité Image et le DTO ImageResponseDTO.
 */
@Component
public class ImageMapper {

    /**
     * Convertit une entité Image en ImageResponseDTO.
     *
     * @param image l'entité Image à convertir
     * @return le DTO correspondant
     */
    public ImageResponseDTO toDTO(Image image) {
        if (image == null) {
            return null;
        }
        ImageResponseDTO imageResponseDTO = new ImageResponseDTO();
        BeanUtils.copyProperties(image, imageResponseDTO);
        return imageResponseDTO;
    }

    /**
     * Convertit un ImageResponseDTO en une nouvelle entité Image.
     *
     * @param imageRequestDTO le DTO à convertir
     * @return la nouvelle entité Image
     */
    public Image toEntity(ImageRequestDTO imageRequestDTO) {
        if (imageRequestDTO == null) {
            return null;
        }
        Image image = new Image();
        BeanUtils.copyProperties(imageRequestDTO, image);
        return image;
    }


}

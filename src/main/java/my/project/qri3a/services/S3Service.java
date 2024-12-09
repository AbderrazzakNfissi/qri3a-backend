package my.project.qri3a.services;

import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.util.List;

public interface S3Service {
    /**
     * Télécharge un fichier sur S3.
     *
     * @param file MultipartFile à télécharger.
     * @param key  Clé (nom du fichier) dans le bucket.
     * @return L'URL du fichier téléchargé.
     * @throws IOException En cas d'erreur lors du téléchargement.
     */
    String uploadFile(MultipartFile file, String key) throws IOException;

    /**
     * Récupère un fichier depuis S3.
     *
     * @param key Clé (nom du fichier) dans le bucket.
     * @return Les données du fichier.
     */
    byte[] getFile(String key);

    /**
     * Supprime un fichier de S3.
     *
     * @param key Clé (nom du fichier) dans le bucket.
     */
    void deleteFile(String key);

    /**
     * Liste tous les objets dans le bucket.
     *
     * @return Liste des objets S3.
     */
    List<S3Object> listFiles();
}

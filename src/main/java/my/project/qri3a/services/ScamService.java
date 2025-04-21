package my.project.qri3a.services;

import my.project.qri3a.dtos.requests.ScamReportRequestDTO;
import my.project.qri3a.dtos.requests.ScamUpdateRequestDTO;
import my.project.qri3a.dtos.responses.ScamResponseDTO;
import my.project.qri3a.dtos.responses.ScamStatisticsDTO;
import my.project.qri3a.enums.ScamStatus;
import my.project.qri3a.exceptions.ResourceNotFoundException;
import my.project.qri3a.exceptions.ResourceNotValidException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.util.UUID;

public interface ScamService {

    // Créer un signalement d'arnaque (toujours anonyme)
    ScamResponseDTO reportScam(ScamReportRequestDTO dto)
            throws ResourceNotFoundException;

    // Récupérer un signalement d'arnaque par ID
    ScamResponseDTO getScamById(UUID scamId) throws ResourceNotFoundException;

    // Mettre à jour le statut d'un signalement d'arnaque (admin)
    ScamResponseDTO updateScamStatus(UUID scamId, ScamUpdateRequestDTO dto, Authentication authentication)
            throws ResourceNotFoundException, ResourceNotValidException;

    // Supprimer un signalement d'arnaque
    void deleteScam(UUID scamId) throws ResourceNotFoundException;

    // Récupérer tous les signalements d'arnaque (admin)
    Page<ScamResponseDTO> getAllScams(Pageable pageable);

    // Récupérer les signalements d'arnaque par statut (admin)
    Page<ScamResponseDTO> getScamsByStatus(ScamStatus status, Pageable pageable);

    // Récupérer les signalements d'arnaque concernant un produit par son identifiant
    Page<ScamResponseDTO> getScamsByProductIdentifier(String productIdentifier, Pageable pageable)
            throws ResourceNotFoundException;

    // Obtenir des statistiques sur les signalements d'arnaque (admin)
    ScamStatisticsDTO getScamStatistics();

    // Vérifier si un produit a des signalements confirmés
    boolean hasConfirmedScams(String productIdentifier);

    long countScamsByStatus(ScamStatus status);
}
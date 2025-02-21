package my.project.qri3a.services;


import my.project.qri3a.dtos.requests.AuthenticationRequest;
import my.project.qri3a.dtos.requests.EmailAndPasswordDTO;
import my.project.qri3a.dtos.responses.AuthenticationResponse;
import my.project.qri3a.exceptions.ResourceAlreadyExistsException;

public interface AuthenticationService {
    AuthenticationResponse registerUser(EmailAndPasswordDTO request) throws ResourceAlreadyExistsException;
    AuthenticationResponse authenticate(AuthenticationRequest request);
    AuthenticationResponse refreshToken(String refreshToken);
}


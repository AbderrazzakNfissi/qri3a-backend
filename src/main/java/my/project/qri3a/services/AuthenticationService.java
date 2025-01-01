package my.project.qri3a.services;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import my.project.qri3a.dtos.requests.AuthenticationRequest;
import my.project.qri3a.dtos.requests.EmailAndPasswordDTO;
import my.project.qri3a.dtos.requests.UserRequestDTO;
import my.project.qri3a.dtos.responses.AuthenticationResponse;
import my.project.qri3a.entities.User;
import my.project.qri3a.exceptions.ResourceAlreadyExistsException;

import java.io.IOException;

public interface AuthenticationService {
    AuthenticationResponse registerUser(EmailAndPasswordDTO request) throws ResourceAlreadyExistsException;
    AuthenticationResponse authenticate(AuthenticationRequest request);
    void refreshToken( HttpServletRequest request,  HttpServletResponse response ) throws IOException;

}


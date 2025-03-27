package my.project.qri3a.enums;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;

public enum Role {
    SELLER,
    ADMIN;

    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this == ADMIN) {
            return Arrays.asList(
                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_SELLER") // Un admin a aussi les droits d'un seller
            );
        } else {
            return Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_SELLER")
            );
        }
    }
}
package my.project.qri3a.repositories;

import my.project.qri3a.entities.TokenModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenRepository extends JpaRepository<TokenModel, UUID> {

    @Query(value = """
      select t from TokenModel t inner join User u\s
      on t.user.id = u.id\s
      where u.id = :id and (t.expired = false or t.revoked = false)\s
      """)
    List<TokenModel> findAllValidTokenByUser(UUID id);

    Optional<TokenModel> findByToken(String token);
}

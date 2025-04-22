// src/main/java/com/yourname/backend/repositories/SkillRepository.java
package com.yourname.backend.repositories;

import com.yourname.backend.entities.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {
    Optional<Skill> findByName(String name);
}

package com.yourname.backend.services;

import com.yourname.backend.entities.Skill;
import com.yourname.backend.repositories.SkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SkillService {
    private final SkillRepository skillRepo;

    public SkillService(SkillRepository skillRepo) {
        this.skillRepo = skillRepo;
    }

    /**
     * Given a set of skill names, fetch existing Skill entities or create new ones.
     */
    @Transactional
    public Set<Skill> fetchOrCreateSkills(Set<String> names) {
        return names.stream().map(name ->
                skillRepo.findByName(name)
                        .orElseGet(() -> skillRepo.save(new Skill(name)))
        ).collect(Collectors.toSet());
    }
}

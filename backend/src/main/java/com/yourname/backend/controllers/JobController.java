package com.yourname.backend.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourname.backend.dto.JobDescriptionDto;
import com.yourname.backend.dto.JobRequest;
import com.yourname.backend.entities.JobDescription;
import com.yourname.backend.entities.Skill;
import com.yourname.backend.repositories.JobDescriptionRepository;
import com.yourname.backend.services.SkillService;
import com.yourname.backend.storage.StorageService;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.yourname.backend.util.CitationCleaner.strip;

@RestController
@RequestMapping("/job")
@CrossOrigin(origins = "http://localhost:3000")
public class JobController {

    private static final Logger log = LoggerFactory.getLogger(JobController.class);

    private final JobDescriptionRepository jobRepo;
    private final StorageService           storageService;
    private final SkillService             skillService;
    private final ObjectMapper             JSON = new ObjectMapper();

    @Value("${python.text-extractor}")
    private String TEXT_EXTRACTOR;

    @Value("${python.job-parser}")
    private String JOB_PARSER;

    @Value("${ai.python-executable:python3}")
    private String PYTHON;

    @Autowired
    public JobController(JobDescriptionRepository jobRepo,
                         StorageService storageService,
                         SkillService skillService) {
        this.jobRepo        = jobRepo;
        this.storageService = storageService;
        this.skillService   = skillService;
    }

    /* ------------------------------------------------------------------
       Manual entry via JSON → treat as .txt + parse
       ------------------------------------------------------------------ */
    @PostMapping(path = "/createManual",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public JobDescriptionDto createManual(@RequestBody JobRequest req) throws Exception {
        // 1) write the raw descriptionText to a temp .txt file
        String jdPath = storageService.storeText(
                req.getDescriptionText(),  // the raw job text
                "job-",             // prefix
                ".txt"                     // suffix
        );

        // 2) invoke your parser (it can extract text & fields for .txt)
        //    you can skip TEXT_EXTRACTOR if your JOB_PARSER handles raw text directly;
        //    otherwise uncomment the next line to pull plain text:
        // String plainTxt = runPythonCaptureText(TEXT_EXTRACTOR, jdPath);
        String plainTxt   = req.getDescriptionText();
        String parsedJson = runPython(JOB_PARSER, jdPath);
        JsonNode n        = JSON.readTree(parsedJson);

        // 3) extract all fields
        String summary    = n.path("Job Description").asText(null);
        String category   = n.path("Job Category").asText(null);
        String location   = n.path("Location").asText(null);

        List<String> skillsList = toStringList(n.path("skills"));
        Set<Skill> skills = skillService.fetchOrCreateSkills(
                skillsList.stream()
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.toSet())
        );

        List<String> reqList  = JSON.convertValue(
                n.path("Requirements").isMissingNode() ? List.of() : n.path("Requirements"),
                JSON.getTypeFactory().constructCollectionType(List.class, String.class)
        );
        List<String> respList = JSON.convertValue(
                n.path("Responsibilities").isMissingNode() ? List.of() : n.path("Responsibilities"),
                JSON.getTypeFactory().constructCollectionType(List.class, String.class)
        );

        // 4) build & persist
        JobDescription jd = new JobDescription();
        jd.setTitle(req.getTitle());
        jd.setCategory(category);
        jd.setLocation(location);
        jd.setDescriptionText(plainTxt);
        jd.setSummary(strip(summary));
        jd.setSkills(skills);
        jd.setRequirements(strip(String.join(", ", reqList)));
        jd.setResponsibilities(strip(String.join(", ", respList)));
        jd.setParsedJson(parsedJson);
        jd.setFilePath(jdPath);
        // status defaults to "Active" in your entity

        JobDescription saved = jobRepo.save(jd);

        // 5) return DTO
        return toDto(saved, skillsList, reqList, respList);
    }

    /* ------------------------------------------------------------------
       File‐upload path (unchanged)
       ------------------------------------------------------------------ */
    @PostMapping(path = "/uploadFile",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public JobDescriptionDto uploadFile(@RequestParam("file")  @NotNull MultipartFile file,
                                        @RequestParam("title") @NotNull String        title)
            throws Exception {
        if (file.isEmpty())
            throw new IllegalArgumentException("File is empty");

        String jdPath     = storageService.store(file);
        String plainTxt   = runPythonCaptureText(TEXT_EXTRACTOR, jdPath);
        String parsedJson = runPython(JOB_PARSER,        jdPath);
        JsonNode n        = JSON.readTree(parsedJson);

        String summary = n.path("Job Description").asText(null);
        List<String> skillsList = toStringList(n.path("skills"));
        Set<Skill> skills = skillService.fetchOrCreateSkills(
                skillsList.stream().map(String::trim).filter(s -> !s.isBlank()).collect(Collectors.toSet())
        );
        List<String> reqList  = JSON.convertValue(
                n.path("Requirements").isMissingNode() ? List.of() : n.path("Requirements"),
                JSON.getTypeFactory().constructCollectionType(List.class, String.class)
        );
        List<String> respList = JSON.convertValue(
                n.path("Responsibilities").isMissingNode() ? List.of() : n.path("Responsibilities"),
                JSON.getTypeFactory().constructCollectionType(List.class, String.class)
        );

        JobDescription jd = new JobDescription();
        jd.setTitle(title);
        jd.setCategory(n.path("Job Category").asText(null));
        jd.setLocation(n.path("Location").asText(null));
        jd.setDescriptionText(plainTxt);
        jd.setSummary(strip(summary));
        jd.setSkills(skills);
        jd.setRequirements(strip(String.join(", ", reqList)));
        jd.setResponsibilities(strip(String.join(", ", respList)));
        jd.setParsedJson(parsedJson);
        jd.setFilePath(jdPath);

        JobDescription saved = jobRepo.save(jd);
        return toDto(saved, skillsList, reqList, respList);
    }

    /* ------------------------------------------------------------------
       GET all jobs
       ------------------------------------------------------------------ */
    @GetMapping("/all")
    public List<JobDescriptionDto> list() {
        return jobRepo.findAll().stream()
                .map(jd -> toDto(
                        jd,
                        jd.getSkills().stream().map(Skill::getName).toList(),
                        splitCsv(jd.getRequirements()),
                        splitCsv(jd.getResponsibilities())
                ))
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobDescriptionDto> getById(@PathVariable Long id) {
        return jobRepo.findById(id)
                .map(jd -> toDto(
                        jd,
                        jd.getSkills().stream().map(Skill::getName).toList(),
                        splitCsv(jd.getRequirements()),
                        splitCsv(jd.getResponsibilities())
                ))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        if (!jobRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        jobRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /* ==================================================================
       Helpers
       ================================================================== */
    private List<String> toStringList(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return List.of();
        if (node.isArray()) {
            return StreamSupport.stream(node.spliterator(), false)
                    .map(JsonNode::asText)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();
        }
        if (node.isTextual()) {
            return Arrays.stream(node.asText().split("\\s*,\\s*"))
                    .filter(s -> !s.isBlank())
                    .toList();
        }
        return List.of();
    }

    private List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split("\\s*,\\s*"))
                .filter(s -> !s.isBlank())
                .toList();
    }

    private JobDescriptionDto toDto(JobDescription jd,
                                    List<String> skills,
                                    List<String> req,
                                    List<String> resp) {
        return new JobDescriptionDto(
                jd.getId(),
                jd.getTitle(),
                jd.getCategory(),
                jd.getLocation(),
                jd.getSummary(),
                skills,
                req,
                resp,
                jd.getStatus()
        );
    }

    private String runPython(String script, String... args)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(PYTHON, script);
        pb.command().addAll(List.of(args));
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (p.waitFor() != 0) {
            log.error("{} failed →\n{}", script, out);
            throw new RuntimeException(script + " failed – see logs");
        }
        return out.trim();
    }

    private String runPythonCaptureText(String script, String... args)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(PYTHON, script);
        pb.command().addAll(List.of(args));
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String last = null;
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) last = line;
        }
        if (p.waitFor() != 0) {
            log.error("{} failed (last line: {})", script, last);
            throw new RuntimeException(script + " failed – see logs");
        }
        return last != null ? last : "";
    }
}
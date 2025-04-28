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

import java.io.*;
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
    private static final ObjectMapper JSON = new ObjectMapper();

    private final JobDescriptionRepository jobRepo;
    private final StorageService storageService;
    private final SkillService skillService;

    @Value("${python.text-extractor}")
    private String TEXT_EXTRACTOR;

    @Value("${python.job-parser}")
    private String JOB_PARSER;

    @Value("${ai.python-executable:python3}")
    private String PYTHON;

    @Autowired
    public JobController(
            JobDescriptionRepository jobRepo,
            StorageService storageService,
            SkillService skillService
    ) {
        this.jobRepo = jobRepo;
        this.storageService = storageService;
        this.skillService = skillService;
    }

    /* ------------------------------------------------------------------
       Manual entry via JSON → treat as .txt + parse
    ------------------------------------------------------------------ */
    @PostMapping(path = "/createManual",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public JobDescriptionDto createManual(@RequestBody JobRequest req) throws Exception {
        Path tmp = Files.createTempFile("job-", ".txt");
        Files.writeString(tmp, req.getDescriptionText(), StandardCharsets.UTF_8);
        String jdPath = tmp.toAbsolutePath().toString();

        // no need for TEXT_EXTRACTOR here if JOB_PARSER handles raw text
        String parsedJson = runPython(JOB_PARSER, jdPath);
        JsonNode n = JSON.readTree(parsedJson);

        String summary = n.path("Job Description").asText(null);
        String category = n.path("Job Category").asText(null);
        String location = n.path("Location").asText(null);

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
        jd.setTitle(req.getTitle());
        jd.setCategory(category);
        jd.setLocation(location);
        jd.setDescriptionText(req.getDescriptionText());
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
       File-upload path
    ------------------------------------------------------------------ */
    @PostMapping(path = "/uploadFile",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public JobDescriptionDto uploadFile(
            @RequestParam("file") @NotNull MultipartFile file,
            @RequestParam("title") @NotNull String title
    ) throws Exception {
        if (file.isEmpty())
            throw new IllegalArgumentException("File is empty");

        String jdPath = storageService.store(file);
        String plainTxt = runPythonCaptureText(TEXT_EXTRACTOR, jdPath);
        String parsedJson = runPython(JOB_PARSER, jdPath);
        JsonNode n = JSON.readTree(parsedJson);

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

    //─────────────────────────────────────────────────────────────────────
    // Helpers
    //─────────────────────────────────────────────────────────────────────

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

    private String runPython(String script, String... args) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add(PYTHON);
        cmd.add(new File(script).getAbsolutePath());
        cmd.addAll(Arrays.asList(args));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File("/app"));

        Process proc = pb.start();

        ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuf = new ByteArrayOutputStream();
        Thread tOut = new Thread(() -> {
            try (InputStream is = proc.getInputStream()) { is.transferTo(outBuf); }
            catch (IOException ignored) {}
        });
        Thread tErr = new Thread(() -> {
            try (InputStream is = proc.getErrorStream()) { is.transferTo(errBuf); }
            catch (IOException ignored) {}
        });
        tOut.start(); tErr.start();

        int exit = proc.waitFor();
        tOut.join(); tErr.join();

        String stdout = outBuf.toString(StandardCharsets.UTF_8);
        String stderr = errBuf.toString(StandardCharsets.UTF_8);

        log.debug("Command {} exited {}. stdout:\\n{}\\nstderr:\\n{}",
                cmd, exit, stdout, stderr);

        if (exit != 0) {
            throw new RuntimeException(
                    String.format("Script %s failed (exit %d)\nstderr:%s", script, exit, stderr)
            );
        }
        return stdout.trim();
    }

    private String runPythonCaptureText(String script, String... args) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add(PYTHON);
        cmd.add(new File(script).getAbsolutePath());
        cmd.addAll(Arrays.asList(args));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File("/app"));

        Process proc = pb.start();

        ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuf = new ByteArrayOutputStream();
        Thread tOut = new Thread(() -> {
            try (InputStream is = proc.getInputStream()) { is.transferTo(outBuf); }
            catch (IOException ignored) {}
        });
        Thread tErr = new Thread(() -> {
            try (InputStream is = proc.getErrorStream()) { is.transferTo(errBuf); }
            catch (IOException ignored) {}
        });
        tOut.start(); tErr.start();

        int exit = proc.waitFor();
        tOut.join(); tErr.join();

        String stdout = outBuf.toString(StandardCharsets.UTF_8);
        String stderr = errBuf.toString(StandardCharsets.UTF_8);

        log.debug("Command {} exited {}. stdout:\\n{}\\nstderr:\\n{}",
                cmd, exit, stdout, stderr);

        if (exit != 0) {
            throw new RuntimeException(
                    String.format("Script %s failed (exit %d)\nstderr:%s", script, exit, stderr)
            );
        }

        // if the extractor returns {"text": "..."} JSON, pull out "text"
        try {
            JsonNode n = JSON.readTree(stdout);
            return n.path("text").asText(stdout.trim());
        } catch (Exception e) {
            log.warn("Failed to parse JSON text from {}, returning raw stdout", script, e);
            return stdout.trim();
        }
    }
}

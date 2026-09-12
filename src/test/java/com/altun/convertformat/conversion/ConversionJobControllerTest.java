package com.altun.convertformat.conversion;

import com.altun.convertformat.common.error.ApiExceptionHandler;
import com.altun.convertformat.common.config.SecurityConfiguration;
import com.altun.convertformat.entities.ConversionJob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConversionJobController.class)
@Import({ApiExceptionHandler.class, SecurityConfiguration.class})
class ConversionJobControllerTest {

    @TempDir
    private Path tempDirectory;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConversionJobService conversionJobService;

    @Test
    void acceptsDocxAndReturnsPendingJob() throws Exception {
        UUID id = UUID.fromString("f89a019d-fce5-4b1f-8d63-d58fb3c79130");
        MockMultipartFile file = docx();
        ConversionJob conversionJob = new ConversionJob("example.docx", "source.docx");
        ReflectionTestUtils.setField(conversionJob, "id", id);
        when(conversionJobService.create(file)).thenReturn(conversionJob);

        mockMvc.perform(multipart("/api/v1/conversions").file(file))
                .andExpect(status().isAccepted())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/conversions/f89a019d-fce5-4b1f-8d63-d58fb3c79130"
                ))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.originalFileName").value("example.docx"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void returnsBadRequestForInvalidFile() throws Exception {
        MockMultipartFile file = docx();
        when(conversionJobService.create(file))
                .thenThrow(new IllegalArgumentException("Yalnızca DOCX dosyaları kabul edilir"));

        mockMvc.perform(multipart("/api/v1/conversions").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Yalnızca DOCX dosyaları kabul edilir"))
                .andExpect(jsonPath("$.path").value("/api/v1/conversions"));
    }

    @Test
    void returnsBadRequestWhenFilePartIsMissing() throws Exception {
        mockMvc.perform(multipart("/api/v1/conversions"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dosya alanı zorunludur"));
    }

    @Test
    void returnsJobStatusById() throws Exception {
        UUID id = UUID.fromString("f89a019d-fce5-4b1f-8d63-d58fb3c79130");
        ConversionJob conversionJob = new ConversionJob("example.docx", "source.docx");
        ReflectionTestUtils.setField(conversionJob, "id", id);
        when(conversionJobService.findById(id)).thenReturn(conversionJob);

        mockMvc.perform(get("/api/v1/conversions/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.originalFileName").value("example.docx"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void returnsNotFoundWhenJobDoesNotExist() throws Exception {
        UUID id = UUID.fromString("f89a019d-fce5-4b1f-8d63-d58fb3c79130");
        when(conversionJobService.findById(id))
                .thenThrow(new ConversionJobNotFoundException(id));

        mockMvc.perform(get("/api/v1/conversions/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Dönüşüm işlemi bulunamadı: " + id))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/conversions/" + id));
    }

    @Test
    void downloadsCompletedPdfForAuthenticatedUser() throws Exception {
        UUID id = UUID.fromString("f89a019d-fce5-4b1f-8d63-d58fb3c79130");
        Path pdf = Files.writeString(tempDirectory.resolve("result.pdf"), "pdf-content");
        when(conversionJobService.loadResult(id))
                .thenReturn(new ConversionFile(pdf, "example.pdf"));

        mockMvc.perform(get("/api/v1/conversions/{id}/file", id).with(user("test")))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition",
                        containsString("filename=\"example.pdf\"")
                ))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().string("pdf-content"));
    }

    @Test
    void protectsPdfDownloadFromAnonymousUsers() throws Exception {
        UUID id = UUID.fromString("f89a019d-fce5-4b1f-8d63-d58fb3c79130");

        mockMvc.perform(get("/api/v1/conversions/{id}/file", id))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsConflictWhileConversionIsPending() throws Exception {
        UUID id = UUID.fromString("f89a019d-fce5-4b1f-8d63-d58fb3c79130");
        when(conversionJobService.loadResult(id))
                .thenThrow(new ConversionNotReadyException(id));

        mockMvc.perform(get("/api/v1/conversions/{id}/file", id).with(user("test")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Dönüşüm işlemi henüz tamamlanmadı: " + id));
    }

    private MockMultipartFile docx() {
        return new MockMultipartFile(
                "file",
                "example.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "content".getBytes()
        );
    }
}

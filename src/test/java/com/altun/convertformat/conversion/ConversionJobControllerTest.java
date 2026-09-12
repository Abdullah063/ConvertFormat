package com.altun.convertformat.conversion;

import com.altun.convertformat.common.error.ApiExceptionHandler;
import com.altun.convertformat.common.config.SecurityConfiguration;
import com.altun.convertformat.entities.ConversionJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConversionJobController.class)
@Import({ApiExceptionHandler.class, SecurityConfiguration.class})
class ConversionJobControllerTest {

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

    private MockMultipartFile docx() {
        return new MockMultipartFile(
                "file",
                "example.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "content".getBytes()
        );
    }
}

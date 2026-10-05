package com.minimarket.pos.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.NoHandlerFoundException;
import com.minimarket.pos.shared.application.error.ApplicationException;
import com.minimarket.pos.shared.application.error.ProblemType;

class GlobalExceptionHandlerTest {

    private static final String CORRELATION_ID = UUID.randomUUID().toString();

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE, CORRELATION_ID);
    }

    @ParameterizedTest
    @MethodSource("applicationProblems")
    void mapsApplicationProblems(ProblemType problemType, HttpStatus expectedStatus) {
        ResponseEntity<ProblemDetail> response = handler.handleApplicationException(
                new ApplicationException(problemType, "Mensaje seguro"), request);

        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Mensaje seguro");
        assertThat(response.getBody().getProperties())
                .containsEntry("code", problemType.code())
                .containsEntry("correlationId", CORRELATION_ID);
    }

    @Test
    void mapsBeanValidationFieldErrors() throws Exception {
        TestRequest target = new TestRequest("");
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(target, "request");
        binding.rejectValue("name", "NotBlank", "El nombre es obligatorio");
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("validate", TestRequest.class);
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(new MethodParameter(method, 0), binding);

        ResponseEntity<ProblemDetail> response =
                handler.handleMethodArgumentNotValid(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(fieldErrors(response)).containsExactly(new FieldViolation("name", "El nombre es obligatorio"));
    }

    @Test
    void usesSafeFallbackWhenFieldMessageIsMissing() throws Exception {
        TestRequest target = new TestRequest("");
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(target, "request");
        binding.addError(new org.springframework.validation.FieldError("request", "name", null));
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("validate", TestRequest.class);
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(new MethodParameter(method, 0), binding);

        ResponseEntity<ProblemDetail> response =
                handler.handleMethodArgumentNotValid(exception, request);

        assertThat(fieldErrors(response)).containsExactly(new FieldViolation("name", "Valor inválido"));
    }

    @Test
    void mapsConstraintViolations() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("quantity");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("debe ser mayor que cero");
        ConstraintViolationException exception =
                new ConstraintViolationException(Set.of(violation));

        ResponseEntity<ProblemDetail> response =
                handler.handleConstraintViolation(exception, request);

        assertThat(fieldErrors(response))
                .containsExactly(new FieldViolation("quantity", "debe ser mayor que cero"));
    }

    @Test
    void mapsMalformedJsonWithoutLeakingParserDetails() {
        HttpInputMessage input = mock(HttpInputMessage.class);
        HttpMessageNotReadableException exception =
                new HttpMessageNotReadableException("sensitive parser detail", input);

        ResponseEntity<ProblemDetail> response = handler.handleUnreadableBody(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("El cuerpo JSON no tiene un formato válido.");
        assertThat(response.getBody().getDetail()).doesNotContain("sensitive");
    }

    @Test
    void mapsMissingRoute() {
        NoHandlerFoundException exception =
                new NoHandlerFoundException("GET", "/missing", HttpHeaders.EMPTY);

        ResponseEntity<ProblemDetail> response = handler.handleNotFound(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProperties()).containsEntry("code", "not-found");
    }

    @Test
    void hidesUnexpectedExceptionAndGeneratesCorrelationWhenFilterWasNotApplied() {
        MockHttpServletRequest unfilteredRequest = new MockHttpServletRequest();

        ResponseEntity<ProblemDetail> response =
                handler.handleUnexpected(new IllegalStateException("database password"), unfilteredRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Ocurrió un error inesperado.");
        assertThat(response.getBody().getDetail()).doesNotContain("password");
        assertThat(response.getBody().getProperties().get("correlationId").toString())
                .isNotBlank();
    }

    private static Stream<Arguments> applicationProblems() {
        return Stream.of(
                Arguments.of(ProblemType.VALIDATION, HttpStatus.BAD_REQUEST),
                Arguments.of(ProblemType.NOT_FOUND, HttpStatus.NOT_FOUND),
                Arguments.of(ProblemType.CONFLICT, HttpStatus.CONFLICT),
                Arguments.of(ProblemType.FORBIDDEN, HttpStatus.FORBIDDEN));
    }

    @SuppressWarnings("unchecked")
    private List<FieldViolation> fieldErrors(ResponseEntity<ProblemDetail> response) {
        assertThat(response.getBody()).isNotNull();
        return (List<FieldViolation>) response.getBody().getProperties().get("fieldErrors");
    }

    @SuppressWarnings("unused")
    private void validate(TestRequest request) {}

    private record TestRequest(String name) {}
}

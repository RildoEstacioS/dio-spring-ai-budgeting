package dio.budgeting.infrastructure.http;

import dio.budgeting.application.DeleteTransactionUseCase;
import dio.budgeting.application.GetTransactionUseCase;
import dio.budgeting.application.ListTransactionsByCategoryUseCase;
import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.application.SearchTransactionsUseCase;
import dio.budgeting.application.SummarizeExpensesUseCase;
import dio.budgeting.application.UpdateTransactionUseCase;
import dio.budgeting.application.input.TransactionSearchQuery;
import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.EntryStatus;
import dio.budgeting.domain.EntryType;
import dio.budgeting.infrastructure.http.request.TransactionRequest;
import dio.budgeting.infrastructure.http.response.TransactionPageResponse;
import dio.budgeting.infrastructure.http.response.TransactionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
public class TransactionController {
    static final String LEGACY_DEFAULTS_HEADER = "X-Budgeting-Legacy-Defaults";
    private static final String LEGACY_DEFAULTS = String.join(",", PersistTransactionUseCase.LEGACY_DEFAULT_FIELDS);
    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private final PersistTransactionUseCase persistTransactionUseCase;
    private final ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase;
    private final SearchTransactionsUseCase searchTransactionsUseCase;
    private final GetTransactionUseCase getTransactionUseCase;
    private final UpdateTransactionUseCase updateTransactionUseCase;
    private final DeleteTransactionUseCase deleteTransactionUseCase;

    private final TranscriptionModel transcriptionModel;
    private final ChatClient chatClient;
    private final TextToSpeechModel textToSpeechModel;

    public TransactionController(PersistTransactionUseCase persistTransactionUseCase,
                                 ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase,
                                 SummarizeExpensesUseCase summarizeExpensesUseCase,
                                 SearchTransactionsUseCase searchTransactionsUseCase,
                                 GetTransactionUseCase getTransactionUseCase,
                                 UpdateTransactionUseCase updateTransactionUseCase,
                                 DeleteTransactionUseCase deleteTransactionUseCase,
                                 TranscriptionModel transcriptionModel,
                                 @Value("classpath:prompts/system-message.st") Resource systemPrompt,
                                 ChatClient.Builder chatClientBuilder,
                                 TextToSpeechModel textToSpeechModel) throws IOException {
        this.persistTransactionUseCase = persistTransactionUseCase;
        this.listTransactionsByCategoryUseCase = listTransactionsByCategoryUseCase;
        this.searchTransactionsUseCase = searchTransactionsUseCase;
        this.getTransactionUseCase = getTransactionUseCase;
        this.updateTransactionUseCase = updateTransactionUseCase;
        this.deleteTransactionUseCase = deleteTransactionUseCase;
        this.transcriptionModel = transcriptionModel;
        this.chatClient = chatClientBuilder
                .defaultSystem(systemPrompt.getContentAsString(Charset.defaultCharset()))
                .defaultTools(persistTransactionUseCase, listTransactionsByCategoryUseCase, summarizeExpensesUseCase)
                .build();
        this.textToSpeechModel = textToSpeechModel;
    }

    /**
     * Sem nenhum campo novo, aplica o contrato legado da v1 (compatibilidade temporária) e informa os
     * valores padrão no cabeçalho X-Budgeting-Legacy-Defaults. Com qualquer campo novo, exige o contrato completo.
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@RequestBody TransactionRequest request) {
        if (request.isLegacy()) {
            log.warn("POST /transactions em modo de compatibilidade; defaults aplicados: {}", LEGACY_DEFAULTS);
            var output = persistTransactionUseCase.execute(TransactionRequestMapper.toLegacyInput(request));
            return created(output).header(LEGACY_DEFAULTS_HEADER, LEGACY_DEFAULTS).body(TransactionResponse.from(output));
        }
        var output = persistTransactionUseCase.create(TransactionRequestMapper.toCommand(request, false));
        return created(output).body(TransactionResponse.from(output));
    }

    @GetMapping
    public TransactionPageResponse listTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) EntryType type,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) EntryStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return TransactionPageResponse.from(searchTransactionsUseCase.execute(
                new TransactionSearchQuery(start, end, type, category, status, page, size)));
    }

    @GetMapping("/{id:" + RoutePatterns.UUID + "}")
    public TransactionResponse getTransaction(@PathVariable UUID id) {
        return TransactionResponse.from(getTransactionUseCase.execute(id));
    }

    @PutMapping("/{id:" + RoutePatterns.UUID + "}")
    public TransactionResponse updateTransaction(@PathVariable UUID id, @RequestBody TransactionRequest request) {
        return TransactionResponse.from(
                updateTransactionUseCase.execute(id, TransactionRequestMapper.toCommand(request, true)));
    }

    @DeleteMapping("/{id:" + RoutePatterns.UUID + "}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable UUID id) {
        deleteTransactionUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{category:" + RoutePatterns.CATEGORY + "}")
    public List<TransactionResponse> readTransactions(@PathVariable Category category) {
        return listTransactionsByCategoryUseCase.execute(category).stream().map(TransactionResponse::from).toList();
    }

    @PostMapping(value = "/ai", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "audio/mp3")
    ResponseEntity<Resource> transcribe(@RequestParam("file") MultipartFile file) {
        var userMessage = transcriptionModel.transcribe(file.getResource());
        var result = chatClient.prompt().user(userMessage).call().content();

        byte[] audio = textToSpeechModel.call(result);
        var resource = new ByteArrayResource(audio);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("audio.mp3")
                                .build()
                                .toString())
                .body(resource);
    }

    private static ResponseEntity.BodyBuilder created(TransactionOutput output) {
        return ResponseEntity.created(URI.create("/transactions/" + output.id()));
    }
}

package com.azure.ai.openai.implementation;

import com.azure.ai.openai.models.ChatChoice;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatRequestAssistantMessage;
import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.ai.openai.models.ChatRequestSystemMessage;
import com.azure.ai.openai.models.ChatRequestUserMessage;
import com.azure.ai.openai.models.ChatResponseMessage;
import com.azure.ai.openai.models.ChatRole;
import com.azure.ai.openai.models.CompletionsFinishReason;
import com.azure.ai.openai.models.EmbeddingsOptions;
import com.azure.core.util.BinaryData;
import com.azure.core.util.Configuration;
import com.azure.core.util.ConfigurationProperty;
import com.azure.core.util.ConfigurationPropertyBuilder;
import com.azure.core.util.Context;
import com.azure.core.util.CoreUtils;
import com.azure.core.util.logging.ClientLogger;
import com.azure.core.util.serializer.JacksonAdapter;
import com.azure.core.util.serializer.JsonSerializer;
import com.azure.core.util.serializer.JsonSerializerProviders;
import com.azure.core.util.serializer.SerializerAdapter;
import com.azure.core.util.serializer.SerializerEncoding;
import com.azure.core.util.tracing.SpanKind;
import com.azure.core.util.tracing.StartSpanOptions;
import com.azure.core.util.tracing.Tracer;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class OpenAITracer {
    private static final ClientLogger LOGGER = new ClientLogger(OpenAITracer.class);
    private static final SerializerAdapter SERIALIZER_ADAPTER = JacksonAdapter.createDefaultSerializerAdapter();

    private static final ConfigurationProperty<Boolean> ENABLE_EVENT_COLLECTION_PROPERTY = ConfigurationPropertyBuilder
        .ofBoolean("openai.distributed_tracing.event_collection_enabled")
        .defaultValue(false)
        .environmentVariableName("AZURE_OPENAI_DISTRIBUTED_TRACING_EVENT_COLLECTION_ENABLED")
        .build();

    private final Tracer tracer;
    private final String serverAddress;
    private final int serverPort;
    private final boolean isEventCollectionEnabled;

    public OpenAITracer(Tracer tracer, String endpoint, Configuration configuration) {
        this.tracer = tracer;

        // we should have already validated the format
        URI uri = URI.create(endpoint);
        this.serverAddress = uri.getHost();
        this.serverPort = uri.getPort() > 0 ? uri.getPort() : 443;
        if (configuration == null) {
            configuration = Configuration.getGlobalConfiguration();
        }

        this.isEventCollectionEnabled = configuration.get(ENABLE_EVENT_COLLECTION_PROPERTY);
    }

    public Context startChatCompletionsSpan(String modelName, BinaryData chatCompletionOptions, Context parent) {
        ChatCompletionsOptions completionsOptions = chatCompletionOptions.toObject(ChatCompletionsOptions.class);
        return startChatCompletionsSpan(modelName, completionsOptions, parent);
    }

    public Context startChatCompletionsSpan(String modelName, ChatCompletionsOptions completionsOptions, Context parent) {
        if (!tracer.isEnabled()) {
            return parent;
        }

        if (parent == null) {
            parent = Context.NONE;
        }

        // TODO spec: define sampling-relevant attributes
        StartSpanOptions options = new StartSpanOptions(SpanKind.INTERNAL)
            .setAttribute("server.address", serverAddress)
            .setAttribute("server.port", serverPort)
            .setAttribute("gen_ai.request.model", modelName)
            .setAttribute("gen_ai.system", "openai");

        // TODO spec: standard operation names - openai has object
        Context span = tracer.start("chat.completions " + modelName, options, parent);
        if (tracer.isRecording(span)) {
            if (completionsOptions.getMaxTokens() != null) {
                tracer.setAttribute("gen_ai.request.max_tokens", completionsOptions.getMaxTokens(), span);
            }

            if (completionsOptions.getTemperature() != null) {
                tracer.setAttribute("gen_ai.request.temperature", completionsOptions.getTemperature(), span);
            }

            if (completionsOptions.getTopP() != null) {
                tracer.setAttribute("gen_ai.request.top_p", completionsOptions.getTopP(), span);
            }

            recordPrompt(completionsOptions, span);
        }
        return span;
    }


    public Context startEmbeddingsSpan(String modelName, BinaryData embeddingsOptions, Context parent) {
        if (!tracer.isEnabled()) {
            return parent;
        }

        if (parent == null) {
            parent = Context.NONE;
        }

        // TODO spec: define sampling-relevant attributes
        StartSpanOptions options = new StartSpanOptions(SpanKind.INTERNAL)
            .setAttribute("server.address", serverAddress)
            .setAttribute("server.port", serverPort)
            .setAttribute("gen_ai.request.model", modelName)
            .setAttribute("gen_ai.system", "openai");

        // TODO spec: standard operation names - openai has object
        Context span = tracer.start("embedding " + modelName, options, parent);
        if (tracer.isRecording(span)) {
            // TODO: optimize serialization
            EmbeddingsOptions embeddings = embeddingsOptions.toObject(EmbeddingsOptions.class);
            recordEmbeddingInput(embeddings, span);
        }
        return span;
    }

    public void setChatCompletionResponse(BinaryData response, Context span) {
        if (!tracer.isEnabled() || !tracer.isRecording(span)) {
            return;
        }

        ChatCompletions chatCompletions = response.toObject(ChatCompletions.class);
        recordChatCompletion(chatCompletions.getId(),
            chatCompletions.getUsage() == null ? null : chatCompletions.getUsage().getCompletionTokens(),
            chatCompletions.getUsage() == null ? null : chatCompletions.getUsage().getPromptTokens(),
            span);
        recordChoices(chatCompletions, span);
    }

    private class ChoiceBuffer {
        private final StringBuilder message = new StringBuilder();
        private ChatRole role;
        private final Context span;
        private Integer tokenCount = 0;

        public ChoiceBuffer(Context span) {
            this.span = span;
        }

        private void addChunk(ChatChoice choice) {
            if (choice.getFinishReason() != null) {
                recordChoice(choice.getFinishReason(), role, message.toString(), span);
                return;
            }
            ChatResponseMessage delta = choice.getDelta();
            if (delta == null) {
                return;
            }

            if (delta.getRole() != null) {
                // TODO log if not the  first chunk
                role = delta.getRole();
            }
            if (delta.getContent() != null) {
                tokenCount ++;
                message.append(delta.getContent());
            }
        }
    }

    public Flux<ChatCompletions> setChatCompletionsStream(Flux<ChatCompletions> completions, Context span) {
        if (!tracer.isEnabled() || !tracer.isRecording(span)) {
            return completions;
        }

        List<ChoiceBuffer> messages = new ArrayList<>();
        AtomicReference<String> responseId = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicReference<String> errorType  = new AtomicReference<>();
        return completions
            .filter(c -> !CoreUtils.isNullOrEmpty(c.getChoices()))
            .doOnNext(c -> {
                responseId.set(c.getId());

                for (ChatChoice choice : c.getChoices()) {
                    int index = choice.getIndex();
                    while (messages.size() <= index) {
                        messages.add(new ChoiceBuffer(span));
                    }
                    messages.get(index).addChunk(choice);
                }
            })
            .doOnError(e -> error.set(e))
            .doOnCancel(() -> errorType.set("cancel"))
            .doFinally(st -> {
                recordChatCompletion(responseId.get(), messages.stream()
                    .map(m -> m.tokenCount)
                    .collect(Collectors.summingInt(Integer::intValue)), null, span);
                tracer.end(errorType.get(), error.get(), span);
            });
    }

    public void endSpan(AutoCloseable scope, Throwable throwable, Context span) {
        if (scope != null) {
            try {
                scope.close();
            } catch (Exception e) {
                LOGGER.verbose("Failed to close scope", e);
            }
        }
        tracer.end(null, throwable, span);
    }

    public AutoCloseable makeSpanCurrent(Context span) {
        return tracer.makeSpanCurrent(span);
    }

    private void recordPrompt(ChatCompletionsOptions options, Context span) {
        if (!isEventCollectionEnabled) {
            return;
        }

        // TODO optimize serialization
        // TODO otel: we should eventually be able to pass it as a structured event payload
        for (ChatRequestMessage message : options.getMessages()) {
            Map<String, Object> attributes = new HashMap<>();
            /*if (message instanceof ChatRequestSystemMessage) {
                ChatRequestSystemMessage systemMessage = (ChatRequestSystemMessage) message;
                attributes.put("gen_ai.chat.role", "system");
                attributes.put("gen_ai.chat.participant_name", systemMessage.getName());
                attributes.put("event.body", systemMessage.getContent());
            } else if (message instanceof ChatRequestUserMessage) {
                ChatRequestUserMessage userMessage = (ChatRequestUserMessage) message;
                attributes.put("gen_ai.chat.role", "user");
                attributes.put("gen_ai.chat.participant_name", userMessage.getName());
            } else if (message instanceof ChatRequestUserMessage) {
                ChatRequestAssistantMessage assistantMessage = (ChatRequestAssistantMessage) message;
                attributes.put("gen_ai.chat.role", "assistant");
                attributes.put("gen_ai.chat.participant_name", assistantMessage.getName());
            }*/
            try {
                attributes.put("event.body", SERIALIZER_ADAPTER.serialize(message, SerializerEncoding.JSON));
            } catch (IOException e) {
                LOGGER.verbose("Failed to serialize message", e);
            }
            tracer.addEvent("gen_ai.prompt", attributes, OffsetDateTime.now(), span);
        }
    }

    private void recordEmbeddingInput(EmbeddingsOptions options, Context span) {
        if (!isEventCollectionEnabled) {
            return;
        }

        // TODO optimize serialization
        // TODO otel: we should eventually be able to pass it as a structured event payload
        for (String input : options.getInput()) {
            Map<String, Object> attributes = Collections.singletonMap("event.body", input);
            tracer.addEvent("gen_ai.embedding", attributes, OffsetDateTime.now(), span);
        }
    }

    private void recordChoices(ChatCompletions completions, Context span) {
        if (isEventCollectionEnabled) {
            for (ChatChoice choice : completions.getChoices()) {
                try {
                    recordChoice(choice.getFinishReason(),
                        choice.getMessage() == null ? null : choice.getMessage().getRole(),
                        //choice.getMessage() == null ? null : choice.getMessage().getContent(),
                        SERIALIZER_ADAPTER.serialize(choice, SerializerEncoding.JSON),
                        span);
                } catch (IOException e) {
                    LOGGER.verbose("Failed to serialize choice", e);
                }
            }
        }
    }

    private void recordChatCompletion(String responseId, Integer completionTokens, Integer promptTokens, Context span) {
        tracer.setAttribute("gen_ai.response.id", responseId, span);
        // TODO Azure OpenAI or spec: response model is not exposed in ChatCompletions, but it available in binary data
        // tracer.setAttribute("gen_ai.response.model", ?, span);

        if (completionTokens != null) {
            tracer.setAttribute("gen_ai.usage.completion_tokens", completionTokens, span);
        }
        if (promptTokens != null) {
            tracer.setAttribute("gen_ai.usage.prompt_tokens", promptTokens, span);
        }
    }

    private void recordChoice(CompletionsFinishReason finishReason, ChatRole role, String message, Context span) {
        if (!isEventCollectionEnabled) {
            return;
        }

        Map<String, Object> attributes = new HashMap<>();
        if (finishReason != null) {
            attributes.put("gen_ai.choice.finish_reason", finishReason.toString());
        }
        if (role != null) {
            attributes.put("gen_ai.choice.role", role.toString());
        }
        if (message != null) {
            attributes.put("event.body", message);
        }

        tracer.addEvent("gen_ai.choice", attributes, OffsetDateTime.now(), span);
    }
}

package com.github.hambuger.memory.chat.memory.tools.docparse;

import com.google.common.collect.Lists;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.hambuger.memory.chat.memory.chat.SpringAiChat;
import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.other.embeddings.SpringAiEmbeddings;
import com.github.hambuger.memory.chat.memory.other.functionCall.aop.FunctionCallRegistry;
import com.github.hambuger.memory.chat.memory.other.token.TokenCalculation;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.SummaryMetadataEnricher;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import static com.github.hambuger.memory.chat.memory.other.constants.CommonConstants.HTTP;
import static org.springframework.util.ResourceUtils.FILE_URL_PREFIX;


/**
 * @author hanjiabao
 * @since 2024/8/12
 */
@Slf4j
@Component
public class DocParse {

    @Resource
    private SpringAiEmbeddings springAiEmbeddings;

    @Resource
    private SpringAiChat springAiChat;

    @Resource
    private TokenCalculation tokenCalculation;

    private static final String PROMPT = """
            用户发起了一个问题：%s
            以下是从相关的文档中查询的到可能相关的内容：
            %s
            根据问题和查询内容回答问题。
            """;


    @Data
    public static class FileParam {

        @JsonPropertyDescription("文件本地路径或者外部url，如果是本地应该以file:开头，如果是外部url应该是http开头")
        @JsonProperty(required = true)
        private String fileUrl;

        @JsonPropertyDescription("问题")
        @JsonProperty(required = true)
        private String question;
    }


    @FunctionCallRegistry(functionDesc = "根据传入的文件回答问题", scene = {ChatSceneEnum.NORMAL_USER, ChatSceneEnum.NORMAL_GROUP})
    public String queryContentFromDocument(FileParam param) {
        String fileUrl = param.getFileUrl();
        if (!StringUtils.startsWith(fileUrl, FILE_URL_PREFIX) && !StringUtils.startsWith(fileUrl, HTTP)) {
            fileUrl = FILE_URL_PREFIX + fileUrl;
        }
        TikaDocumentReader documentReader = new TikaDocumentReader(fileUrl);
        TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();
        List<Document> transform = tokenTextSplitter.transform(documentReader.read());
        SimpleVectorStore vectorStore = new SimpleVectorStore(springAiEmbeddings.getEmbeddingModel());
        vectorStore.add(transform);
        SearchRequest request = SearchRequest.query(param.getQuestion());
        List<Document> documents = vectorStore.similaritySearch(request);
        List<OpenAiApi.ChatCompletionMessage> messages = Lists.newArrayList(new OpenAiApi.ChatCompletionMessage(String.format(PROMPT, param.getQuestion(),
                StringUtils.join(documents.stream().map(Document::getContent).collect(Collectors.toList()), "\n\n")), OpenAiApi.ChatCompletionMessage.Role.USER));
        OpenAiApi.ChatCompletion chatCompletion = springAiChat.generateMsgWithMsgList(messages, false);
        return Optional.ofNullable(chatCompletion).map(OpenAiApi.ChatCompletion::choices).map(list -> list.get(0)).map(OpenAiApi.ChatCompletion.Choice::message).map(OpenAiApi.ChatCompletionMessage::content).orElse(null);
    }


    public String summaryDoc(String fileUrl) {
        TikaDocumentReader documentReader = new TikaDocumentReader(fileUrl);
        SummaryMetadataEnricher summaryMetadataEnricher = new SummaryMetadataEnricher(new OpenAiChatModel(springAiChat.openAiApi), Lists.newArrayList(SummaryMetadataEnricher.SummaryType.CURRENT));
        List<Document> transformDocumentList = documentReader.read();
        List<Document> transform;
        int sumTokens;
        while (true) {
            transform = summaryMetadataEnricher.transform(transformDocumentList);
            sumTokens = transform.stream().mapToInt(document -> tokenCalculation.getMessageTextTokenCount(document.getMetadata().get("section_summary").toString())).sum();
            if (sumTokens < 2000) {
                break;
            }else {
                String str = transform.stream().map(doc -> doc.getMetadata().get("section_summary").toString()).collect(Collectors.joining("\n"));
                byte[] byteArray = str.getBytes();
                ByteArrayResource byteArrayResource = new ByteArrayResource(byteArray);
                transformDocumentList = new TikaDocumentReader(byteArrayResource).read();
            }
        }
        return transform.stream().map(map -> map.getMetadata().get("section_summary").toString()).collect(Collectors.joining("\n"));
    }

}

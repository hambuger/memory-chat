package com.github.hambuger.memory.chat.memory.constants;

/**
 * @author hanjiabao
 * @since 2024/7/3
 */
public class LangChainConstants {

    public static final String API_KEY = System.getProperty("OPENAI_API_KEY", "sk-xxx");

    public static final String API_HOST = "https://api.openai.com/v1";

    public static final String LOCAL = "127.0.0.1";

    public static final int PROXY_PORT = 7890;

    public static final Double TEMPLATE = 0.0d;

    public static final String MODEL_NAME = "gpt-4o";

    public static final String DIRECTORY_PATH = "test";

    public static final String TEXT_EMBEDDING_3_SMALL = "text-embedding-3-small";
}

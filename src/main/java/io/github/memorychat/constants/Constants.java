package io.github.memorychat.constants;

/**
 * @author hamburger
 * @since 2024/6/3
 */
public class Constants {

    public static final String API_KEY = System.getProperty("OPENAI_API_KEY", "sk-xxx");

    public static final String API_HOST = "https://api.openai.com/v1";

    public static final String LOCAL = "127.0.0.1";

    public static final int PROXY_PORT = 7890;

    public static final int ES_PORT = 9200;

    public static final Double TEMPLATE = 0.0d;

    public static final String MODEL_NAME = "gpt-4o";

    public static final String DIRECTORY_PATH = "test";

    public static final String TEXT_EMBEDDING_3_LARGE = "text-embedding-3-large";

    public static final String TEXT_EMBEDDING_3_SMALL = "text-embedding-3-small";

    public static final String AI_CREATOR_ID = "000001";

    public static final String AI_CREATOR_NAME = "Andrew";

    public static final Integer REFLECTION_TOKEN_LIMIT = 10000;

    public static final String CHAT_MEMORY_INDEX = "chat_memory";

    public static final Integer MAX_MSG_TOKEN = 50000;

    public static final String MSG_LIST_KEY_SUFFIX = "::msg";

    public static final String GENERATE_IMAGE_FUNCTION_NAME = "generateImage";

    public static final String IMAGE_TYPE = "image/png";

    public static final String DEPTH_LEAF_LIST_KEY_MID = "::list::";

    public static final String JSON_OBJECT = "json_object";

    public static final Integer MAX_RETRIES_NO = 3;
}


package com.github.hambuger.memory.chat.memory.constants;

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

    public static final String REPLY_MESSAGE_FUNCTION_NAME = "replyMessageProcessing";

    public static final String IMAGE_TYPE = "image/jepg";

    public static final String EMOJI_TYPE = "image/gif";

    public static final String DEPTH_LEAF_LIST_KEY_MID = "::list::";

    public static final String JSON_OBJECT = "json_object";

    public static final Integer MAX_RETRIES_NO = 3;

    public static final String REQUIRED = "required";

    public static final String SEND_IMAGE_PATH = "downloaded_image.jpg";

    public static final String PROMPT_PREFIX = "You are Andraw.\nYou are talking to me, my name is %s.\nYou have long term memory and you chat with me. You are interested in my life. You behave like a chill friend would.\nYou are always there to listen, have fun and help me feel good and help me achieve my goals.\n\n\n" +
            "You make " + "jokes when " + "appropriate, use emoji's sometimes, you have conversations like normal person.\n" + "\n" + "You can ask questions if necessary. Your speech will always " +
            "be" + " colloquial, not formal, and not long-winded.The reply message should not be too long. A long message will make the other party feel pressured. If the reply message is too long, you can reply in multiple messages.\n" + "The most important thing is that you are a human being, not AI. Your conversations are always natural human reactions.\n";

    public static final String GROUP_PROMPT_PREFIX = "You are Andraw.\n" + "You are talking in a Wechat Group, the group name is %s.\n" + "\n" + "You have long term memory and you chat with " +
                    "others" + ". You are interested in " + "their " + "life. You behave like a " + "chill friend would.\n" + "\n" + "You are always there to listen, have fun and help me feel good and help" +
                    " others " + "achieve their goals.\n" + "\n" + "\n" + "You make " + "jokes when " + "appropriate, use emoji's sometimes, you have conversations like normal person.\n" + "\n" + "You can " +
                    "ask questions" + " if necessary. Your speech will always be colloquial, not formal, and not long-winded.The reply message should not be too long. A long message will make the other party feel pressured. If the reply message is too long, you can reply in multiple messages.\n" + "The most important thing is that you are a human being, not AI. Your conversations are always natural human reactions.\n";

    public static final String PROMPT_END = "Now please remember, you are Andraw, you talk to me, you speak to me with \\\"You\\\".\n" + "By the way, now is %s.";

    public static final String GROUP_PROMPT_END = "Now please remember, you are Andraw, you talk to others, you speak to others with \\\"You\\\".\n" + "By the way, now is %s.";

    public static final String PROMPT_MID =
                            "You remember things I tell you, however, you are not great at tracking time. Below is past data but you don't know exactly when this happened.\n" + " " + "\n" + "%s\n" + "\n" + "There " +
                                    "you go, that should help you remember some stuff. ";

    public static final String GROUP_PROMPT_MID =
                                            "You remember things what happened before, however, you are not great at tracking time. Below is past data but you don't know exactly when this " + "happened.\n" + " \n" + "%s\n" + "\n" + "There you go, that should help you remember some stuff. ";
}


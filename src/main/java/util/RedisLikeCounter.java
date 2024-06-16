package util;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.LongAccumulator;

import memory.model.MemoryDTO;


public class RedisLikeCounter {

    private static final ConcurrentHashMap<String, LongAccumulator> countMap = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, List<String>> listMap = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, List<MemoryDTO>> chatMessageMap = new ConcurrentHashMap<>();


    public static void delOldMemory(String msgListKey, int i) {
        List<MemoryDTO> oldMemoryList = chatMessageMap.get(msgListKey);
        chatMessageMap.putIfAbsent(msgListKey, oldMemoryList.subList(i + 1, oldMemoryList.size()));
    }


    /**
     * Increment the counter for the given key by the specified amount.
     * If the key does not exist, it will be created with the initial value of amount.
     *
     * @param key    the key whose counter is to be incremented
     * @param amount the amount by which the counter is to be incremented
     */
    public static void incrBy(String key, long amount) {
        countMap.computeIfAbsent(key, k -> new LongAccumulator(Long::sum, 0)).accumulate(amount);
    }


    /**
     * Get the current value of the counter for the given key.
     * If the key does not exist, return 0.
     *
     * @param key the key whose counter value is to be retrieved
     * @return the current value of the counter
     */
    public static long get(String key) {
        return countMap.getOrDefault(key, new LongAccumulator(Long::sum, 0)).get();
    }


    public static void addElement(String key, String element) {
        listMap.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(element);
    }

    public static void reset(String key) {
        countMap.remove(key);
    }

    /**
     * Get the list of elements for the given key.
     * If the key does not exist, return an empty list.
     *
     * @param key the key whose list is to be retrieved
     * @return the list of elements
     */
    public static List<String> getList(String key) {
        return listMap.getOrDefault(key, new CopyOnWriteArrayList<>());
    }


    public static void addMsg(String key, MemoryDTO msg) {
        chatMessageMap.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(msg);
    }


    /**
     * Get the list of elements for the given key.
     * If the key does not exist, return an empty list.
     *
     * @param key the key whose list is to be retrieved
     * @return the list of elements
     */
    public static List<MemoryDTO> getMsg(String key) {
        return chatMessageMap.getOrDefault(key, new CopyOnWriteArrayList<>());
    }

}


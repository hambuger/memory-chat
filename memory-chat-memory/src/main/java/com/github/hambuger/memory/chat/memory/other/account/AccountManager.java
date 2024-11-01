package com.github.hambuger.memory.chat.memory.other.account;

import cn.hutool.core.date.DatePattern;
import com.alibaba.fastjson.JSON;
import com.github.hambuger.memory.chat.memory.other.util.CFR2Utils;
import com.github.hambuger.memory.chat.memory.other.util.EsClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class AccountManager {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Resource
    private EsClient esClient;

    @Resource
    private CFR2Utils cfr2Utils;

    private static final String ACCOUNT_SET_KEY ="account_ids";
    private static final Random random = new Random();

    // 初始化生成所有账号ID（16进制表示）
    public void populateAllAccountIds() {
        for (int r = 0; r <= 255; r++) {
            for (int g = 0; g <= 255; g++) {
                for (int b = 0; b <= 255; b++) {
                    String accountId = String.format("%02X%02X%02X", r, g, b);  // 将RGB转为16进制
                    redisTemplate.opsForSet().add(ACCOUNT_SET_KEY, accountId);  // 存入 Redis Set
                }
            }
        }
    }

    // 随机获取一个账号ID并删除
    public String getRandomAccountId() {
        String accountId = redisTemplate.opsForSet().pop(ACCOUNT_SET_KEY);  // 随机弹出并删除一个账号ID
        return accountId != null ? accountId : generateRandomAccountId();  // 如果没有值了，则生成一个新的
    }

    // 生成一个随机账号ID（RGB的16进制表示）
    private String generateRandomAccountId() {
        int r = random.nextInt(256);
        int g = random.nextInt(256);
        int b = random.nextInt(256);
        return String.format("%02X%02X%02X", r, g, b);  // 随机生成RGB值并格式化为16进制
    }

    public boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }

        String EMAIL_REGEX =
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

        Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);
        Matcher matcher = EMAIL_PATTERN.matcher(email);
        return matcher.matches();
    }

    public AccountDTO registerAccount(AccountDTO accountDTO) {
        if(!isValidEmail(accountDTO.getUserEmail())){
            throw new RuntimeException("非法的邮箱地址");
        }
        if(!isUniqueName(accountDTO.getUserEmail())){
            throw new RuntimeException("邮箱已被使用");
        }
        String userKey = UUID.randomUUID().toString().replace("-", "");
        String userId = getRandomAccountId();
        accountDTO.setUserId(userId);
        accountDTO.setUserKey(userKey);
        accountDTO.setAccountStatus("ACTIVE");
        accountDTO.setAccountPermissions("NORMAL");
        accountDTO.setCreationDate(DatePattern.NORM_DATETIME_FORMAT.format(new Date()));
        String base64Data = accountDTO.getUserAvatarBase64();
        if(StringUtils.isBlank(base64Data)){
            accountDTO.setUserAvatarBase64("https://file.hamburgerhan.com/默认头像.svg");
            accountDTO.setUserAvatarUrl("https://file.hamburgerhan.com/默认头像.svg");
        }else{
            accountDTO.setUserAvatarUrl(cfr2Utils.uploadBase64(base64Data, userId + "." + base64Data.split(";")[0].split("/")[1]));
        }
        if (StringUtils.isBlank(accountDTO.getUserKey())) {




        }
        IndexRequest indexRequest = new IndexRequest("user").id(userId).source(JSON.toJSONString(accountDTO), XContentType.JSON);
        try {
            esClient.index(indexRequest);
        } catch (IOException e) {
            log.error("insert user error", e);
        }
        return accountDTO;

    }

    private boolean isUniqueName(String userEmail) {

        SearchRequest searchRequest = new SearchRequest("user");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        QueryBuilder termQuery = new TermsQueryBuilder("userEmail", userEmail.trim());
        searchSourceBuilder.query(termQuery);
        searchRequest.source(searchSourceBuilder);
        try {
            SearchResponse searchResponse = esClient.search(searchRequest);
            if(searchResponse!= null && searchResponse.getHits().getTotalHits().value > 0){
                return false;
            }

        } catch (Exception e) {
            log.error("isUniqueName", e);
        }
        return true;
    }

    public List<AccountDTO> queryByUserIds(List<String> userIdList){
        SearchRequest searchRequest = new SearchRequest("user");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        QueryBuilder termQuery = new TermsQueryBuilder("userId", userIdList);
        searchSourceBuilder.query(termQuery);
        searchRequest.source(searchSourceBuilder);
        try {
            SearchResponse searchResponse = esClient.search(searchRequest);
            if(searchResponse!= null && searchResponse.getHits().getTotalHits().value > 0){
                List<AccountDTO> accountDTOS = new ArrayList<>();

                for (SearchHit hit : searchResponse.getHits()) {
                    AccountDTO accountDTO = JSON.parseObject(hit.getSourceAsString(), AccountDTO.class);
                    accountDTOS.add(accountDTO);
                }
                return accountDTOS;
            }

        } catch (Exception e) {
            log.error("isUniqueName", e);
        }
        return new ArrayList<>();

    }

    public AccountDTO loginAccount(AccountDTO accountDTO) {

        SearchRequest searchRequest = new SearchRequest("user");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        QueryBuilder termQuery = new TermsQueryBuilder("userEmail", accountDTO.getUserEmail().trim());
        searchSourceBuilder.query(termQuery);
        searchRequest.source(searchSourceBuilder);
        try {
            SearchResponse searchResponse = esClient.search(searchRequest);
            if (searchResponse == null && searchResponse.getHits().getTotalHits().value == 0) {
                throw new RuntimeException("账号不存在");
            }
            AccountDTO parsedObject = JSON.parseObject(Arrays.stream(searchResponse.getHits().getHits()).toList().get(0).getSourceAsString(), AccountDTO.class);

            if (StringUtils.equals(parsedObject.getUserName(), accountDTO.getUserName())) {
                return parsedObject;
            } else {
                throw new RuntimeException("名称不正确");
            }

        } catch (Exception e) {
            log.error("loginAccount", e);
        }
        return null;


    }

    public String getUserIdByUserKey(String userKey){
        SearchRequest searchRequest = new SearchRequest("user");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        QueryBuilder termQuery = new TermsQueryBuilder("userKey", userKey.trim());
        searchSourceBuilder.query(termQuery);
        searchRequest.source(searchSourceBuilder);
        try {
            SearchResponse searchResponse = esClient.search(searchRequest);
            if (searchResponse == null && searchResponse.getHits().getTotalHits().value == 0) {
                throw new RuntimeException("账号不存在");
            }
            AccountDTO parsedObject = JSON.parseObject(Arrays.stream(searchResponse.getHits().getHits()).toList().get(0).getSourceAsString(), AccountDTO.class);

            return parsedObject.getUserId();

        } catch (Exception e) {
            log.error("getUserIdByUserKey", e);
        }
        return null;
    }

}

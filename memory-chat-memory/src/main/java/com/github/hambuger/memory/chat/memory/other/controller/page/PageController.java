package com.github.hambuger.memory.chat.memory.other.controller.page;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.github.hambuger.memory.chat.memory.other.account.AccountDTO;
import com.github.hambuger.memory.chat.memory.other.account.AccountManager;
import com.github.hambuger.memory.chat.memory.other.util.CFR2Utils;
import com.github.hambuger.memory.chat.memory.other.util.EsClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class PageController {

    private static final String API_KEY = "0575dc0320b546a78b89faabb7a11d04";

    @Resource
    private EsClient esClient;

    @Resource
    private CFR2Utils cfr2Utils;

    @Resource
    private AccountManager accountManager;

    @GetMapping("/posts")
    public List<PostsPage> posts(@RequestParam(value = "userKey") String userKey) {
        SearchRequest searchRequest = new SearchRequest("posts");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.sort("createdAt", SortOrder.DESC);
        if(!StringUtils.equals(userKey, API_KEY)){
            QueryBuilder termQuery = QueryBuilders.boolQuery()
//                    .must(new TermsQueryBuilder("permissions", Lists.newArrayList("NORMAL")))
                    .must(new TermsQueryBuilder("status", "ACTIVE"));
            searchSourceBuilder.query(termQuery);
        }
        searchRequest.source(searchSourceBuilder);
        try {
            SearchResponse searchResponse = esClient.search(searchRequest);

            List<PostsPage> postsPageList = Arrays.stream(searchResponse.getHits().getHits()).map(hit -> JSON.parseObject(hit.getSourceAsString(), PostsPage.class)).collect(Collectors.toList());
            List<String> pageIdList = postsPageList.stream().map(PostsPage::getId).distinct().collect(Collectors.toList());
            Map<String,Long> likeMap = getLikeCounts(pageIdList);
            Map<String,Long> commentMap = getCommentCounts(pageIdList);
            String userId = accountManager.getUserIdByUserKey(userKey);
            if(StringUtils.isNotBlank(userId)){
                Map<String,Integer> likeStatusMap = getLikeStatusMap(pageIdList, userId);
                postsPageList.forEach(page -> page.setLikeStatus(Optional.ofNullable(likeStatusMap).map(likeMap1 -> likeMap1.get(page.getId())).orElse(0)));

            }
            postsPageList.forEach(page -> {
                page.setLikes(Optional.ofNullable(likeMap).map(likeMap1 -> likeMap1.get(page.getId())).map(Long::intValue).orElse(0));
                page.setComments(Optional.ofNullable(commentMap).map(likeMap1 -> likeMap1.get(page.getId())).map(Long::intValue).orElse(0));
            });
            return postsPageList;
        } catch (Exception e) {
            log.error("posts", e);
        }
        return new ArrayList<>();

    }

    private Map<String, Integer> getLikeStatusMap(List<String> pageIdList, String userId) {
        SearchRequest searchRequest = new SearchRequest("comments"); // 替换为你的索引名
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // 构建查询
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termsQuery("commentId", pageIdList))
                .filter(QueryBuilders.termsQuery("commentStatus", "ACTIVE"))
                .filter(QueryBuilders.termsQuery("commentType", "LIKE"))
                .filter(QueryBuilders.termsQuery("commentUserId",  userId));
        searchSourceBuilder.query(boolQuery);
        searchRequest.source(searchSourceBuilder);
        // 执行搜索
        SearchResponse searchResponse;
        try {
            searchResponse = esClient.search(searchRequest);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Map<String, Integer> resultMap = new HashMap<>();
        if(searchResponse != null && searchResponse.getHits() != null && searchResponse.getHits().getHits() != null && searchResponse.getHits().getHits().length > 0){
            for (String pageId : pageIdList) {
                resultMap.put(pageId, 0);
            }
            for (SearchHit hit : searchResponse.getHits().getHits()) {
                CommentInfo commentInfo = JSON.parseObject(hit.getSourceAsString(), CommentInfo.class);
                resultMap.put(commentInfo.getCommentId(), 1);
            }
        }
        return resultMap;
    }

    public Map<String,Long> getLikeCounts(List<String> pageIdList) throws IOException {
        SearchRequest searchRequest = new SearchRequest("comments"); // 替换为你的索引名
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // 构建查询
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termsQuery("commentId", pageIdList))
                .filter(QueryBuilders.termsQuery("commentStatus", "ACTIVE"))
                .filter(QueryBuilders.termsQuery("commentType", "LIKE"));

        searchSourceBuilder.query(boolQuery);

        // 添加聚合
        searchSourceBuilder.aggregation(AggregationBuilders
                .terms("group_by_bizId")
                .field("commentId")
                .subAggregation(AggregationBuilders.count("count").field("_id"))); // 使用 _id 进行计数

        searchRequest.source(searchSourceBuilder);

        // 执行搜索
        SearchResponse searchResponse = esClient.search(searchRequest);

        // 处理结果
        Map<String,Long> result = new HashMap<>();
        Terms terms = searchResponse.getAggregations().get("group_by_bizId");
        for (Terms.Bucket bucket : terms.getBuckets()) {
            String bizId = bucket.getKeyAsString();
            long count = bucket.getDocCount();
            result.put(bizId, count);
        }
        return result;
    }

    public Map<String,Long> getCommentCounts(List<String> pageIdList) throws IOException {
        SearchRequest searchRequest = new SearchRequest("comments"); // 替换为你的索引名
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // 构建查询
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termsQuery("commentId", pageIdList))
                .filter(QueryBuilders.termsQuery("commentStatus", "ACTIVE"))
                .filter(QueryBuilders.termsQuery("commentType", "PAGE"))
                .filter(QueryBuilders.termsQuery("parentCommentId", pageIdList));

        searchSourceBuilder.query(boolQuery);

        // 添加聚合
        searchSourceBuilder.aggregation(AggregationBuilders
                .terms("group_by_bizId")
                .field("commentId")
                .subAggregation(AggregationBuilders.count("count").field("_id"))); // 使用 _id 进行计数

        searchRequest.source(searchSourceBuilder);

        // 执行搜索
        SearchResponse searchResponse = esClient.search(searchRequest);

        // 处理结果
        Map<String,Long> result = new HashMap<>();
        Terms terms = searchResponse.getAggregations().get("group_by_bizId");
        for (Terms.Bucket bucket : terms.getBuckets()) {
            String bizId = bucket.getKeyAsString();
            long count = bucket.getDocCount();
            result.put(bizId, count);
        }
        return result;
    }

    public Integer getLikeStatus(String likeId, String userId){
        if(StringUtils.isBlank(userId)){
            return 0;
        }
        SearchRequest searchRequest = new SearchRequest("comments");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        QueryBuilder query = QueryBuilders.boolQuery().must(new TermsQueryBuilder("commentStatus", "ACTIVE"))
                .must(new TermsQueryBuilder("commentType", "LIKE"))
                .must((new TermsQueryBuilder("parentCommentId", likeId)))
                .must((new TermsQueryBuilder("commentUserId", userId)));
        searchSourceBuilder.query(query);
        searchRequest.source(searchSourceBuilder);
        try {
            SearchResponse searchResponse = esClient.search(searchRequest);
            if(searchResponse.getHits() == null || searchResponse.getHits().getHits() == null || searchResponse.getHits().getHits().length == 0){
                return 0;
            }
            CommentInfo commentInfo = JSON.parseObject(searchResponse.getHits().getHits()[0].getSourceAsString(), CommentInfo.class);
            return commentInfo != null ? 1 : 0;
        } catch (Exception e) {
            log.error("posts", e);
        }
        return 0;
    }

    public int getLikes(String pageId){
        SearchRequest searchRequest = new SearchRequest("comments");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        QueryBuilder query = QueryBuilders.boolQuery().must(new TermsQueryBuilder("commentStatus", "ACTIVE"))
                .must(new TermsQueryBuilder("commentType", "LIKE"))
                .must((new TermsQueryBuilder("commentId", pageId)));
        searchSourceBuilder.query(query);
        searchRequest.source(searchSourceBuilder);
        try {
            SearchResponse searchResponse = esClient.search(searchRequest);
            if(searchResponse.getHits() == null || searchResponse.getHits().getHits() == null || searchResponse.getHits().getHits().length == 0){
                return 0;
            }
            return searchResponse.getHits().getHits().length;
        } catch (Exception e) {
            log.error("posts", e);
        }
        return 0;
    }

    @GetMapping("/posts/{id}")
    public PostsPageDetail getPostById(@PathVariable String id, @RequestParam(value = "userId", required = false) String userId) {
        SearchRequest searchRequest = new SearchRequest("posts");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        QueryBuilder query = new TermsQueryBuilder("id", id);
        searchSourceBuilder.query(query);
        searchRequest.source(searchSourceBuilder);
        try {
            SearchResponse searchResponse = esClient.search(searchRequest);
            PostsPageDetail postsPage = JSON.parseObject(searchResponse.getHits().getHits()[0].getSourceAsString(), PostsPageDetail.class);
            postsPage.setLikes(getLikes(postsPage.getId()));
            postsPage.setCommentVOList(getCommentList(postsPage.getId()));
            postsPage.setLikeStatus(getLikeStatus(id,userId));
            return postsPage;
        } catch (Exception e) {
            log.error("posts", e);
        }
        return null;
    }

    private List<CommentVO> getCommentList(String id) {
        SearchRequest searchRequest = new SearchRequest("comments");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        QueryBuilder query = QueryBuilders.boolQuery().must(new TermsQueryBuilder("commentStatus", "ACTIVE"))
                .must(new TermsQueryBuilder("commentType", "PAGE"))
                .must((new TermsQueryBuilder("commentId", id)));
        searchSourceBuilder.query(query);
        searchSourceBuilder.size(10000);
        searchRequest.source(searchSourceBuilder);
        try {
            SearchResponse searchResponse = esClient.search(searchRequest);
            SearchHits hits = searchResponse.getHits();
            List<CommentVO> commentVOList = new ArrayList<>();

            for (SearchHit hit : hits.getHits()) {
                CommentInfo commentInfo = JSON.parseObject(hit.getSourceAsString(), CommentInfo.class);
                CommentVO commentVO = new CommentVO();
                commentVO.setId(hit.getId());
                commentVO.setPageId(commentInfo.getCommentId());
                commentVO.setCommentId(commentInfo.getParentCommentId());
                commentVO.setCommentText(commentInfo.getCommentText());
                commentVO.setCommentStartIndex(commentInfo.getCommentStartIndex());
                commentVO.setCommentEndIndex(commentInfo.getCommentEndIndex());
                commentVO.setUserId(commentInfo.getCommentUserId());
                commentVO.setCommentTime(DateUtil.parse(commentInfo.getCommentTime(), DatePattern.NORM_DATETIME_FORMAT));
                commentVOList.add(commentVO);
            }
            if(CollectionUtils.isEmpty(commentVOList)){
                return new ArrayList<>();
            }
            List<String> userIdList = commentVOList.stream().map(CommentVO::getUserId).toList();
            List<AccountDTO> accountDTOS = accountManager.queryByUserIds(userIdList);
            Map<String, AccountDTO> userIdAndNameMap = accountDTOS.stream().collect(Collectors.toMap(AccountDTO::getUserId, Function.identity()));
            commentVOList.forEach(vo -> {
                vo.setUserName(userIdAndNameMap.get(vo.getUserId()).getUserName());
                vo.setUserAvatarUrl(userIdAndNameMap.get(vo.getUserId()).getUserAvatarUrl());
            });
            return commentVOList;
        } catch (Exception e) {
            log.error("getCommentList", e);
        }
        return new ArrayList<>();



    }

    @PostMapping("/posts/save")
    public String savePosts(@RequestBody PostsPage postsPage) {
        if (!StringUtils.equals(postsPage.getAuthKey(), API_KEY)) {
            return null;
        }
        String postId = postsPage.getId();
        if (StringUtils.isBlank(postId)) {
            postsPage.setId(UUID.randomUUID().toString().replace("-", ""));
            postsPage.setAuthor("Hamburger");
            postsPage.setComments(0);
            postsPage.setLikes(0);
            postsPage.setCreatedAt(DateUtil.format(new Date(), DatePattern.NORM_DATETIME_FORMAT));
        }
        if(StringUtils.isNotBlank(postsPage.getImage())){
            if(postsPage.getImage().startsWith("data")){
                String avatarUrl = cfr2Utils.uploadBase64(postsPage.getImage(), postsPage.getId() + "." + postsPage.getImage().split(";")[0].split("/")[1]);
                postsPage.setImage(avatarUrl);
            }else{
                postsPage.setImage(postsPage.getImage());
            }
        }
        postsPage.setStatus(postsPage.getStatus());
        postsPage.setAuthKey(null);
        postsPage.setTitle(postsPage.getTitle());
        postsPage.setContent(postsPage.getContent());
        postsPage.setUpdater("Hamburger");
        postsPage.setUpdatedAt(DateUtil.format(new Date(), DatePattern.NORM_DATETIME_FORMAT));
        try {
            if (StringUtils.isNotBlank(postId)) {
                UpdateRequest updateRequest = new UpdateRequest("posts", postId).doc(JSON.toJSONString(postsPage), XContentType.JSON);
                esClient.update(updateRequest);
            } else {
                IndexRequest indexRequest = new IndexRequest("posts").id(postsPage.getId()).source(JSON.toJSONString(postsPage), XContentType.JSON);
                esClient.index(indexRequest);
            }
        } catch (Exception e) {
            log.error("posts", e);
        }
        return postsPage.getId();
    }


    @PostMapping("/posts/like")
    public Boolean like(@RequestBody LikeVO likeVO) {
        try {
            String userKey = accountManager.getUserIdByUserKey(likeVO.getUserKey());
            if(StringUtils.isBlank(userKey)){
                return false;
            }
            String id = likeVO.getPageId()+likeVO.getLikeId();
            CommentInfo commentInfo = new CommentInfo();
            commentInfo.setCommentId(likeVO.getPageId());
            commentInfo.setParentCommentId(likeVO.getLikeId());
            commentInfo.setCommentTime(DateUtil.format(new Date(), DatePattern.NORM_DATETIME_FORMAT));
            commentInfo.setCommentType("LIKE");
            commentInfo.setCommentText("LIKE");
            commentInfo.setCommentUserId(userKey);
            commentInfo.setCommentStatus(Objects.equals(likeVO.getLikeOperate(), 1) ? "ACTIVE" : "DISABLE");
            IndexRequest indexRequest = new IndexRequest("comments").id(id).source(JSON.toJSONString(commentInfo), XContentType.JSON);
            esClient.index(indexRequest);
        } catch (Exception e) {
            log.error("like", e);
        }
        return true;
    }

    @PostMapping("/posts/comment")
    public String comment(@RequestBody CommentVO commentVO) {
        try {
            String userKey = accountManager.getUserIdByUserKey(commentVO.getUserKey());
            if(StringUtils.isBlank(userKey)){
                return null;
            }
            String id = UUID.randomUUID().toString().replace("-", "");
            CommentInfo commentInfo = new CommentInfo();
            commentInfo.setCommentId(commentVO.getPageId());
            commentInfo.setParentCommentId(commentVO.getCommentId());
            commentInfo.setCommentTime(DateUtil.format(new Date(), DatePattern.NORM_DATETIME_FORMAT));
            commentInfo.setCommentType(commentVO.getCommentType());
            commentInfo.setCommentText(commentVO.getCommentText());
            commentInfo.setCommentUserId(userKey);
            commentInfo.setCommentStatus("ACTIVE");
            commentInfo.setCommentStartIndex(commentVO.getCommentStartIndex());
            commentInfo.setCommentEndIndex(commentVO.getCommentEndIndex());
            IndexRequest indexRequest = new IndexRequest("comments").id(id).source(JSON.toJSONString(commentInfo), XContentType.JSON);
            esClient.index(indexRequest);
            return  id;
        } catch (Exception e) {
            log.error("comment", e);
        }
        return null;
    }

    @PostMapping("/upload")
    @ResponseBody
    public Map<String, Object> uploadFile(@RequestHeader("Authorization") String authToken,
                                                          @RequestParam("image") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        // 验证 token 的合法性（简化逻辑）
        if (!validateToken(authToken)) {
            response.put("status", "fail");
            response.put("message", "Unauthorized");
            return response;
        }

        String filename = UUID.randomUUID() + "-" + file.getOriginalFilename();

        try {
            String remoteUrl = cfr2Utils.uploadFile(file, filename);

            // 返回多字段信息
            Map<String, Object> data = new HashMap<>();
            data.put("url", remoteUrl);
            data.put("filename", file.getOriginalFilename());
            data.put("size", file.getSize());

            response.put("status", "success");
            response.put("message", "File uploaded successfully");
            response.put("data", data);

            return response;
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "File upload failed");
            return response;
        }
    }

    private boolean validateToken(String token) {
        return ("Bearer " + API_KEY).equals(token);
    }

}

package com.kk.linyuanbin.demoforredis.chapter;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ZParams;

import java.util.*;

/**
 * @author linyuanbin
 * @description functions for article management
 * @date 2020/9/11-9:11
 */
public class Chapter01 {
    private static final int ONE_WEEK_IN_SECONDS = 7 * 60 * 60 * 24;
    private static final int VOTE_SCORE = ONE_WEEK_IN_SECONDS / 200;
    private static final int ARTICLES_PER_PAGE = 25;

    public static void main(String[] args) {
        new Chapter01().run();
    }

    public void run(){
        /*
        * 建立redis连接，
        * 并选择使用下标为15的库（即最后一个库）
        * */
        Jedis conn = new Jedis("localhost", 6379, 100000);
        conn.select(15);

        String articleId = postArticle(
                conn, "username", "A title", "http://www.qq.com");
        System.out.println("We posted a new article with id: " + articleId);
        System.out.println("Its HASH looks like:");
        Map<String,String> articleData = conn.hgetAll("article:" + articleId);
        for (Map.Entry<String,String> entry : articleData.entrySet()){
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }

        System.out.println();

        articleVote(conn, "other_user", "article:" + articleId);
        String votes = conn.hget("article:" + articleId, "votes");
        System.out.println("We voted for the article, it now has votes: " + votes);
        assert Integer.parseInt(votes) > 1;

        System.out.println("The currently highest-scoring articles are:");
        List<Map<String,String>> articles = getArticles(conn, 1);
        printArticles(articles);
        assert articles.size() >= 1;

        addGroup(conn, articleId, new String[]{"new-group"});
        System.out.println("We added the article to a new group, other articles include:");
        articles = getGroupArticles(conn, "new-group", 1);
        printArticles(articles);
        assert articles.size() >= 1;
        
    }
    
    /**
     * @description: postArticle 发表文章，并返回文章id
     * @param [conn, user, title, link]
     * @return java.lang.String
     * @throws 
     * @author linyuanbin
     * @date 2020/9/11-09:23
     */
    
    public String postArticle(Jedis conn, String user, String title, String link) {
        /*
        * incr()函数将给定key集合中的保存的value自增1并返回
        * */
        String articleId = String.valueOf(conn.incr("article:"));

        /*
        * 在redis中添加对应文章的投票记录集合（用于记录给该文章投票的users），
        * 作者默认给自己的文章投票，
        * 设置文章投票记录集合过期时间为一周（因为文章投票周期为一周，超过即不许再投票）
        * */
        String voted = "voted:" + articleId;
        conn.sadd(voted, user);
        conn.expire(voted, ONE_WEEK_IN_SECONDS);

        /*
        * 获取当前时间（单位为s）,
        * 定义article存储在redis中的key
        * */
        long now = System.currentTimeMillis() / 1000;
        String article = "article:" + articleId;

        /*
        * 初始化存储article信息的hashmap，
        * 并向hashmap中存储article的信息
        * */
        HashMap<String, String> articleData = new HashMap<>();
        articleData.put("title", title);
        articleData.put("link", link);
        articleData.put("user", user);
        articleData.put("now", String.valueOf(now));
        articleData.put("votes", "1");

        /*
        * 将存储article信息的hashmap保存到redis中（数据格式为redis中的hash），
        * 在redis中的有序集“score：”中保存本文章的分值数据（用于保存与修改投票分值，并按分值排序与获取文章），
        * 在redis中的有序集“time：”中保存本文章的发布时间数据（用于按发布时间排序与获取文章）
        * */
        conn.hmset(article, articleData);
        conn.zadd("score:", now + VOTE_SCORE, article);
        conn.zadd("time:", now, article);

        return articleId;
    }

    /**
     * @description: articleVote 文章投票（在一定条件下）
     * @param [conn, user, article]
     * @return void
     * @throws
     * @author linyuanbin
     * @date 2020/9/11-09:25
     */

    public void articleVote(Jedis conn, String user, String article) {
        /*
        * 获取当前时间可投票文章的应发表时间，
        * 如果给定文章的发表时间早于应发表时间，则无法进行投票，函数直接返回
        * */
        long cutoff = (System.currentTimeMillis() / 1000) - ONE_WEEK_IN_SECONDS;
        if (conn.zscore("time:", article) < cutoff) {
            return;
        }

        /*
        * 根据给定的article的key获取article的id，
        * 尝试将投票的user记录到给定article的投票记录集合中去，
        * 若添加记录成功（返回为1，说明该article的投票记录集合中没有该user的记录，即该user未给该article投票），则对应修改存储该article信息的hash集合中的voted字段与记录article分值的有序集合中该article的分值
        * 否则（返回0，代表尝试添加记录失败，说明该article的投票记录集合中已有该投票user的记录，即该user已给该article投票），不执行任何业务
        * */
        String articleId = article.substring(article.indexOf(":") + 1);
        if (conn.sadd("voted:" + articleId, user) == 1){
            conn.zincrby("score:", VOTE_SCORE, article);
            conn.hincrBy(article, "votes", 1);
        }
    }

    /**
     * @description: getArticles 获取某页上的所有文章（使用默认排序，即根据文章分值score排序，即从记录文章分值的有序集的某页上获取所有文章）
     * @param [conn, page]
     * @return java.util.List<java.util.Map<java.lang.String,java.lang.String>>
     * @throws 
     * @author linyuanbin
     * @date 2020/9/11-09:29
     */
    
    public List<Map<String, String>> getArticles(Jedis conn, int page) {
        return getArticles(conn, page, "score:");
    }

    /**
     * @description: getArticles 根据指定排序方式获得某页上的所有文章
     * @param [conn, page, order]
     * @return java.util.List<java.util.Map<java.lang.String,java.lang.String>>
     * @throws 
     * @author linyuanbin
     * @date 2020/9/11-09:34
     */
    
    public List<Map<String, String>> getArticles(Jedis conn, int page, String order) {
        /*
        * 获取给定页面的首篇与末篇文章的下标
        * */
        int start = (page - 1) * ARTICLES_PER_PAGE;
        int end = start + ARTICLES_PER_PAGE - 1;

        /*
        * 移除并返回给定排序方式order的有序集合中的给定起始start与结束end下标范围内的所有articles的key（以id为表现形式），
        * 定义与初始化存储articles的list容器，
        * 获取给定ids集合中所有id的对应的article的hash集合（即存储这些article信息的hash集合），并添加新字段id到hash集合中，
        * 将该hash集合添加到articles的list容器中，
        * 返回list容器
        * */
        Set<String> ids = conn.zrevrange(order, start, end);
        List<Map<String, String>> articles = new ArrayList<>();
        for (String id : ids){
            Map<String, String> articlaData = conn.hgetAll(id);
            articlaData.put("id", id);
            articles.add(articlaData);
        }

        return articles;
    }

    /**
     * @description: addGroup 将某一篇文章添加到多个给定群组中
     * @param [conn, articleId, toAdd]
     * @return void
     * @throws 
     * @author linyuanbin
     * @date 2020/9/11-09:39
     */
    
    public void addGroup(Jedis conn, String articleId, String [] toAdd) {
        /*
        * 获取给定articleId的article的key，
        * 将该article的key保存到给定群组的集合中
        * */
        String article = "article:" + articleId;
        for (String group : toAdd) {
            conn.sadd("group:" + group, article);
        }
    }

    /**
     * @description: getGroupArticles 获得某个群组的记录文章分值的有序集中某页上的所有文章（使用默认排序，即根据文章分值score排序，即从对应群组的记录文章分值的有序集的某页上获取所有文章）
     * @param [conn, group, page]
     * @return java.util.List<java.util.Map<java.lang.String,java.lang.String>>
     * @throws
     * @author linyuanbin
     * @date 2020/9/11-09:40
     */

    public List<Map<String, String>> getGroupArticles(Jedis conn, String group, int page) {
        return getGroupArticles(conn, group, page, "score:");
    }
    
    /**
     * @description: getGroupArticles 根据指定排序方式获得某个群组的记录文章分值的有序集中某页上的所有文章
     * @param [conn, group, page, order]
     * @return java.util.List<java.util.Map<java.lang.String,java.lang.String>>
     * @throws 
     * @author linyuanbin
     * @date 2020/9/11-09:43
     */
    
    public List<Map<String, String>> getGroupArticles(Jedis conn, String group, int page, String order) {
        /*
        * 获取给定群组的根据给定排序方式记录文章的有序集合的key（名称）
        * */
        String key = order + group;

        /*
        * 首先判断给定群组的给定排序方式的有序集是否存在于缓存中，
        * 若不存在，则新建该有序集（通过给定群组的集合set与给定排序方式的有序集合zset进行交集运算得到，因为无序集set中默认value为1，而有序集中value为浮点数，所以需要设置交集取值策略为MAX，即取相同key下的最大value为新有序集对应key的value），
        * 设置新获得的有序集（即给定群组的给定排序方式的有序集）的过期时间，因为交集运算消耗较多系统资源，缓存交集运算结果方便或许业务直接调用，极大提升1效率与减少系统消耗
        * */
        if (!conn.exists(key)){
            ZParams params = new ZParams().aggregate(ZParams.Aggregate.MAX);
            conn.zinterstore(key, params, "group:" + group, order);
            conn.expire(key, 60);
        }
        return getArticles(conn, page, key);
    }
    
    /**
     * @description: printArticles 打印出给定文章列表上所有文章的所有信息
     * @param [articles]
     * @return void
     * @throws 
     * @author linyuanbin
     * @date 2020/9/11-09:44
     */
    
    private void printArticles(List<Map<String, String>> articles) {
        for (Map<String, String> article : articles) {
            System.out.println(" id: " + article.get("id"));
            for (Map.Entry<String, String> entry : article.entrySet()){
                if (entry.getKey().equals("id")){
                    continue;
                }
                System.out.println(" " + entry.getKey() + ":" + entry.getValue());
            }
        }
    }
}

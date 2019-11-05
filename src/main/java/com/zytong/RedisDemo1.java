package com.zytong;

import redis.clients.jedis.Jedis;

import java.util.*;

public class RedisDemo1 {

    private static final int ARTICLES_PER_PAGE = 5;

    public static void main(String[] args) {
        new RedisDemo1().run();
    }

    public void run() {
        Jedis conn = new Jedis("localhost");
        conn.select(10);

        String articleId = postArticle(conn, "username2", "A title2", "http://www.google.com");
        System.out.println("We posted a new article with id: " + articleId);

//        String articleId = "1";
        articleVote(conn, "other_user", "article:" + articleId);

        List<Map<String, String>> articles = getArticles(conn, 1);
        printArticles(articles);
    }

    public String postArticle(Jedis conn, String user, String title, String link) {
        String articleId = String.valueOf(conn.incr("article:"));//自增id

        String voted = "voted:" + articleId;
        conn.sadd(voted, user);

        long now = System.currentTimeMillis() / 1000;
        String article = "article:" + articleId;        //article:${id}
        HashMap<String, String> articleData = new HashMap<String, String>();
        articleData.put("title", title);
        articleData.put("link", link);
        articleData.put("user", user);
        articleData.put("now", String.valueOf(now));
        articleData.put("votes", "1");
        conn.hmset(article, articleData);
        conn.zadd("time:", now, article);
        conn.zadd("vote:",1,article);

        return articleId;
    }

    public void articleVote(Jedis conn, String user, String article) {
        String articleId = article.substring(article.indexOf(':') + 1);
        if (conn.sadd("voted:" + articleId, user) == 1) {      //voted:${id}
            conn.hincrBy(article, "votes", 1);
            conn.zincrby("vote:",1,article);
        }
    }

//    public List<Map<String, String>> getArticles(Jedis conn, int page) {
//        int start = (page - 1) * ARTICLES_PER_PAGE;
//        int end = start + ARTICLES_PER_PAGE - 1;
//
//        Set<String> ids = conn.zrevrange("time:", start, end);
//        List<Map<String, String>> articles = new ArrayList<Map<String, String>>();
//        for (String id : ids) {
//            Map<String, String> articleData = conn.hgetAll(id);
//            articleData.put("id", id);
//            articles.add(articleData);
//        }
//
//        return articles;
//    }

//    public List<Map<String, String>> getArticles(Jedis conn, int page) {
//        int start = (page - 1) * ARTICLES_PER_PAGE;
//        int end = start + ARTICLES_PER_PAGE - 1;
//
//        Set<String> ids = conn.zrevrange("vote:", start, end);
//        List<Map<String, String>> articles = new ArrayList<Map<String, String>>();
//        for (String id : ids) {
//            Map<String, String> articleData = conn.hgetAll(id);
//            articleData.put("id", id);
//            articles.add(articleData);
//        }
//
//        return articles;
//    }

    private void printArticles(List<Map<String, String>> articles) {
        for (Map<String, String> article : articles) {
            System.out.println("  id: " + article.get("id"));
            for (Map.Entry<String, String> entry : article.entrySet()) {
                if (entry.getKey().equals("id")) {
                    continue;
                }
                System.out.println("    " + entry.getKey() + ": " + entry.getValue());
            }
        }
    }

    /**
     * 取出评分最高的文章和取出最新发布的文章
     * 实现步骤：
     * 1、程序需要先使用ZREVRANGE取出多个文章ID，然后在对每个文章ID执行一次HGETALL命令来取出文章的详细信息，
     *    这个方法可以用于取出评分最高的文章，又可以用于取出最新发布的文章。
     * 需要注意的是：
     * 因为有序集合会根据成员的值从小到大排列元素，所以使用ZREVRANGE命令，已分值从大到小的排列顺序取出文章ID才是正确的做法
     *
     */

    public List<Map<String,String>> getArticles(Jedis conn, int page, String order) {
        //1、设置获取文章的起始索引和结束索引。
        int start = (page - 1) * ARTICLES_PER_PAGE;
        int end = start + ARTICLES_PER_PAGE - 1;
        //2、获取多个文章ID,
        Set<String> ids = conn.zrevrange(order, start, end);
        List<Map<String,String>> articles = new ArrayList<Map<String,String>>();
        for (String id : ids){
            //3、根据文章ID获取文章的详细信息
            Map<String,String> articleData = conn.hgetAll(id);
            articleData.put("id", id);
            //4、添加到ArrayList容器中
            articles.add(articleData);
        }

        return articles;
    }

    public List<Map<String, String>> getArticles(Jedis conn, int page) {
        //调用下面重载的方法
        return getArticles(conn, page,"vote:");
    }

}
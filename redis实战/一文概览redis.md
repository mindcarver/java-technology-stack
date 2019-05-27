## 初始Redis

1. Redis是一个远程内存数据库，它提供了5种不同类型的数据结构。
2. Redis是一个速度非常快的非关系数据库
3. memcached和Redis的区别：这两者都可以存储键值映射，彼此的性能也相差无几，但是R能以两种不同的方式将数据写入磁盘并且除了存储普通的字符串键之外，还可以存储其他4中结构，而m只能存储普通过的字符串键
4. R的单个命令都是原子性的

### Redis的操作实例

```java
 ##字符串
guanhangdeMacBook-Pro:~ guanhang$ redis-cli
127.0.0.1:6379> set hello world
OK
127.0.0.1:6379> get hello
"world"
127.0.0.1:6379> del hello
(integer) 1
127.0.0.1:6379> get hello
(nil)
127.0.0.1:6379> set number 0
OK
127.0.0.1:6379> get number
"0"
127.0.0.1:6379> INCR number
(integer) 1
127.0.0.1:6379> get number
"1"

##List
127.0.0.1:6379> RPUSH list-key item
(integer) 1
127.0.0.1:6379> RPUSH list-key item2
(integer) 2
127.0.0.1:6379> RPUSH list-key item
(integer) 3
127.0.0.1:6379> LRANGE list-key 0 -1
1) "item"
2) "item2"
3) "item"
127.0.0.1:6379> lindex list-key 1
"item2"
127.0.0.1:6379> lpop list-key
"item"
127.0.0.1:6379> LRANGE list-key 0 -1
1) "item2"
2) "item"

##SET 
127.0.0.1:6379> sadd set-key item
(integer) 1
127.0.0.1:6379> sadd set-key item2
(integer) 1
127.0.0.1:6379> sadd set-key item3
(integer) 1
127.0.0.1:6379> SMEMBERS set-key
1) "item3"
2) "item2"
3) "item"
127.0.0.1:6379> SISMEMBER set-key item4
(integer) 0
127.0.0.1:6379> SISMEMBER set-key item
(integer) 1
127.0.0.1:6379> SMEMBERS set-key
1) "item3"
2) "item2"
3) "item"
127.0.0.1:6379> SREM set-key item2
(integer) 1
127.0.0.1:6379> SREM set-key item2
(integer) 0
127.0.0.1:6379> SMEMBERS set-key
1) "item3"
2) "item"

##MAP
127.0.0.1:6379> hset hashkey subkey1 value1
(integer) 1
127.0.0.1:6379> hset hashkey subkey2 value2
(integer) 1
127.0.0.1:6379> hset hashkey subkey1 value1
(integer) 0
127.0.0.1:6379> hgetall hash-key
(empty list or set)
127.0.0.1:6379> hgetall hashkey
1) "subkey1"
2) "value1"
3) "subkey2"
4) "value2"
127.0.0.1:6379> hdel hashkey subkey2
(integer) 1
127.0.0.1:6379> hget hashkey subkey1
"value1"
127.0.0.1:6379> hgetall hashkey
1) "subkey1"
2) "value1"

##ZSET
127.0.0.1:6379> zadd zsetkey 728 member1
(integer) 1
127.0.0.1:6379> zadd zsetkey 982 member0
(integer) 1
127.0.0.1:6379> zrange zsetkey 0 -1 withscores
1) "member1"
2) "728"
3) "member0"
4) "982"
127.0.0.1:6379> zrangebyscore zsetkey 0 800 withscores
1) "member1"
2) "728"
127.0.0.1:6379> zrem zsetkey member1
(integer) 1
127.0.0.1:6379> zrange zsetkey 0 -1 withscores
1) "member0"
2) "982"
127.0.0.1:6379> 

##查看数据库大小并清空缓存
127.0.0.1:6379[15]> dbsize
(integer) 6
127.0.0.1:6379[15]> flushdb
OK
127.0.0.1:6379[15]> dbsize
```

### 使用案例

使用redis实现文章投票计数功能：

1. 使用article:键的累加帮助生成文章的ID
2. 使用article:id的散列来记录文章的信息
3. score:的有序集合来记录文章的分数
4. time:有序集合来记录文章生成的时间
5. group:name的无序集合来存储该组的文章

```java
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ZParams;

import java.util.*;

public class Chapter01
{
    private static final int ONE_WEEK_IN_SECONDS = 7 * 86400;
    private static final int VOTE_SCORE = 432;
    private static final int ARTICLES_PER_PAGE = 25;

    public static final void main(String[] args)
    {
        new Chapter01().run();
    }

    public void run()
    {
        Jedis conn = new Jedis("127.0.0.1",6379);
        //使用
        conn.select(15);
        //发布文章
        String articleId = postArticle(conn, "username", "A title",
                "http://www.google.com");
        System.out.println("We posted a new article with id: "
                + articleId);
        System.out.println("Its HASH looks like:");
        Map<String, String> articleData = conn
                .hgetAll("article:" + articleId);
        for (Map.Entry<String, String> entry : articleData.entrySet())
        {
            System.out.println("  " + entry.getKey() + ": "
                    + entry.getValue());
        }

        System.out.println();
        //对文章进行投票
        articleVote(conn, "other_user", "article:" + articleId);
        String votes = conn.hget("article:" + articleId, "votes");
        System.out.println(
                "We voted for the article, it now has votes: "
                        + votes);
        assert Integer.parseInt(votes) > 1;

        System.out.println(
                "The currently highest-scoring articles are:");
        //分页查询文章信息
        List<Map<String, String>> articles = getArticles(conn, 1);
        //打印文章信息
        printArticles(articles);
        assert articles.size() >= 1;
        //给文章分组
        addGroups(conn, articleId, new String[]
                { "new-group" });
        System.out.println(
                "We added the article to a new group, other articles include:");
        //获取组内文章信息
        articles = getGroupArticles(conn, "new-group", 1);
        printArticles(articles);
        assert articles.size() >= 1;
    }

    /**
     * 发布一篇文章
     */
    public String postArticle(Jedis conn, String user, String title,
                              String link)
    {

        //存储文章编号的key+1
        String articleId = String.valueOf(conn.incr("article:"));
        String voted = "voted:" + articleId;
        //对该用户投票的文章数+1
        conn.sadd(voted, user);
        //过期时间
        conn.expire(voted, ONE_WEEK_IN_SECONDS);

        long now = System.currentTimeMillis() / 1000;
        String article = "article:" + articleId;
        HashMap<String, String> articleData = new HashMap<String, String>();
        articleData.put("title", title);
        articleData.put("link", link);
        articleData.put("user", user);
        articleData.put("now", String.valueOf(now));
        articleData.put("votes", "1");
        //存储文章的信息
        conn.hmset(article, articleData);
        //存储文章的评分
        conn.zadd("score:", now + VOTE_SCORE, article);
        //存储文章的时间
        conn.zadd("time:", now, article);

        return articleId;
    }

    /**
     * 对文章进行投票
     */
    public void articleVote(Jedis conn, String user, String article)
    {
        long cutoff = (System.currentTimeMillis() / 1000)
                - ONE_WEEK_IN_SECONDS;
        //如果时间超过了1周则不予投票
        if (conn.zscore("time:", article) < cutoff)
        {
            return;
        }

        String articleId = article.substring(article.indexOf(':') + 1);
        if (conn.sadd("voted:" + articleId, user) == 1)
        {
            //分数增加
            conn.zincrby("score:", VOTE_SCORE, article);
            //投票数增加
            conn.hincrBy(article, "votes", 1l);
        }
    }
    //分页查询文章的排名
    public List<Map<String, String>> getArticles(Jedis conn, int page)
    {
        return getArticles(conn, page, "score:");
    }

    //分页查询文章的排名
    public List<Map<String, String>> getArticles(Jedis conn, int page,
                                                 String order)
    {
        int start = (page - 1) * ARTICLES_PER_PAGE;
        int end = start + ARTICLES_PER_PAGE - 1;

        Set<String> ids = conn.zrevrange(order, start, end);
        List<Map<String, String>> articles = new ArrayList<Map<String, String>>();
        for (String id : ids)
        {
            Map<String, String> articleData = conn.hgetAll(id);
            articleData.put("id", id);
            articles.add(articleData);
        }

        return articles;
    }

    //给文章添加分组信息
    public void addGroups(Jedis conn, String articleId, String[] toAdd)
    {
        String article = "article:" + articleId;
        for (String group : toAdd)
        {
            conn.sadd("group:" + group, article);
        }
    }

    public List<Map<String, String>> getGroupArticles(Jedis conn,
                                                      String group, int page)
    {
        return getGroupArticles(conn, group, page, "score:");
    }

    //获取该组的文章信息，按评分排序，无序集合默认分数为1，因此取交集的分数决定于score的分数
    public List<Map<String, String>> getGroupArticles(Jedis conn,
                                                      String group, int page, String order)
    {
        String key = order + group;
        if (!conn.exists(key))
        {
            ZParams params = new ZParams()
                    .aggregate(ZParams.Aggregate.MAX);
            conn.zinterstore(key, params, "group:" + group, order);
            conn.expire(key, 60);
        }
        return getArticles(conn, page, key);
    }

    //打印文章信息
    private void printArticles(List<Map<String, String>> articles)
    {
        for (Map<String, String> article : articles)
        {
            System.out.println("  id: " + article.get("id"));
            for (Map.Entry<String, String> entry : article
                    .entrySet())
            {
                if (entry.getKey().equals("id"))
                {
                    continue;
                }
                System.out.println("    " + entry.getKey()
                        + ": " + entry.getValue());
            }
        }
    }
}
```

## 使用Redis构建Web应用

### 使用Redis缓存cookie

本案例的介绍：

1. 对于登录的cookie，有两种常见的方法可以将登录信息存储在cookie里面：一种是签名cookie，一种是令牌cookie。签名cookie会在cookie中存储更多的信息，而令牌cookie会在后端数据库存储更多的信息
2. 我们将使用一个map来存储登录cookie令牌和用户的映射
3. 用户最近浏览的商品会放在一个zset里面，当记录超过25个时会进行裁剪
4. 只保留最新的1000万个会话，因此会有过期会话的清理

具体实现：

1. 使用login:的散列存储token到用户的映射
2. 使用recent:的有序集合来存储token以及其浏览的时间
3. 使用view:token的有序集合存储物品以及其浏览的时间

### 使用Redis实现购物车

1. 使用cart:session的散列来存储用户购物车的商品
2. 由于大部分WEB网页变化很少，因此采用R来缓存页面，由于页面过多，因此只选择缓存1万个页面，如果某页面包含的商品的浏览次数不是前1万名，不会缓存该网页
3. 商品的页面通常只会从数据库载入一两行数据，因此可以用R来缓存数据的行，这里使用一个守护线程，让指定的数据行缓存到Redis里面，并不定期的更新，缓存会将数据行转成JSON并存储在R的字符串中

### 实现细节

1. 使用cache:hash(request)的key来缓存页面
2. 使用delay:的有序集合存储数据行延迟时间，即多少秒更新一次
3. 使用schedule:的有序集合来存储数据行应该在何时缓存进R
4. 添加一个view:的有序集合来存储商品的浏览次数，每浏览一次其值-1，同时需要对该有序集合进行定期裁剪

```java
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.gson.Gson;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Tuple;

public class Chapter02
{
    public static final void main(String[] args) throws InterruptedException
    {
        new Chapter02().run();
    }

    public void run() throws InterruptedException
    {
        Jedis conn = new Jedis("localhost");
        conn.select(15);
        //登录cookie的相关信息存储
        testLoginCookies(conn);
        //购物车信息存储
        testShopppingCartCookies(conn);
        //数据行缓存
        testCacheRows(conn);
        //网页信息缓存
        testCacheRequest(conn);
    }

    public void testLoginCookies(Jedis conn) throws InterruptedException
    {
        System.out.println("\n----- testLoginCookies -----");
        String token = UUID.randomUUID().toString();

        updateToken(conn, token, "username", "itemX");
        System.out.println("We just logged-in/updated token: " + token);
        System.out.println("For user: 'username'");
        System.out.println();

        System.out.println(
                "What username do we get when we look-up that token?");
        String r = checkToken(conn, token);
        System.out.println(r);
        System.out.println();
        assert r != null;

        System.out.println(
                "Let's drop the maximum number of cookies to 0 to clean them out");
        System.out.println(
                "We will start a thread to do the cleaning, while we stop it later");
        //缓存清理线程
        CleanSessionsThread thread = new CleanSessionsThread(0);
        thread.start();
        Thread.sleep(1000);
        thread.quit();
        Thread.sleep(2000);
        if (thread.isAlive())
        {
            throw new RuntimeException(
                    "The clean sessions thread is still alive?!?");
        }

        long s = conn.hlen("login:");
        System.out.println(
                "The current number of sessions still available is: "
                        + s);
        assert s == 0;
    }

    public void testShopppingCartCookies(Jedis conn)
            throws InterruptedException
    {
        System.out.println("\n----- testShopppingCartCookies -----");
        String token = UUID.randomUUID().toString();

        System.out.println("We'll refresh our session...");
        updateToken(conn, token, "username", "itemX");
        System.out.println("And add an item to the shopping cart");
        addToCart(conn, token, "itemY", 3);
        Map<String, String> r = conn.hgetAll("cart:" + token);
        System.out.println("Our shopping cart currently has:");
        for (Map.Entry<String, String> entry : r.entrySet())
        {
            System.out.println("  " + entry.getKey() + ": "
                    + entry.getValue());
        }
        System.out.println();

        assert r.size() >= 1;

        System.out.println("Let's clean out our sessions and carts");
        CleanFullSessionsThread thread = new CleanFullSessionsThread(0);
        thread.start();
        Thread.sleep(1000);
        thread.quit();
        Thread.sleep(2000);
        if (thread.isAlive())
        {
            throw new RuntimeException(
                    "The clean sessions thread is still alive?!?");
        }

        r = conn.hgetAll("cart:" + token);
        System.out.println("Our shopping cart now contains:");
        for (Map.Entry<String, String> entry : r.entrySet())
        {
            System.out.println("  " + entry.getKey() + ": "
                    + entry.getValue());
        }
        assert r.size() == 0;
    }

    public void testCacheRows(Jedis conn) throws InterruptedException
    {
        System.out.println("\n----- testCacheRows -----");
        System.out.println(
                "First, let's schedule caching of itemX every 5 seconds");
        scheduleRowCache(conn, "itemX", 5);
        System.out.println("Our schedule looks like:");
        Set<Tuple> s = conn.zrangeWithScores("schedule:", 0, -1);
        for (Tuple tuple : s)
        {
            System.out.println("  " + tuple.getElement() + ", "
                    + tuple.getScore());
        }
        assert s.size() != 0;

        System.out.println(
                "We'll start a caching thread that will cache the data...");
        //缓存行清理
        CacheRowsThread thread = new CacheRowsThread();
        thread.start();

        Thread.sleep(1000);
        System.out.println("Our cached data looks like:");
        String r = conn.get("inv:itemX");
        System.out.println(r);
        assert r != null;
        System.out.println();

        System.out.println("We'll check again in 5 seconds...");
        Thread.sleep(5000);
        System.out.println("Notice that the data has changed...");
        String r2 = conn.get("inv:itemX");
        System.out.println(r2);
        System.out.println();
        assert r2 != null;
        assert !r.equals(r2);

        System.out.println("Let's force un-caching");
        scheduleRowCache(conn, "itemX", -1);
        Thread.sleep(1000);
        r = conn.get("inv:itemX");
        System.out.println("The cache was cleared? " + (r == null));
        assert r == null;

        thread.quit();
        Thread.sleep(2000);
        if (thread.isAlive())
        {
            throw new RuntimeException(
                    "The database caching thread is still alive?!?");
        }
    }

    public void testCacheRequest(Jedis conn)
    {
        System.out.println("\n----- testCacheRequest -----");
        String token = UUID.randomUUID().toString();

        Callback callback = new Callback()
        {
            public String call(String request)
            {
                return "content for " + request;
            }
        };
        //更新用户有关的信息
        updateToken(conn, token, "username", "itemX");
        String url = "http://test.com/?item=itemX";
        System.out.println(
                "We are going to cache a simple request against "
                        + url);
        //请求缓存页面，如果没有缓存，进行缓存（按照标准）
        String result = cacheRequest(conn, url, callback);
        System.out.println("We got initial content:\n" + result);
        System.out.println();

        assert result != null;

        System.out.println(
                "To test that we've cached the request, we'll pass a bad callback");
        String result2 = cacheRequest(conn, url, null);
        System.out.println("We ended up getting the same response!\n"
                + result2);

        assert result.equals(result2);

        assert !canCache(conn, "http://test.com/");
        assert !canCache(conn, "http://test.com/?item=itemX&_=1234536");
    }

    public String checkToken(Jedis conn, String token)
    {
        return conn.hget("login:", token);
    }

    //更新token
    public void updateToken(Jedis conn, String token, String user,
                            String item) {
        long timestamp = System.currentTimeMillis() / 1000;
        conn.hset("login:", token, user);
        conn.zadd("recent:", timestamp, token);
        if (item != null)
        {
            //添加用户浏览过的商品
            conn.zadd("viewed:" + token, timestamp, item);
            //只记录25个商品浏览记录
            conn.zremrangeByRank("viewed:" + token, 0, -26);
            //键为item的分值减一
            conn.zincrby("viewed:", -1, item);
        }
    }

    //购物车添加商品
    public void addToCart(Jedis conn, String session, String item,
                          int count)
    {
        if (count <= 0)
        {
            conn.hdel("cart:" + session, item);
        } else
        {
            conn.hset("cart:" + session, item,
                    String.valueOf(count));
        }
    }

    //缓存数据行
    public void scheduleRowCache(Jedis conn, String rowId, int delay)
    {
        conn.zadd("delay:", delay, rowId);
        conn.zadd("schedule:", System.currentTimeMillis() / 1000,
                rowId);
    }

    public String cacheRequest(Jedis conn, String request,
                               Callback callback)
    {
        //判断是否值得缓存
        if (!canCache(conn, request))
        {
            return callback != null ? callback.call(request) : null;
        }

        String pageKey = "cache:" + hashRequest(request);
        String content = conn.get(pageKey);//尝试查找是否有缓存

        if (content == null && callback != null)
        {
            content = callback.call(request);//如果没有缓存那么生成缓存
            conn.setex(pageKey, 300, content);//将content设置为pageKey的缓存，时间为300秒
        }

        return content;
    }

    //判断网页是否要缓存，判断的理由是view的数量是否排名靠前
    public boolean canCache(Jedis conn, String request)
    {
        try
        {
            URL url = new URL(request);
            HashMap<String, String> params = new HashMap<String, String>();
            if (url.getQuery() != null)
            {
                for (String param : url.getQuery().split("&"))
                {
                    String[] pair = param.split("=", 2);
                    params.put(pair[0], pair.length == 2
                            ? pair[1] : null);
                }
            }

            String itemId = extractItemId(params);
            if (itemId == null || isDynamic(params))
            {
                return false;
            }
            Long rank = conn.zrank("viewed:", itemId);
            return rank != null && rank < 10000;
        } catch (MalformedURLException mue)
        {
            return false;
        }
    }

    public boolean isDynamic(Map<String, String> params)
    {
        return params.containsKey("_");
    }

    public String extractItemId(Map<String, String> params)
    {
        return params.get("item");
    }

    public String hashRequest(String request)
    {
        return String.valueOf(request.hashCode());
    }

    public interface Callback
    {
        public String call(String request);
    }

    public class CleanSessionsThread extends Thread
    {
        private Jedis conn;
        private int limit;
        private boolean quit;

        public CleanSessionsThread(int limit)
        {
            this.conn = new Jedis("localhost");
            this.conn.select(15);
            this.limit = limit;
        }

        public void quit()
        {
            quit = true;
        }

        public void run()
        {
            while (!quit)
            {
                //返回该集合大小，如果超出总数就清理
                long size = conn.zcard("recent:");
                if (size <= limit)
                {
                    try
                    {
                        sleep(1000);
                    } catch (InterruptedException ie)
                    {
                        Thread.currentThread()
                                .interrupt();
                    }
                    continue;
                }
                //每次最多清理100个
                long endIndex = Math.min(size - limit, 100);
                Set<String> tokenSet = conn.zrange("recent:", 0,
                        endIndex - 1);
                String[] tokens = tokenSet.toArray(
                        new String[tokenSet.size()]);


                //为将要被删除的令牌构建键名
                ArrayList<String> sessionKeys = new ArrayList<String>();
                for (String token : tokens)
                {
                    sessionKeys.add("viewed:" + token);
                }


                conn.del(sessionKeys
                        .toArray(new String[sessionKeys
                                .size()]));
                conn.hdel("login:", tokens);
                conn.zrem("recent:", tokens);
            }
        }
    }

    public class CleanFullSessionsThread extends Thread
    {
        private Jedis conn;
        private int limit;
        private boolean quit;

        public CleanFullSessionsThread(int limit)
        {
            this.conn = new Jedis("localhost");
            this.conn.select(15);
            this.limit = limit;
        }

        public void quit()
        {
            quit = true;
        }

        public void run()
        {
            while (!quit)
            {
                long size = conn.zcard("recent:");
                if (size <= limit)
                {
                    try
                    {
                        sleep(1000);
                    } catch (InterruptedException ie)
                    {
                        Thread.currentThread()
                                .interrupt();
                    }
                    continue;
                }

                long endIndex = Math.min(size - limit, 100);
                Set<String> sessionSet = conn.zrange("recent:",
                        0, endIndex - 1);
                String[] sessions = sessionSet.toArray(
                        new String[sessionSet.size()]);

                ArrayList<String> sessionKeys = new ArrayList<String>();
                for (String sess : sessions)
                {
                    sessionKeys.add("viewed:" + sess);
                    sessionKeys.add("cart:" + sess);
                }

                conn.del(sessionKeys
                        .toArray(new String[sessionKeys
                                .size()]));
                conn.hdel("login:", sessions);
                conn.zrem("recent:", sessions);
            }
        }
    }

    public class CacheRowsThread extends Thread
    {
        private Jedis conn;
        private boolean quit;

        public CacheRowsThread()
        {
            this.conn = new Jedis("localhost");
            this.conn.select(15);
        }

        public void quit()
        {
            quit = true;
        }

        public void run()
        {
            Gson gson = new Gson();
            while (!quit)
            {
                Set<Tuple> range = conn.zrangeWithScores(
                        "schedule:", 0, 0);
                Tuple next = range.size() > 0
                        ? range.iterator().next()
                        : null;
                long now = System.currentTimeMillis() / 1000;
                if (next == null || next.getScore() > now)
                {
                    try
                    {
                        sleep(50);
                    } catch (InterruptedException ie)
                    {
                        Thread.currentThread()
                                .interrupt();
                    }
                    continue;
                }

                String rowId = next.getElement();
                double delay = conn.zscore("delay:", rowId);
                if (delay <= 0)
                {
                    conn.zrem("delay:", rowId);
                    conn.zrem("schedule:", rowId);
                    conn.del("inv:" + rowId);
                    continue;
                }

                Inventory row = Inventory.get(rowId);
                conn.zadd("schedule:", now + delay, rowId);
                conn.set("inv:" + rowId, gson.toJson(row));
            }
        }
    }

    public static class Inventory
    {
        private String id;
        private String data;
        private long time;

        private Inventory(String id)
        {
            this.id = id;
            this.data = "data to cache...";
            this.time = System.currentTimeMillis() / 1000;
        }

        public static Inventory get(String id)
        {
            return new Inventory(id);
        }
    }
}

```

## Redis命令

### 字符串

字符串可以存储三种类型：

> 字符串
>
> 整数
>
> 浮点数

常用的自增自减命令：

| 命令              | 描述          |
| ----------------- | ------------- |
| INCR name         | 键值+1        |
| DECR name         | 键值-1        |
| INCRBY name amout | +amout        |
| DECRBY name amout | -amout        |
| INCRBYFLOAT       | +浮点数amount |

字符串的相关操作：

| 命令                        | 描述                                                     |
| --------------------------- | -------------------------------------------------------- |
| APPEND name value           | 追加拼接                                                 |
| GETRANGE name start end     | 取子串                                                   |
| SETRANGE name offset value  | 设定子串值                                               |
| GETBIT name offset          | 返回指定偏移量的二进制位                                 |
| SETBIT name offset value    | 设置指定偏移量的位                                       |
| BITCOUNT name [start end]   | 统计串中1的二进制位的数量                                |
| BITOP op dest name [name …] | 对一个或多个二进制位串执行按位的逻辑运算，结果放入dest中 |

注意：

> SETRANGE:如果当前字符串的长度不够写入的话，会自动填充null字节
>
> SETBIT:同上
>
> GETRANGE:如果读取的位置超出末尾，超出的数据会被认为是空串
>
> GETBIT:超出的部分会被认为是0
>
> R存储的二进制位是按照偏移量从高到低排列的，也就是二进制数33，依次存储为：0010 0001

### LIST

| 命令                      | 描述                     |
| ------------------------- | ------------------------ |
| RPUSH name value [value…] | 将一个或多个值推入到右端 |
| LPUSH name value [value…] | 与上类似                 |
| RPOP name                 | 移除最右端元素           |
| LPOP name                 | 与上类似                 |
| LINDEX name offset        | 返回指定偏移量的元素     |
| LRANGE name start end     | 范围指定区间的元素       |
| LTRIM name start end      | 只保留区间内的元素       |

高级命令：

| 命令                           | 描述                                                         |
| ------------------------------ | ------------------------------------------------------------ |
| BLPOP name [name …] timeout    | 弹出非空列表最左端的元素，或者空列表进行阻塞，等待元素出现然后弹出，阻塞有超时时间 |
| BRPOP name [name…] timeout     | 与上类似                                                     |
| RPOPLPUSH source dest          | 从source中弹出最右端元素，推入dest列表的最左端，并返回该元素 |
| BRPOPLPUSH source dest timeout | 与上类似，如果source为空则阻塞等待                           |

阻塞命令常用在消息传递和任务队列

### 集合

| 命令                   | 描述                                                   |
| ---------------------- | ------------------------------------------------------ |
| SADD name item [item…] | 添加元素到集合                                         |
| SREM name item [item…] | 移除元素                                               |
| SISMEMBER name item    | 检查元素是否存在                                       |
| SCARD name             | 返回元素数量                                           |
| SRANDMEMBER name count | 随机返回一个或者而多个元素,count为负数时，元素可以重复 |
| SMEMBERS name          | 返回集合包含的所有元素                                 |
| SPOP name              | 随机的移除一个元素                                     |
| SMOVE source dest item | 从source中移除item并添加到dest中                       |

多个集合的操作:

| 命令                          | 描述                                          |
| ----------------------------- | --------------------------------------------- |
| SDIFF name [name…]            | 返回存在于第一个集合,但不存在于其他集合的元素 |
| SDIFFSTORE dest name [name]   | 与上类似,只不过结果存于dest中                 |
| SINTER name [name…]           | 返回那些同时存在于所有集合中的元素            |
| SINTERSTORE dest name [name…] | 与上类似,结果存于dest                         |
| SUNION name [name…]           | 返回那些至少存在于一个集合中的元素            |
| SUNIONSTORE dest name [name…] | 与上类似,结果存于dest                         |

### 散列

| 命令                             | 描述                           |
| -------------------------------- | ------------------------------ |
| HMGET name key [key…]            | 从散列里面获取一个或多个键的值 |
| HMSET name key value [key value] | 设置键值对                     |
| HDEL name key [key…]             | 删除键值对                     |
| HLEN name                        | 返回散列包含的键值对数量       |
| HMEXISTS name key                | 检查给定key是否在散列中        |
| HKEYS name                       | 获取散列包含的所有键           |
| HVALS name                       | 获取散列包含的所有键值对       |
| HINCRBY name key incr            | 将key的值加上整数incr          |
| HINCRBYFLOAT name key incr       | 与上类似,只不过加的是浮点数    |

### 有序集合

| 命令                                                         | 描述                            |
| ------------------------------------------------------------ | ------------------------------- |
| ZADD name score member [score member…]                       | 新增成员                        |
| ZREM name member [member…]                                   | 移除成员                        |
| ZCARD name                                                   | 返回成员数量                    |
| ZINCRBY name incr member                                     | 分值增加incr                    |
| ZCOUNT name min max                                          | 分值介于某个区间的成员数量      |
| ZRANK name member                                            | 返回member在有序集合中的排名    |
| ZSCORE name member                                           | 返回member的分支                |
| ZRANGE name start stop [WITHSCORES]                          | 返回介于该区间的成员,并返回分值 |
| ZREVRANK name member                                         | 返回成员的排名,从大到小排       |
| ZREVRANGE name start stop [WITHSCORES]                       | 返回区间内的成员,从大到小排列   |
| ZRANGEBYSCORE key min max [WITHSCORES] [LIMIT offset count]  | 返回某个区间的成员              |
| ZREVRANGEBYSCORE key min max [WITHSCORES] [LIMIT offset count] | 与上类似,分支按照从大到小排列   |
| ZREMRANGEBYRANK name start stop                              | 移除区间的成员                  |
| ZREMRANGEBYSCORE name min max                                | 移除区间的成员                  |

我们可以把集合作为输入传给ZINTERSTORE和ZUNIONSTORE,命令会将集合看成成员分值全为1的有序集合

交集和并集运算默认的聚合函数是sum,也可以指定聚合函数

### 发布与订阅

订阅者负责订阅频道,发送者负责向频道发送二进制字符串消息

| 命令                               | 详解                                                         |
| ---------------------------------- | ------------------------------------------------------------ |
| SUBSCRIBE channel [channel …]      | 订阅给定的一个或者多个频道                                   |
| UNSUBSCRIBE [channel [channel…]]   | 退订给定的一个或多个频道,如果执行时没有给定任何频道,退订所有频道 |
| PUBLISH channle message            | 给频道发送消息                                               |
| PSUBSCRIBE pattern [patterm …]     | 订阅与给定模式相匹配的所有频道                               |
| PUNSUBSCRIBE [pattern [pattern …]] | 退订给定的模式,如果执行没有给定任何模式,退订所有模式         |

旧版的redis会挤压消息导致速度变慢。同时如果客户端在订阅期间断线，将会丢失发送的信息。总之，Redis的下消息订阅不是完全可靠的

### 其他命令

#### 排序命令

使用SORT命令进行排序，负责执行排序操作的SORT命令可以根据字符串、列表、集合、有序集合、散列这5种键里面存储的数据，对列表、集合以及有序集合进行排序。

```
SORT source [BY pattern] [LIMIT offset count] [GET pattern [GET pattern ...]] [ASC|DESC] [ALPHA][STORE dest]
其中ALPHA表示字母表
```

#### Redis的事务

R有5种命令可以让用户在不被打断的情况下对多个键进行操作:

> WATCH MULTI EXEC UNWATCH DISCARD

Redis的基本事务需要用到MULTI命令和EXEC命令，可以在不被打断的情况下执行多个命令

执行事务的时候，我们需要先执行WATCH命令，然后再执行其他命令，最后再执行EXEC命令。这些命令会放入一个队里里面，在执行EXEC后会执行队列里面的命令

#### 键的过期时间

R会对过期的键进行删除，但是没有办法对集合键中的单个元素设置过期时间

| 命令                           | 描述                                               |
| ------------------------------ | -------------------------------------------------- |
| PERSIST name                   | 移除过期时间                                       |
| TTL name                       | 查看距离过期还有多少秒                             |
| EXPIRE name seconds            | 指定描述后过期                                     |
| EXPIREAT name timestamp        | 将给定键的过期时间设置为UNIX时间戳                 |
| PTTL name                      | 查看离过期时间还有多少毫秒                         |
| PEXPIRE name mills             | 指定多少毫秒后过期                                 |
| PEXPIREAT name timestamp-mills | 将一个毫秒精度的UNIX时间戳设置为给定的键的过期时间 |

## 数据安全与性能保障

### 持久化选项

R提供了两种不同的持久化方法来讲数据存储到硬盘里面：

> 快照：将某一时刻的所有数据都写入硬盘里面
>
> 只追加文件（AOF）：将执行的命令复制到硬盘里面

这两种方法既可以同时使用，又可以单独使用

持久化配置样例：

> 快照持久化选项：
>
> save 60 1000 //当60s之内有1000次条件满足后，就会触发BGSAVE命令，多个配置时，只要一个满足就会触发
>
> stop-writes-on-bgseave-error no
>
> rdbcompression yes
>
> dbfilename dump.rdb
>
> AOF持久化：
>
> appendonly on
>
> appendfsync everysec
>
> no-appendfsync-on-rewrite no
>
> //下述两个命令满足，执行BGREWRITEAOF命令
>
> auto-aof-rewrite-percentage 100 //当AOF文件的体积比上一次重写之后的体积大了至少一倍，
>
> auto-aof-rewrite-min-size 64mb //当AOF文件>64Mb
>
> dir ./

### 快照持久化

使用快照持久化，如果发生系统崩溃，那么R将丢失最近一次创建快照之后写入的数据

创建快照的办法：

>  使用BGSAVE命令，会创建子进程写入
> 也可以使用SAVE命令来创建一个快照，接到SAVE命令的R服务器在快照创建完毕之前将不再响应任何其他命令
> 当R通过shutdown命令关闭服务器的请求时，或者接收到标准TERM信息时，会执行SAVE命令后再关闭
> 当一个R服务器向另一个服务器发送SYNC命令来开始一次复制操作时，如果主服务器目前没有在执行BGSAVE操作，或者主服务器并非刚刚执行完BGSAVE操作，那么主服务器会执行BGSAVE命令

当数据量很大时，使用BGSAVE创建子进程会耗时会很长，这时候可以考虑在空闲期使用SAVE命令节省时间

### AOF持久化

AOF持久化将被执行的命令写到AOF文件的末尾，以此来记录数据发生的变化。AOF持久化通过appendfsync命令开启，该命令选项：

> always 每个命令都要同步写入硬盘，这样会严重降低R的速度
>
> everysec:每秒执行一次同步，推荐使用，系统崩溃最多丢失一秒的数据
>
> no：让系统来决定何时进行同步

#### 重写/压缩AOF文件

AOF文件的缺点是文件会不断增加，导致占用空间较多，解决办法：

> 使用BGREWRITEAOF命令，这个命令会移除冗余命令来重写AOF文件，该命令会创建子线程。
>
> 对应配置文件中的命令为auto-aof-rewrite-min-size

### 复制

复制是指不同服务器之间的数据复制，这些服务器按彼此之间的关系可以分为主服务器和从服务器。客户端每次向主服务器写入时，从服务器都会实时得到更新

1. 当从服务器连接主服务器时，主服务器会执行BGSAVE命令，因此配置文件中要配置dir和dbfilename选项
2. 如果配置文件中有slaveof host port，那么R服务器将根据给定的IP和端口来连接主服务器
3. 可以通过SLAVEOF no one来终止复制操作
4. 也可以通过SLAVEOF host port命令来让服务器开始复制一个新的主服务器
5. 主服务器可以向运行中的服务器发送SLAVEOF命令将其设置为从服务器
6. R不支持主主复制

#### Redis复制的启动过程

> 1. 从服务器连接主服务器会发送SYNC命令
> 2. 主服务器接收到命令开始执行BGSAVE，执行完后向从服务器发送快照
> 3. 从服务器丢弃旧数据，开始载入快照
> 4. 快照载入完后，从服务器将会接收主服务器发送的每一个写命令

#### 主从链

从服务器也可以有自己的从服务器，因此可以形成主从链，如果一个从服务器在执行复制时，它将会断开它与它的从服务器之间的连接，导致其从服务器需要重新连接并重新同步。当存在多个从服务器时，可能无法同时快速更新所有的从服务器，或者从新连接和从新同步导致系统超载，为了缓解这个问题，用户可以创建一个由R主从节点组成的中间层来分担主服务器的复制工作，形成了一个树状的结构

#### 检查硬盘是否写入

为了验证主服务器是否将写命令发送到从服务器，用户需要在想主服务器写入真正的数据之后，再向主服务器写入一个唯一的虚拟值，通过在从服务器判断该虚拟值存不存在从而判断是否已经将命令发送到从服务器

同时可以通过检查INFO命令的输出结果中aof_pending_bio_fsync属性值是否为0来判断服务器是否将已知的所有数据都保存到硬盘中了

### 处理系统故障

#### 验证快照文件和AOF文件

Redis提供了redis-check-aof命令来验证AOF文件以及redis-check-dump命令来验证快照文件

redis-check-aof提供了一个–fix参数用来修复AOF文件，当发现文件命令有错误或者不完整，将会丢弃出错命令之后的所有命令。快照不支持修复

#### 更换故障主服务器

假设A和B分别是主从服务器，更换A的一般步骤：

> 首先向B发送一个SAVE命令创建快照，命令完成后使用QUIT退出客户端
>
> 接着将快照文件发送到C服务器，并在C上启动R
>
> 在C上使用SLAVEOF命令让B成为从服务器

另一种方法：

> 将从服务器升级为主服务器(turn)，并为升级后的主服务器创建从服务器

另外，使用Redis Sentinel可以监视指定的R主服务器及其属下的从服务器器，并能进行自动故障转移

### Redis事务

前面我们知道R的事务以MULTI开始，以EXEC结束，但是R的事务并不是和普通关系数据库一样，它会将所有的命令累积到最后EXEC执行后才会以流水线的形式执行，这会导致两个问题：

> 事务中不能根据读取到的数据决定下一步操作，也就是无法以一致的形式读取数据
>
> 在多个事务同时处理同一个对象时使用的二阶提交无法进行

解决的办法，使用WATCH命令：

> WATCH：该命令可以对键进行监视，从监视之后直到用户执行EXEC命令的这段时间内，如果其他客户端抢先对任何被监视的键进行了替换、更新和删除等操作，那么当用户执行EXEC命令时会出错
>
> UNWATCH：用在WATCH之后，MULTI执行之前，用来取消键的监视
>
> DISCARD：在EXEC之前运行，可以对连接重置，即取消WATCH，并清空命令队列里的命令

实际上使用WATCH是一种乐观锁策略，传统的关系型数据库使用的是悲观锁

### 非事务型流水线

使用流水线将多个命令一同执行可以使省略不必要的通信时间，但是由于事务又有其他的消耗，因此R支持了一种非事务型流水线，通过向pipeline传递一个false参数来提升性能

### 关于性能的注意事项

1. 可以通过redis-benchmark来测试服务器R的性能，默认采用50个客户端测试，可以使用 “-c 1 -q”来使用一个客户端进行测试，其中-q用来简化输出
2. 正常使用中单个客户端只能达到测试的50%-60%的性能
3. 如果性能只有benchmark的20%，或者客户端返回:Cannot assign requested address错误原因可能是每个命令或者每组命令都创建了新的连接

### 案例：

商品的买入和卖出案例：

1. 使用inventory:用户ID的set存放用户拥有的商品
2. 使用markte:的zset存放卖的商品，其中key是item名+卖家，值是价格
3. 使用user:用户ID的hash存放用户信息

```java
public class Chapter04 {
    public static final void main(String[] args) {
        new Chapter04().run();
    }

    public void run() {
        Jedis conn = new Jedis("localhost");
        conn.select(15);
        //测试添加商品到市场
        testListItem(conn, false);
        //测试购买商品
        testPurchaseItem(conn);
        //benchmark案例，和上面二者无关
        testBenchmarkUpdateToken(conn);
    }

    public void testListItem(Jedis conn, boolean nested) {
        if (!nested){
            System.out.println("\n----- testListItem -----");
        }

        System.out.println("We need to set up just enough state so that a user can list an item");
        String seller = "userX";
        String item = "itemX";
        //卖家添加物品
        conn.sadd("inventory:" + seller, item);
        //打印物品
        Set<String> i = conn.smembers("inventory:" + seller);

        System.out.println("The user's inventory has:");
        for (String member : i){
            System.out.println("  " + member);
        }
        assert i.size() > 0;
        System.out.println();

        System.out.println("Listing the item...");
        //模拟商品添加到市场
        boolean l = listItem(conn, item, seller, 10);
        System.out.println("Listing the item succeeded? " + l);
        assert l;
        //打印出市场中的商品信息
        Set<Tuple> r = conn.zrangeWithScores("market:", 0, -1);
        System.out.println("The market contains:");
        for (Tuple tuple : r){
            System.out.println("  " + tuple.getElement() + ", " + tuple.getScore());
        }
        assert r.size() > 0;
    }

    public void testPurchaseItem(Jedis conn) {
        System.out.println("\n----- testPurchaseItem -----");
        testListItem(conn, true);

        System.out.println("We need to set up just enough state so a user can buy an item");
        conn.hset("users:userY", "funds", "125");
        Map<String,String> r = conn.hgetAll("users:userY");
        System.out.println("The user has some money:");
        for (Map.Entry<String,String> entry : r.entrySet()){
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }
        assert r.size() > 0;
        assert r.get("funds") != null;
        System.out.println();

        System.out.println("Let's purchase an item");
        boolean p = purchaseItem(conn, "userY", "itemX", "userX", 10);
        System.out.println("Purchasing an item succeeded? " + p);
        assert p;
        r = conn.hgetAll("users:userY");
        System.out.println("Their money is now:");
        for (Map.Entry<String,String> entry : r.entrySet()){
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }
        assert r.size() > 0;

        String buyer = "userY";
        Set<String> i = conn.smembers("inventory:" + buyer);
        System.out.println("Their inventory is now:");
        for (String member : i){
            System.out.println("  " + member);
        }
        assert i.size() > 0;
        assert i.contains("itemX");
        assert conn.zscore("market:", "itemX.userX") == null;
    }

    public void testBenchmarkUpdateToken(Jedis conn) {
        System.out.println("\n----- testBenchmarkUpdate -----");
        benchmarkUpdateToken(conn, 5);
    }

    //模拟卖家将商品放入市场
    public boolean listItem(
            Jedis conn, String itemId, String sellerId, double price) {

        String inventory = "inventory:" + sellerId;
        String item = itemId + '.' + sellerId;
        long end = System.currentTimeMillis() + 5000;

        while (System.currentTimeMillis() < end) {
            //监视物品
            conn.watch(inventory);
            if (!conn.sismember(inventory, itemId)){
                conn.unwatch();
                return false;
            }

            Transaction trans = conn.multi();
            trans.zadd("market:", price, item);
            trans.srem(inventory, itemId);
            List<Object> results = trans.exec();
            // null response indicates that the transaction was aborted due to
            // the watched key changing.
            if (results == null){
                continue;
            }
            return true;
        }
        return false;
    }

    public boolean purchaseItem(
            Jedis conn, String buyerId, String itemId, String sellerId, double lprice) {

        String buyer = "users:" + buyerId;
        String seller = "users:" + sellerId;
        String item = itemId + '.' + sellerId;
        String inventory = "inventory:" + buyerId;
        long end = System.currentTimeMillis() + 10000;

        while (System.currentTimeMillis() < end){
            conn.watch("market:", buyer);

            double price = conn.zscore("market:", item);
            double funds = Double.parseDouble(conn.hget(buyer, "funds"));
            if (price != lprice || price > funds){
                conn.unwatch();
                return false;
            }

            Transaction trans = conn.multi();
            trans.hincrBy(seller, "funds", (int)price);
            trans.hincrBy(buyer, "funds", (int)-price);
            trans.sadd(inventory, itemId);
            trans.zrem("market:", item);
            List<Object> results = trans.exec();
            // null response indicates that the transaction was aborted due to
            // the watched key changing.
            if (results == null){
                continue;
            }
            return true;
        }

        return false;
    }

    public void benchmarkUpdateToken(Jedis conn, int duration) {
        try{
            @SuppressWarnings("rawtypes")
            Class[] args = new Class[]{
                    Jedis.class, String.class, String.class, String.class};
            Method[] methods = new Method[]{
                    this.getClass().getDeclaredMethod("updateToken", args),
                    this.getClass().getDeclaredMethod("updateTokenPipeline", args),
            };
            for (Method method : methods){
                int count = 0;
                long start = System.currentTimeMillis();
                long end = start + (duration * 1000);
                while (System.currentTimeMillis() < end){
                    count++;
                    method.invoke(this, conn, "token", "user", "item");
                }
                long delta = System.currentTimeMillis() - start;
                System.out.println(
                        method.getName() + ' ' +
                                count + ' ' +
                                (delta / 1000) + ' ' +
                                (count / (delta / 1000)));
            }
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    public void updateToken(Jedis conn, String token, String user, String item) {
        long timestamp = System.currentTimeMillis() / 1000;
        conn.hset("login:", token, user);
        conn.zadd("recent:", timestamp, token);
        if (item != null) {
            conn.zadd("viewed:" + token, timestamp, item);
            conn.zremrangeByRank("viewed:" + token, 0, -26);
            conn.zincrby("viewed:", -1, item);
        }
    }

    public void updateTokenPipeline(Jedis conn, String token, String user, String item) {
        long timestamp = System.currentTimeMillis() / 1000;
        Pipeline pipe = conn.pipelined();
        pipe.multi();
        pipe.hset("login:", token, user);
        pipe.zadd("recent:", timestamp, token);
        if (item != null){
            pipe.zadd("viewed:" + token, timestamp, item);
            pipe.zremrangeByRank("viewed:" + token, 0, -26);
            pipe.zincrby("viewed:", -1, item);
        }
        pipe.exec();
    }
}
```

## 使用Redis构建应用程序组件

### 自动补全联系人：

案例介绍与实现：

1. 模拟输入框弹出最近联系人的功能，
2. 采用一个队列存储联系人

```java
//采用先删除后添加的策略，从而保证是最近出现的联系人
public void addUpdateContact(Jedis conn, String user, String contact) {
    String acList = "recent:" + user;
    Transaction trans = conn.multi();
    trans.lrem(acList, 0, contact);
    trans.lpush(acList, contact);
    trans.ltrim(acList, 0, 99);
    trans.exec();
}
//搜索匹配的联系人
public List<String> fetchAutocompleteList(Jedis conn, String user, String prefix) {
    List<String> candidates = conn.lrange("recent:" + user, 0, -1);
    List<String> matches = new ArrayList<String>();
    for (String candidate : candidates) {
        if (candidate.toLowerCase().startsWith(prefix)){
            matches.add(candidate);
        }
    }
    return matches;
}
```

#### 通讯录自动补全

上面的案例采用列表的方式，并且匹配搜索是通过程序来实现的，因此不适合队列比较长的情况

这里采用一个千人公会的联系人自动补全来模拟这种场景：

1. 采用zset来存储联系人，并设置分数为0。这是R的特性：有序集合的分数都相同，将会根据成员的名称排序，如果有序集合的分数都是0，但会根据名称的二进制排序
2. 这里名称都是英文，因此可以在集合里面插入要搜索范围的前驱元素和后驱元素，然后查找中间的元素就能完成搜索。前驱是前缀字符+{，后驱是后缀+{
3. 为了防止并发插入而造成误删，这里会在前驱和后驱字符加上一个uuid

```java
private static final String VALID_CHARACTERS = "`abcdefghijklmnopqrstuvwxyz{";
public String[] findPrefixRange(String prefix) {
    int posn = VALID_CHARACTERS.indexOf(prefix.charAt(prefix.length() - 1));
    char suffix = VALID_CHARACTERS.charAt(posn > 0 ? posn - 1 : 0);
    String start = prefix.substring(0, prefix.length() - 1) + suffix + '{';
    String end = prefix + '{';
    return new String[]{start, end};
}
public Set<String> autocompleteOnPrefix(Jedis conn, String guild, String prefix) {
    String[] range = findPrefixRange(prefix);
    String start = range[0];
    String end = range[1];
    String identifier = UUID.randomUUID().toString();
    start += identifier;
    end += identifier;
    String zsetName = "members:" + guild;

    conn.zadd(zsetName, 0, start);
    conn.zadd(zsetName, 0, end);

    Set<String> items = null;
    while (true){
        conn.watch(zsetName);
        int sindex = conn.zrank(zsetName, start).intValue();
        int eindex = conn.zrank(zsetName, end).intValue();
        int erange = Math.min(sindex + 9, eindex - 2);

        Transaction trans = conn.multi();
        trans.zrem(zsetName, start);
        trans.zrem(zsetName, end);
        trans.zrange(zsetName, sindex, erange);
        List<Object> results = trans.exec();
        if (results != null){
            items = (Set<String>)results.get(results.size() - 1);
            break;
        }
    }

    for (Iterator<String> iterator = items.iterator(); iterator.hasNext(); ){
        if (iterator.next().indexOf('{') != -1){
            iterator.remove();
        }
    }
    return items;
}
```

### 分布式锁

分布式锁是由不同机器上的不同R客户端进行获取和释放的。前面我们的交易案例中，使用WATCH来检视市场的变化，从而实现买卖行为能够正确的实时，但是使用WATCH有一个问题，就是整个市场都会被监视，导致市场的任何变更都会导致买卖行为重试。本节采用SETNX命令来优化这个问题，实现分布式锁功能。

核心思想：使用SETXN来设置锁的值，从而表示当前锁已经被占用

获取锁的代码如下:

```java
//获取锁
public String acquireLockWithTimeout(
  Jedis conn, String lockName, long acquireTimeout, long lockTimeout)
{
  String identifier = UUID.randomUUID().toString();
  String lockKey = "lock:" + lockName;
  int lockExpire = (int)(lockTimeout / 1000);

  long end = System.currentTimeMillis() + acquireTimeout;
  while (System.currentTimeMillis() < end) {
    //给锁设置过期时间
    if (conn.setnx(lockKey, identifier) == 1){
      conn.expire(lockKey, lockExpire);
      return identifier;
    }
    //防止系统崩溃导致没有设置超时时间成功
    if (conn.ttl(lockKey) == -1) {
      conn.expire(lockKey, lockExpire);
    }
    try {
      Thread.sleep(1);
    }catch(InterruptedException ie){
      Thread.currentThread().interrupt();
    }
  }

  // null indicates that the lock was not acquired
  return null;
}
```

释放锁的代码如下：

```java
public boolean releaseLock(Jedis conn, String lockName, String identifier) {
  String lockKey = "lock:" + lockName;
  //这里采用循环是因为锁有超时时间，不会死循环
  while (true){
    //检查锁是否被变更
    conn.watch(lockKey);
    //检查是否是自己加的锁
    if (identifier.equals(conn.get(lockKey))){
      Transaction trans = conn.multi();
      trans.del(lockKey);
      List<Object> results = trans.exec();
      //键发生变化会导致结果为null
      if (results == null){
        continue;
      }
      return true;
    }

    conn.unwatch();
    break;
  }

  return false;
}
```

### 计数信号量

信号量是一种锁，它可以让用户限制一项资源最多能够同时被多少个进程访问。上面的锁可以看成是只能被一个进程访问的信号量。

```java
public String acquireFairSemaphore(
  Jedis conn, String semname, int limit, long timeout)
{
  //生成每个访问进程的UUID
  String identifier = UUID.randomUUID().toString();
  //使用两个有序集合，owner表示进程的拥有者，分值是模拟的计数器值
  //使用自定的计数器而不是采用系统时间的原因是防止各个机器上的时间有区别
  //counter是计数器，用来区分哪个进程先获取信号量
  String czset = semname + ":owner";
  String ctr = semname + ":counter";

  long now = System.currentTimeMillis();
  Transaction trans = conn.multi();
  //删除超时的信号量,-inf表示负无穷
  trans.zremrangeByScore(
    semname.getBytes(),
    "-inf".getBytes(),
    String.valueOf(now - timeout).getBytes());
  ZParams params = new ZParams();
  params.weightsByDouble(1.0d,0d);
  //取交集，czset和semname的权重分别是1,0,作用是在owner中删除过期的键
  trans.zinterstore(czset, params, czset, semname);
  //计数器自增
  trans.incr(ctr);
  List<Object> results = trans.exec();
  //获取计数器自增后的值
  int counter = ((Long)results.get(results.size() - 1)).intValue();
  //更新相关数据库
  trans = conn.multi();
  trans.zadd(semname, now, identifier);
  trans.zadd(czset, counter, identifier);
  trans.zrank(czset, identifier);
  results = trans.exec();
  //这里如果产生竞争条件，也就是A先自增，但是B先走到这处，A就会”偷走“B的信号量，因为B是比A更优先的
  int result = ((Long)results.get(results.size() - 1)).intValue();
  if (result < limit){
    return identifier;
  }
  //如果队列已满，则清理无用数据
  trans = conn.multi();
  trans.zrem(semname, identifier);
  trans.zrem(czset, identifier);
  trans.exec();
  return null;
}
```

释放锁的代码：

```java
//释放锁
public boolean releaseFairSemaphore(
  Jedis conn, String semname, String identifier) {
  Transaction trans = conn.multi();
  trans.zrem(semname, identifier);
  trans.zrem(semname + ":owner", identifier);
  List<Object> results = trans.exec();
  return (Long)results.get(results.size() - 1) == 1;
}
```

#### 消除竞争条件

前面介绍的锁和信号量都会在竞争条件下产生逻辑问题，比如分配比限制更多的信号量，解决的办法就是对获取信号量的方法进行加锁处理。

### 任务队里的实现

### 任务队列

队列的实现较为简单

#### 先进先出队列：

> 用队列实现，实用rpush和blpop实现

#### 多个可执行任务

> 给任务分类型，实用回调函数处理不同类型的任务

#### 任务优先级

> 实用blpop命令，传入优先级不同的多个队列，将会阻塞弹出第一个非空队列中的内容

#### 延迟任务

> 实用zset存储任务，分值为执行时间，通过一两个线程不断循环判断当前任务是否要执行

```java
//添加任务，区分延时任务和普通任务
public String executeLater(
  Jedis conn, String queue, String name, List<String> args, long delay) {
  Gson gson = new Gson();
  String identifier = UUID.randomUUID().toString();
  String itemArgs = gson.toJson(args);
  String item = gson.toJson(new String[]{identifier, queue, name, itemArgs});
  if (delay > 0){
    conn.zadd("delayed:", System.currentTimeMillis() + delay, item);
  } else {
    conn.rpush("queue:" + queue, item);
  }
  return identifier;
}
```

扫描线程代码如下:

```java
//扫描线程
public class PollQueueThread
  extends Thread {
  private Jedis conn;
  private boolean quit;
  private Gson gson = new Gson();

  public PollQueueThread(){
    this.conn = new Jedis("localhost");
    this.conn.select(15);
  }

  public void quit() {
    quit = true;
  }

  public void run() {
    while (!quit){
      //轮询扫描
      Set<Tuple> items = conn.zrangeWithScores("delayed:", 0, 0);
      Tuple item = items.size() > 0 ? items.iterator().next() : null;
      if (item == null || item.getScore() > System.currentTimeMillis()) {
        try{
          sleep(10);
        }catch(InterruptedException ie){
          Thread.interrupted();
        }
        continue;
      }

      String json = item.getElement();
      String[] values = gson.fromJson(json, String[].class);
      String identifier = values[0];
      String queue = values[1];
      //延迟任务入队的时候要使用锁
      String locked = acquireLock(conn, identifier);
      if (locked == null){
        continue;
      }
      if (conn.zrem("delayed:", json) == 1){
        conn.rpush("queue:" + queue, json);
      }

      releaseLock(conn, identifier, locked);
    }
  }
}
```

### 消息拉取

模拟聊天室的消息接受和发送

> 主要有三个数据结构：
>
> 聊天室的有序集合存的是用户以及用户已经阅读的消息计数器（消息id）
>
> 用户的有序集合存的是聊天室以及已经阅读过得消息id
>
> 消息的有序结合存储消息内容和消息的id

创建聊天室：

```java
//初始化聊天室
public String createChat(Jedis conn, String sender, Set<String> recipients, String message) {
  //每个聊天室都生成一个id
  String chatId = String.valueOf(conn.incr("ids:chat:"));
  return createChat(conn, sender, recipients, message, chatId);
}
//创建聊天室
public String createChat(
  Jedis conn, String sender, Set<String> recipients, String message, String chatId) {
  recipients.add(sender);

  Transaction trans = conn.multi();
  for (String recipient : recipients){
    //分别更新聊天室和用户的有序集合
    //对于聊天室，成员是用户id，分值为0
    //对于用户，成员是聊天室id，分值为0
    trans.zadd("chat:" + chatId, 0, recipient);
    trans.zadd("seen:" + recipient, 0, chatId);
  }
  trans.exec();
  return sendMessage(conn, chatId, sender, message);
}
//发送消息
public String sendMessage(Jedis conn, String chatId, String sender, String message) {
  //当使用某个键的值去更新另一个键的值时，需要通过锁同步
  String identifier = acquireLock(conn, "chat:" + chatId);
  if (identifier == null){
    throw new RuntimeException("Couldn't get the lock");
  }
  try {
    //消息ID自增
    long messageId = conn.incr("ids:" + chatId);
    HashMap<String,Object> values = new HashMap<String,Object>();
    values.put("id", messageId);
    values.put("ts", System.currentTimeMillis());
    values.put("sender", sender);
    values.put("message", message);
    String packed = new Gson().toJson(values);
    //存储消息
    conn.zadd("msgs:" + chatId, messageId, packed);
  }finally{
    releaseLock(conn, "chat:" + chatId, identifier);
  }
  return chatId;
}
```

获取消息：

```java
//查找为接收到的
public List<ChatMessages> fetchPendingMessages(Jedis conn, String recipient) {
  Set<Tuple> seenSet = conn.zrangeWithScores("seen:" + recipient, 0, -1);
  List<Tuple> seenList = new ArrayList<Tuple>(seenSet);
  //遍历接收者信息，找出他们在每个聊天室里未接受的消息
  Transaction trans = conn.multi();
  for (Tuple tuple : seenList){
    String chatId = tuple.getElement();
    int seenId = (int)tuple.getScore();
    trans.zrangeByScore("msgs:" + chatId, String.valueOf(seenId + 1), "inf");
  }
  List<Object> results = trans.exec();

  Gson gson = new Gson();
  Iterator<Tuple> seenIterator = seenList.iterator();
  Iterator<Object> resultsIterator = results.iterator();

  List<ChatMessages> chatMessages = new ArrayList<ChatMessages>();
  List<Object[]> seenUpdates = new ArrayList<Object[]>();
  List<Object[]> msgRemoves = new ArrayList<Object[]>();
  //遍历接收者的聊天室相关信息记录，找出未读信息
  while (seenIterator.hasNext()){
    Tuple seen = seenIterator.next();
    Set<String> messageStrings = (Set<String>)resultsIterator.next();
    if (messageStrings.size() == 0){
      continue;
    }

    int seenId = 0;
    String chatId = seen.getElement();
    List<Map<String,Object>> messages = new ArrayList<Map<String,Object>>();
    for (String messageJson : messageStrings){
      Map<String,Object> message = (Map<String,Object>)gson.fromJson(
        messageJson, new TypeToken<Map<String,Object>>(){}.getType());
      int messageId = ((Double)message.get("id")).intValue();
      if (messageId > seenId){
        seenId = messageId;
      }
      //收集未读消息
      message.put("id", messageId);
      messages.add(message);
    }
    //该聊天室添加接收者
    conn.zadd(":" + chatId, seenId, recipient);
    //用于更新接受者的信息
    seenUpdates.add(new Object[]{"seen:" + recipient, seenId, chatId});
    //找出所有人都阅读过的消息，实际就是第一个消息，并移除
    Set<Tuple> minIdSet = conn.zrangeWithScores("chat:" + chatId, 0, 0);
    if (minIdSet.size() > 0){
      msgRemoves.add(new Object[]{
        "msgs:" + chatId, minIdSet.iterator().next().getScore()});
    }
    chatMessages.add(new ChatMessages(chatId, messages));
  }

  trans = conn.multi();
  //更新已读消息记录
  for (Object[] seenUpdate : seenUpdates){
    trans.zadd(
      (String)seenUpdate[0],
      (Integer)seenUpdate[1],
      (String)seenUpdate[2]);
  }
  //移除消息
  for (Object[] msgRemove : msgRemoves){
    trans.zremrangeByScore(
      (String)msgRemove[0], 0, ((Double)msgRemove[1]).intValue());
  }
  trans.exec();

  return chatMessages;
}
```

## 基于搜索的应用程序

#### 文档搜索

模拟根据关键词搜索相关的文档，实现细节：

> 1. 根据文档建立反向索引，首先收集文档中的词汇，过滤一些不必要的词汇，然后建立反向索引
> 2. 搜索支持使用+号进行同义词搜索，使用-进行排除搜索,用户可以输入命令，代码中会解析（省略）
> 3. 支持搜索结果按照时间以及文章评排序

解析文章并建立索引的代码：

```java
//构建支持搜索的词汇的匹配规则，规定字符个数大于2
private static final Pattern WORDS_RE = Pattern.compile("[a-z']{2,}");

//建立索引
public int indexDocument(Jedis conn, String docid, String content) {
    //得到文章的词汇
    Set<String> words = tokenize(content);
    Transaction trans = conn.multi();
    //建立反向索引
    for (String word : words) {
        trans.sadd("idx:" + word, docid);
    }
    return trans.exec().size();
}
//得到文章的词汇
public Set<String> tokenize(String content) {
    Set<String> words = new HashSet<String>();
    //WORDS_RE是文章的内容
    Matcher matcher = WORDS_RE.matcher(content);
    while (matcher.find()){
        //找出词汇
        String word = matcher.group().trim();
        if (word.length() > 2 && !STOP_WORDS.contains(word)){
            words.add(word);
        }
    }
    //返回文章的词汇
    return words;
}
```

为了方便集合操作，提供了相关的工具方法：

```java
//为了方便对集合进行运算，提供一些工具方法
public String intersect(Transaction trans, int ttl, String... items) {
    return setCommon(trans, "sinterstore", ttl, items);
}

public String union(Transaction trans, int ttl, String... items) {
    return setCommon(trans, "sunionstore", ttl, items);
}

public String difference(Transaction trans, int ttl, String... items) {
    return setCommon(trans, "sdiffstore", ttl, items);
}
public String zintersect(
    Transaction trans, int ttl, ZParams params, String... sets) {
    return zsetCommon(trans, "zinterstore", ttl, params, sets);
}
//提供对集合进行操作的方法
private String setCommon(
    Transaction trans, String method, int ttl, String... items)
{
    //items是传入的词汇
    String[] keys = new String[items.length];
    for (int i = 0; i < items.length; i++){
        keys[i] = "idx:" + items[i];
    }
    //结果存入该id中
    String id = UUID.randomUUID().toString();
    try{
        //通过反射调用方法，原因是因为只要运行时才知道调用什么方法
        trans.getClass()
            .getDeclaredMethod(method, String.class, String[].class)
            .invoke(trans, "idx:" + id, keys);
    }catch(Exception e){
        throw new RuntimeException(e);
    }
    trans.expire("idx:" + id, ttl);
    return id;
}
//对有序集合的操作
private String zsetCommon(
    Transaction trans, String method, int ttl, ZParams params, String... sets) {
    String[] keys = new String[sets.length];
    for (int i = 0; i < sets.length; i++) {
        keys[i] = "idx:" + sets[i];
    }

    String id = UUID.randomUUID().toString();
    try{
        trans.getClass()
            .getDeclaredMethod(method, String.class, ZParams.class, String[].class)
            .invoke(trans, "idx:" + id, params, keys);
    }catch(Exception e){
        throw new RuntimeException(e);
    }
    trans.expire("idx:" + id, ttl);
    return id;
}
```

查询的代码：

```java
public String parseAndSearch(Jedis conn, String queryString, int ttl) {
    //解析命令，返回结果中的all表示所有查询的单词，unwanted表示排除的单词
    Query query = parse(queryString);
    if (query.all.isEmpty()){
        return null;
    }

    List<String> toIntersect = new ArrayList<String>();
    for (List<String> syn : query.all) {
        if (syn.size() > 1) {
            Transaction trans = conn.multi();
            toIntersect.add(union(trans, ttl, syn.toArray(new String[syn.size()])));
            trans.exec();
        }else{
            toIntersect.add(syn.get(0));
        }
    }

    String intersectResult = null;
    if (toIntersect.size() > 1) {
        Transaction trans = conn.multi();
        intersectResult = intersect(
            trans, ttl, toIntersect.toArray(new String[toIntersect.size()]));
        trans.exec();
    }else{
        intersectResult = toIntersect.get(0);
    }

    if (!query.unwanted.isEmpty()) {
        String[] keys = query.unwanted
            .toArray(new String[query.unwanted.size() + 1]);
        keys[keys.length - 1] = intersectResult;
        Transaction trans = conn.multi();
        intersectResult = difference(trans, ttl, keys);
        trans.exec();
    }

    return intersectResult;
}
```

对结果进行排序，并参考两个变量：文章的更新时间和文章的评分：

```java
//weights存放了文章的更新时间和评分对排序的权重
public SearchResult searchAndZsort(
    Jedis conn, String queryString, boolean desc, Map<String,Integer> weights) {
    int ttl = 300;
    int start = 0;
    int num = 20;
    String id = parseAndSearch(conn, queryString, ttl);
    //按照两个权重排序，一个是更新时间，一个是评分
    int updateWeight = weights.containsKey("update") ? weights.get("update") : 1;
    int voteWeight = weights.containsKey("vote") ? weights.get("vote") : 0;
    //更新时间和评分，分别存于update和votes
    String[] keys = new String[]{id, "sort:update", "sort:votes"};
    Transaction trans = conn.multi();
    //对结合进行运算
    id = zintersect(
        trans, ttl, new ZParams().weights(0, updateWeight, voteWeight), keys);

    trans.zcard("idx:" + id);
    if (desc) {
        trans.zrevrange("idx:" + id, start, start + num - 1);
    }else{
        trans.zrange("idx:" + id, start, start + num - 1);
    }
    //返回结果
    List<Object> results = trans.exec();

    return new SearchResult(
        id,
        ((Long)results.get(results.size() - 2)).longValue(),
        // Note: it's a LinkedHashSet, so it's ordered
        new ArrayList<String>((Set<String>)results.get(results.size() - 1)));
}
```

#### 比较字符串的前缀

将字符串转化为long，这样就可以比价字符串的前缀，本案只比较前6个字符

字符串转long的核心方法：

```java
public long stringToScore(String string, boolean ignoreCase) {
    if (ignoreCase){
        string = string.toLowerCase();
    }

    List<Integer> pieces = new ArrayList<Integer>();
    //拼接ASCII码
    for (int i = 0; i < Math.min(string.length(), 6); i++) {
        pieces.add((int)string.charAt(i));
    }
    //不足6位进行补全
    while (pieces.size() < 6){
        pieces.add(-1);
    }

    long score = 0;
    //进行转换
    for (int piece : pieces) {
        score = score * 257 + piece + 1;
    }

    return score * 2 + (string.length() > 6 ? 1 : 0);
}
```

## 构建简单的社交网络

### 模拟社交网络

#### 存储用户信息

这里使用散列存储用户信息：

```java
public long createUser(Jedis conn, String login, String name) {
    String llogin = login.toLowerCase();
    //就行加锁处理，防止同名的用户出现
    String lock = acquireLockWithTimeout(conn, "user:" + llogin, 10, 1);
    if (lock == null){
        return -1;
    }

    if (conn.hget("users:", llogin) != null) {
        return -1;
    }
    //id的生成采用自增方式
    long id = conn.incr("user:id:");
    Transaction trans = conn.multi();
    trans.hset("users:", llogin, String.valueOf(id));
    Map<String,String> values = new HashMap<String,String>();
    //要存储的信息项
    values.put("login", login);
    values.put("id", String.valueOf(id));
    values.put("name", name);
    values.put("followers", "0");
    values.put("following", "0");
    values.put("posts", "0");
    values.put("signup", String.valueOf(System.currentTimeMillis()));
    //存储用户信息
    trans.hmset("user:" + id, values);
    trans.exec();
    releaseLock(conn, "user:" + llogin, lock);
    return id;
}
```

#### 存储用户的状态消息

用户的状态消息也是用散列存储，主要存储的是消息信息：

```java
public long createStatus(
    Jedis conn, long uid, String message, Map<String,String> data) {
    Transaction trans = conn.multi();
    trans.hget("user:" + uid, "login");
    //状态信息也是自增的
    trans.incr("status:id:");
    //先判断有没有改用户
    List<Object> response = trans.exec();
    String login = (String)response.get(0);
    long id = (Long)response.get(1);

    if (login == null) {
        return -1;
    }

    if (data == null){
        data = new HashMap<String,String>();
    }
    //散列要存储的内容
    data.put("message", message);
    data.put("posted", String.valueOf(System.currentTimeMillis()));
    data.put("id", String.valueOf(id));
    data.put("uid", String.valueOf(uid));
    data.put("login", login);

    trans = conn.multi();
    trans.hmset("status:" + id, data);
    trans.hincrBy("user:" + uid, "posts", 1);
    trans.exec();
    return id;
}
```

#### 主页时间线消息的显示

主页时间线也是用散列来显示，key是消息ID，value是时间

```java
public List<Map<String,String>> getStatusMessages(
    Jedis conn, long uid, int page, int count) {
    //获取主页的状态的id
    Set<String> statusIds = conn.zrevrange(
        "home:" + uid, (page - 1) * count, page * count - 1);
    //通过状态id获取具体的消息
    Transaction trans = conn.multi();
    for (String id : statusIds) {
        trans.hgetAll("status:" + id);
    }
    //组装并返回消息
    List<Map<String,String>> statuses = new ArrayList<Map<String,String>>();
    for (Object result : trans.exec()) {
        Map<String,String> status = (Map<String,String>)result;
        if (status != null && status.size() > 0){
            statuses.add(status);
        }
    }
    return statuses;
}
```

#### 关注者列表和正在关注列表

关注者和正在关注都是用散列来存储，散列的值是时间戳

下面是关注操作的执行步骤，取消关注的操作类似

```java
public boolean followUser(Jedis conn, long uid, long otherUid) {
    //要更新的关注者和正在关注的键
    String fkey1 = "following:" + uid;
    String fkey2 = "followers:" + otherUid;

    if (conn.zscore(fkey1, String.valueOf(otherUid)) != null) {
        return false;
    }

    long now = System.currentTimeMillis();

    Transaction trans = conn.multi();
    trans.zadd(fkey1, now, String.valueOf(otherUid));
    trans.zadd(fkey2, now, String.valueOf(uid));
    trans.zcard(fkey1);
    trans.zcard(fkey2);
    trans.zrevrangeWithScores("profile:" + otherUid, 0, HOME_TIMELINE_SIZE - 1);

    List<Object> response = trans.exec();
    long following = (Long)response.get(response.size() - 3);
    long followers = (Long)response.get(response.size() - 2);
    Set<Tuple> statuses = (Set<Tuple>)response.get(response.size() - 1);

    trans = conn.multi();
    //用户表中更新粉丝和关注者信息
    trans.hset("user:" + uid, "following", String.valueOf(following));
    trans.hset("user:" + otherUid, "followers", String.valueOf(followers));
    //更新执行关注操作的用户的时间线
    if (statuses.size() > 0) {
        for (Tuple status : statuses){
            trans.zadd("home:" + uid, status.getScore(), status.getElement());
        }
    }
    //只保留一定数量的时间线消息
    trans.zremrangeByRank("home:" + uid, 0, 0 - HOME_TIMELINE_SIZE - 1);
    trans.exec();

    return true;
}
```

#### 状态的发布与删除

一个用户发布状态，需要更新关注者的主页

```java
public long postStatus(
    Jedis conn, long uid, String message, Map<String,String> data) {
    //先创建状态消息
    long id = createStatus(conn, uid, message, data);
    if (id == -1){
        return -1;
    }
    //获取发布的状态
    String postedString = conn.hget("status:" + id, "posted");
    if (postedString == null) {
        return -1;
    }
    //获取状态的id
    long posted = Long.parseLong(postedString);
    //更新个人信息页
    conn.zadd("profile:" + uid, posted, String.valueOf(id));
    //对关注者的主页更新
    syndicateStatus(conn, uid, id, posted, 0);
    return id;
}

public void syndicateStatus(
    Jedis conn, long uid, long postId, long postTime, double start) {
    //获取所有的关注者
    Set<Tuple> followers = conn.zrangeByScoreWithScores(
        "followers:" + uid,
        String.valueOf(start), "inf",
        0, POSTS_PER_PASS);

    Transaction trans = conn.multi();
    for (Tuple tuple : followers){
        String follower = tuple.getElement();
        start = tuple.getScore();
        //更新关注者的主页
        trans.zadd("home:" + follower, postTime, String.valueOf(postId));
        trans.zrange("home:" + follower, 0, -1);
        trans.zremrangeByRank(
            "home:" + follower, 0, 0 - HOME_TIMELINE_SIZE - 1);
    }
    trans.exec();
    //分批更新，第一批之后的关注者使用延迟执行
    if (followers.size() >= POSTS_PER_PASS) {
        try{
            Method method = getClass().getDeclaredMethod(
                "syndicateStatus", Jedis.class, Long.TYPE, Long.TYPE, Long.TYPE, Double.TYPE);
            executeLater("default", method, uid, postId, postTime, start);
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }
}
```

删除已发布消息：

```java
public boolean deleteStatus(Jedis conn, long uid, long statusId) {
    String key = "status:" + statusId;
    String lock = acquireLockWithTimeout(conn, key, 1, 10);
    if (lock == null) {
        return false;
    }

    try{
        if (!String.valueOf(uid).equals(conn.hget(key, "uid"))) {
            return false;
        }

        Transaction trans = conn.multi();
        trans.del(key);
        trans.zrem("profile:" + uid, String.valueOf(statusId));
        trans.zrem("home:" + uid, String.valueOf(statusId));
        trans.hincrBy("user:" + uid, "posts", -1);
        trans.exec();

        return true;
    }finally{
        releaseLock(conn, key, lock);
    }
}
```

## 降低内存占用

降低内存有助于减少创建快照所需的时间，提升载入AOF文件和重写AOF文件时的效率，缩短同步的时间

### 短结构

Redis为列表、集合、散列和有序集合提供了一组配置选项，这些选项可以让Redis以更节约空间的方式存储长度较短的结构

#### 压缩列表

Redis在通常情况下使用双向列表表示列表、使用散列表示散列、使用散列加跳跃表表示有序集合。对于双向链表来说，它除了存储必要的成员之外，还要存储字符串长度，剩余可用的字节数量，以及指向前后节点的指针，因此需要额外21字节的开销

而压缩列表只记录前一个节点的长度、当前节点的长度、以及存储的成员的值，总共只需要额外的2个字节

使用压缩列表的配置项：

> A-max-ziplist-entries 512 使用压缩列表所包含的最大数量
>
> A-max-ziplist-value 64 使用压缩列表所支持的最大字节
>
> 其中A可以用list、hash、zset代替
>
> 如果超过上面两个限制，将会从压缩编码转换为其他结构，内存占用因此也会增加，并且编码形式不会在条件再次满足后转为压缩编码

可以通过debug_object(“键”)查看当前的encoding形式

#### 整数集合编码

对于set来说，如果所有成员都能被解释成十进制整数，并且集合成员的数量又足够少的话，那么R就会以有序证书数组的方式存储set

配置：

> set-max-intset-entries 512 超过该大小，将不会使用整数集合编码

不论是压缩列表和整数集合编码都会设置一定的限制，这是由于对于长度较大的列表和set，对数据进行更新和读取需要对内存里的数据进行移动，因此会给性能带来负面的影响。

### 分片结构

分片就是将数据划分为更小的部分，这样每个小分部就可以采用上面的压缩编码方式进行存储，从而降低存储空间

分片的方式一般是采用hash算法，得到一个数字，根据分片的大小和数据的总大小，得到分的片数，以及该key的分片id，下面是分片式散列的代码：

```java
/**
* @param base 散列的名字
* @param key 要分片的键
* @param totalElements 总元素
* @param shardSize 分片大小
* @return 返回分片id
*/
public String shardKey(String base, String key, long totalElements, int shardSize) {
  long shardId = 0;
  if (isDigit(key)) {
    //如果键本身就是数字直接转为数字，这里假设数据是密级出现的
    shardId = Integer.parseInt(key, 10) / shardSize;
  }else{
    CRC32 crc = new CRC32();
    crc.update(key.getBytes());
    long shards = 2 * totalElements / shardSize;
    shardId = Math.abs(((int)crc.getValue()) % shards);
  }
  return base + ':' + shardId;
}
```

由于改变散列的存储结构，因此对散列的一些操作方法也要重写，下面是set和get的重写：

```java
public Long shardHset(
  Jedis conn, String base, String key, String value, long totalElements, int shardSize) {
  String shard = shardKey(base, key, totalElements, shardSize);
  return conn.hset(shard, key, value);
}

public String shardHget(
  Jedis conn, String base, String key, int totalElements, int shardSize) {
  String shard = shardKey(base, key, totalElements, shardSize);
  return conn.hget(shard, key);
}
```

分片set也是类似，下面是利用uuid的前15位（uuid的前56个二进制位）来实现分片set的代码，由于uuid虽然是数字，但不是集中出现的，所有会加一个前缀，使其识别为字符串：

```java
public Long shardSadd(
  Jedis conn, String base, String member, long totalElements, int shardSize) {
  String shard = shardKey(base, "x" + member, totalElements, shardSize);
  return conn.sadd(shard, member);
}
```

我们可以通过这个sadd方法来实现一个访客计数器：

```java
private int SHARD_SIZE = 512;
//访客计数
public void countVisit(Jedis conn, String sessionId) {
  Calendar today = Calendar.getInstance();
  String key = "unique:" + ISO_FORMAT.format(today.getTime());
  //根据昨天的访问量估计今天的总人数
  long expected = getExpected(conn, key, today);
  long id = Long.parseLong(sessionId.replace("-", "").substring(0, 15), 16);
  //如果id不存在，访问计数+1
  if (shardSadd(conn, key, String.valueOf(id), expected, SHARD_SIZE) != 0) {
    conn.incr(key);
  }
}
private long DAILY_EXPECTED = 1000000;
private Map<String,Long> EXPECTED = new HashMap<String,Long>();

//获取期望的人数
public long getExpected(Jedis conn, String key, Calendar today) {
  if (!EXPECTED.containsKey(key)) {
    String exkey = key + ":expected";
    String expectedStr = conn.get(exkey);

    long expected = 0;
    if (expectedStr == null) {
      Calendar yesterday = (Calendar)today.clone();
      yesterday.add(Calendar.DATE, -1);
      //获取昨天的人数
      expectedStr = conn.get(
        "unique:" + ISO_FORMAT.format(yesterday.getTime()));
      //DAILY_EXPECTED是默认的人数
      expected = expectedStr != null ? Long.parseLong(expectedStr) : DAILY_EXPECTED;
      //预期人数是昨天人数的1.5倍
      expected = (long)Math.pow(2, (long)(Math.ceil(Math.log(expected * 1.5) / Math.log(2))));
      //如果已经有其他人存入了，直接返回
      if (conn.setnx(exkey, String.valueOf(expected)) == 0) {
        expectedStr = conn.get(exkey);
        expected = Integer.parseInt(expectedStr);
      }
    }else{
      //如果该值已经存在，直接返回
      expected = Long.parseLong(expectedStr);
    }

    EXPECTED.put(key, expected);
  }

  return EXPECTED.get(key);
}
```

### 打包二进制位和字节

这里讨论的的是存储用户的国家和省份（州）信息，因为国家和州的名字相对较长，对于大量用户来说，占用的空间较大，我们可以采用存储索引的方式降低内存使用（国家和州的信息各占一个字节），同时为了降低键的数量，我们把大量用户的位置信息存放在一个字符串里面，为了防止字符串超出能分配的大小，会进行字符串分片处理。

```java
//假设每片存这么多的用户
private long USERS_PER_SHARD = (long)Math.pow(2, 20);

public void setLocation(
    Jedis conn, long userId, String country, String state)
{
    String code = getCode(country, state);
    //进行分片
    long shardId = userId / USERS_PER_SHARD;
    int position = (int)(userId % USERS_PER_SHARD);
    int offset = position * 2;

    Pipeline pipe = conn.pipelined();
    //分片存储，此处是字符串的分片，相当于把字符串拆开存放，shardId基本都是0，为了减少键的数量
    //相当于把大量的用户位置信息存放于一个字符串中
    pipe.setrange("location:" + shardId, offset, code);

    String tkey = UUID.randomUUID().toString();

    pipe.zadd(tkey, userId, "max");
    //存储最大用户id，方便聚合计算
    pipe.zunionstore(
        "location:max",
        new ZParams().aggregate(ZParams.Aggregate.MAX),
        tkey,
        "location:max");
    pipe.del(tkey);
    pipe.sync();
}
//COUNTRIES存放的国家的数组
public String getCode(String country, String state) {
    int cindex = bisectLeft(COUNTRIES, country);
    //如果索引不合法，返回-1
    if (cindex > COUNTRIES.length || !country.equals(COUNTRIES[cindex])) {
        cindex = -1;
    }
    //为了让不存在时，索引是0
    cindex++;

    int sindex = -1;
    if (state != null && STATES.containsKey(country)) {
        //获取州的索引，STATES是一个map，键是国家
        String[] states = STATES.get(country);
        sindex = bisectLeft(states, state);
        if (sindex > states.length || !state.equals(states[sindex])) {
            sindex=-1;
        }
    }
    sindex++;
    //返回的是字符串
    return new String(new char[]{(char)cindex, (char)sindex});
}
//找出key在数组中的偏移量，如果不存在，返回的在插入该元素时的索引
private int bisectLeft(String[] values, String key) {
    int index = Arrays.binarySearch(values, key);
    return index < 0 ? Math.abs(index) - 1 : index;
}
```

存储后，我们可以对数据进行聚合计算，统计用户国家和省份的数据量：

```java
public Pair<Map<String,Long>,Map<String,Map<String,Long>>> aggregateLocation(Jedis conn) {
    Map<String,Long> countries = new HashMap<String,Long>();
    Map<String,Map<String,Long>> states = new HashMap<String,Map<String,Long>>();
    //用最大的用户ID来判断最大的分片ID
    long maxId = conn.zscore("location:max", "max").longValue();
    long maxBlock = maxId;

    byte[] buffer = new byte[(int)Math.pow(2, 17)];
    //读取分片中的每个数据块，这里模拟inputream读取数据
    for (int shardId = 0; shardId <= maxBlock; shardId++) {
        InputStream in = new RedisInputStream(conn, "location:" + shardId);
        try{
            int read = 0;
            while ((read = in.read(buffer, 0, buffer.length)) != -1){
                for (int offset = 0; offset < read - 1; offset += 2) {
                    String code = new String(buffer, offset, 2);
                    //将位置编码转化成国家和州的信息
                    updateAggregates(countries, states, code);
                }
            }
        }catch(IOException ioe) {
            throw new RuntimeException(ioe);
        }finally{
            try{
                in.close();
            }catch(Exception e){
                // ignore
            }
        }
    }
    //返回结果
    return new Pair<Map<String,Long>,Map<String,Map<String,Long>>>(countries, states);
}
public void updateAggregates(
            Map<String,Long> countries, Map<String,Map<String,Long>> states, String code)
{
    if (code.length() != 2) {
        return;
    }
    //获取国家和州的字符
    int countryIdx = (int)code.charAt(0) - 1;
    int stateIdx = (int)code.charAt(1) - 1;

    if (countryIdx < 0 || countryIdx >= COUNTRIES.length) {
        return;
    }
    //获取国家字符信息
    String country = COUNTRIES[countryIdx];
    Long countryAgg = countries.get(country);
    if (countryAgg == null){
        countryAgg = Long.valueOf(0);
    }
    //统计该国家的数量
    countries.put(country, countryAgg + 1);

    if (!STATES.containsKey(country)) {
        return;
    }
    if (stateIdx < 0 || stateIdx >= STATES.get(country).length){
        return;
    }
    //获取州的字符串，并统计数量
    String state = STATES.get(country)[stateIdx];
    Map<String,Long> stateAggs = states.get(country);
    if (stateAggs == null){
        stateAggs = new HashMap<String,Long>();
        states.put(country, stateAggs);
    }
    Long stateAgg = stateAggs.get(state);
    if (stateAgg == null){
        stateAgg = Long.valueOf(0);
    }
    stateAggs.put(state, stateAgg + 1);
}
```

上面RedisInputStream的实现:

```java
public class RedisInputStream
    extends InputStream {
    private Jedis conn;
    private String key;
    private int pos;

    public RedisInputStream(Jedis conn, String key){
        this.conn = conn;
        this.key = key;
    }

    @Override
    public int available()
        throws IOException
    {
        long len = conn.strlen(key);
        return (int)(len - pos);
    }

    @Override
    public int read()
        throws IOException
    {
        byte[] block = conn.substr(key.getBytes(), pos, pos);
        if (block == null || block.length == 0){
            return -1;
        }
        pos++;
        return (int)(block[0] & 0xff);
    }

    @Override
    public int read(byte[] buf, int off, int len)
        throws IOException
    {
        byte[] block = conn.substr(key.getBytes(), pos, pos + (len - off - 1));
        if (block == null || block.length == 0){
            return -1;
        }
        System.arraycopy(block, 0, buf, off, block.length);
        pos += block.length;
        return block.length;
    }

    @Override
    public void close() {
        // no-op
    }
}
```

也可以传部分用户ID进行计算：

```java
public Pair<Map<String,Long>,Map<String,Map<String,Long>>> aggregateLocationList(
    Jedis conn, long[] userIds)
{
    Map<String,Long> countries = new HashMap<String,Long>();
    Map<String,Map<String,Long>> states = new HashMap<String,Map<String,Long>>();

    Pipeline pipe = conn.pipelined();
    for (int i = 0; i < userIds.length; i++) {
        long userId = userIds[i];
        long shardId = userId / USERS_PER_SHARD;
        int position = (int)(userId % USERS_PER_SHARD);
        int offset = position * 2;

        pipe.substr("location:" + shardId, offset, offset + 1);
        //每1000个同步一次结果并处理
        if ((i + 1) % 1000 == 0) {
            updateAggregates(countries, states, pipe.syncAndReturnAll());
        }
    }

    updateAggregates(countries, states, pipe.syncAndReturnAll());

    return new Pair<Map<String,Long>,Map<String,Map<String,Long>>>(countries, states);
}
public void updateAggregates(
    Map<String,Long> countries, Map<String,Map<String,Long>> states, List<Object> codes)
{
    for (Object code : codes) {
        updateAggregates(countries, states, (String)code);
    }
}
```

## 扩展Redis

#### 扩展读性能

1. 可以通过只读从服务器提升系统处理读查询的性能
2. 多个从服务器采用主从复制树
3. 使用Redis Sentinel配合Redis的复制功能，并对下线的主服务器进行故障转移

-----

> <https://blog.csdn.net/guanhang89/article/details/81490400>
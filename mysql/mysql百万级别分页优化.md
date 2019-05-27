一般刚开始学SQL的时候，会这样写 ：

```
SELECT * FROM table ORDER BY id LIMIT 1000, 10; 
```

但在数据达到百万级的时候，这样写会慢死 ：

```
SELECT * FROM table ORDER BY id LIMIT 1000000, 10; 
```

也许耗费几十秒 

网上很多优化的方法是这样的：

```
SELECT * FROM table WHERE id >= (SELECT id FROM table LIMIT 1000000, 1) LIMIT 10; 
```

是的，速度提升到0.x秒了，看样子还行了 
可是，还不是完美的！ 

以下这句才是完美的！ 

```
SELECT * FROM table WHERE id BETWEEN 1000000 AND 1000010; 
```

比上面那句，还要再快5至10倍 

另外，如果需要查询 id 不是连续的一段，最佳的方法就是先找出 id ，然后用 in 查询：

```
SELECT * FROM table WHERE id IN(10000, 100000, 1000000...); 
```

再分享一点 
查询字段一较长字符串的时候，表设计时要为该字段多加一个字段,如，存储网址的字段 
查询的时候，不要直接查询字符串，效率低下，应该查诡该字串的crc32或md5 

如何优化Mysql千万级快速分页 

Limit 1,111 数据大了确实有些性能上的问题，而通过各种方法给用上where id >= XX,这样用上索引的id号可能速度上快点儿。By:jack 
Mysql limit分页慢的解决办法（Mysql limit 优化，百万至千万条记录实现快速分页） 

MySql 性能到底能有多高？用了php半年多，真正如此深入的去思考这个问题还是从前天开始。有过痛苦有过绝望，到现在充满信心！MySql 这个数据库绝对是适合dba级的高手去玩的，一般做一点1万篇新闻的小型系统怎么写都可以，用xx框架可以实现快速开发。可是数据量到了10万，百万至千 万，他的性能还能那么高吗？一点小小的失误，可能造成整个系统的改写，甚至更本系统无法正常运行！好了，不那么多废话了。用事实说话，看例子： 
数 据表 collect ( id, title ,info ,vtype) 就这4个字段，其中 title 用定长，info 用text, id 是逐渐，vtype是tinyint，vtype是索引。这是一个基本的新闻系统的简单模型。现在往里面填充数据，填充10万篇新闻。 
最后collect 为 10万条记录，数据库表占用硬盘1.6G。OK ,看下面这条sql语句： 

select id,title from collect limit 1000,10; 很快；基本上0.01秒就OK，再看下面的 
select id,title from collect limit 90000,10; 从9万条开始分页，结果？ 
8-9秒完成，my god 哪出问题了？？？？其实要优化这条数据，网上找得到答案。看下面一条语句: 
select id from collect order by id limit 90000,10; 很快，0.04秒就OK。 为什么？因为用了id主键做索引当然快。网上的改法是： 
select id,title from collect where id>=(select id from collect order by id limit 90000,1) limit 10; 
这就是用了id做索引的结果。可是问题复杂那么一点点，就完了。看下面的语句 
select id from collect where vtype=1 order by id limit 90000,10; 很慢，用了8-9秒！ 
到 了这里我相信很多人会和我一样，有崩溃感觉！vtype 做了索引了啊？怎么会慢呢？vtype做了索引是不错，你直接 select id from collect where vtype=1 limit 1000,10; 是很快的，基本上0.05秒，可是提高90倍，从9万开始，那就是0.05*90=4.5秒的速度了。和测试结果8-9秒到了一个数量级。从这里开始有人 提出了分表的思路，这个和dis #cuz 论坛是一样的思路。思路如下： 
建一个索引表： t (id,title,vtype) 并设置成定长，然后做分页，分页出结果再到 collect 里面去找info 。 是否可行呢？实验下就知道了。 
10万条记录到 t(id,title,vtype) 里，数据表大小20M左右。用 
select id from t where vtype=1 order by id limit 90000,10; 很快了。基本上0.1-0.2秒可以跑完。为什么会这样呢？我猜想是因为collect 数据太多，所以分页要跑很长的路。limit 完全和数据表的大小有关的。其实这样做还是全表扫描，只是因为数据量小，只有10万才快。OK， 来个疯狂的实验，加到100万条，测试性能。 
加了10倍的数据，马上t表就到了200多M，而且是定长。还是刚才的查询语句，时间是0.1-0.2秒完成！分表性能没问题？错！因为我们的limit还是9万，所以快。给个大的，90万开始 
select id from t where vtype=1 order by id limit 900000,10; 看看结果，时间是1-2秒！ 
why ?? 分表了时间还是这么长，非常之郁闷！有人说定长会提高limit的性能，开始我也以为，因为一条记录的长度是固定的，mysql 应该可以算出90万的位置才对啊？ 可是我们高估了mysql 的智能，他不是商务数据库，事实证明定长和非定长对limit影响不大？ 怪不得有人说 discuz到了100万条记录就会很慢，我相信这是真的，这个和数据库设计有关！ 
难道MySQL 无法突破100万的限制吗？？？到了100万的分页就真的到了极限？？？ 
答案是： NO !!!! 为什么突破不了100万是因为不会设计mysql造成的。下面介绍非分表法，来个疯狂的测试！一张表搞定100万记录，并且10G 数据库，如何快速分页！ 
好了，我们的测试又回到 collect表，开始测试结论是： 30万数据，用分表法可行，超过30万他的速度会慢道你无法忍受！当然如果用分表+我这种方法，那是绝对完美的。但是用了我这种方法后，不用分表也可以完美解决！ 
答 案就是：复合索引！ 有一次设计mysql索引的时候，无意中发现索引名字可以任取，可以选择几个字段进来，这有什么用呢？开始的select id from collect order by id limit 90000,10; 这么快就是因为走了索引，可是如果加了where 就不走索引了。抱着试试看的想法加了 search(vtype,id) 这样的索引。然后测试 
select id from collect where vtype=1 limit 90000,10; 非常快！0.04秒完成！ 
再测试: select id ,title from collect where vtype=1 limit 90000,10; 非常遗憾，8-9秒，没走search索引！ 
再测试：search(id,vtype)，还是select id 这个语句，也非常遗憾，0.5秒。 
综上：如果对于有where 条件，又想走索引用limit的，必须设计一个索引，将where 放第一位，limit用到的主键放第2位，而且只能select 主键！ 
完美解决了分页问题了。可以快速返回id就有希望优化limit ， 按这样的逻辑，百万级的limit 应该在0.0x秒就可以分完。看来mysql 语句的优化和索引时非常重要的！ 
好了，回到原题，如何将上面的研究成功快速应用于开发呢？如果用复合查询，我的轻量级框架就没的用了。分页字符串还得自己写，那多麻烦？这里再看一个例子，思路就出来了： 
select * from collect where id in (9000,12,50,7000); 竟然 0秒就可以查完！ 
mygod ，mysql 的索引竟然对于in语句同样有效！看来网上说in无法用索引是错误的！ 
有了这个结论，就可以很简单的应用于轻量级框架了： 

```
$db=dblink(); 
$db->pagesize=20; 
$sql=”select id from collect where vtype=$vtype”; 
$db->execute($sql); 
$strpage=$db->strpage(); //将分页字符串保存在临时变量，方便输出 
while($rs=$db->fetch_array()){ 
$strid.=$rs['id'].','; 
} 
$strid=substr($strid,0,strlen($strid)-1); //构造出id字符串 
$db->pagesize=0; //很关键，在不注销类的情况下，将分页清空，这样只需要用一次数据库连接，不需要再开； 
$db->execute(“select id,title,url,sTime,gTime,vtype,tag from collect where id in ($strid)”); 
<?php while($rs=$db->fetch_array()): ?> 
<tr> 
<td> <?php echo $rs['id'];?></td> 
<td> <?php echo $rs['url'];?></td> 
<td> <?php echo $rs['sTime'];?></td> 
<td> <?php echo $rs['gTime'];?></td> 
<td> <?php echo $rs['vtype'];?></td> 
<td> <a href=”?act=show&id=<?php echo $rs['id'];?>” target=”_blank”><?php echo $rs['title'];?></a></td> 
<td> <?php echo $rs['tag'];?></td> 
</tr> 
<?php endwhile; ?> 
</table> 
<?php 
echo $strpage;
```

通过简单的变换，其实思路很简单：

1）通过优化索引，找出id，并拼成 “123,90000,12000″ 这样的字符串。

2）第2次查询找出结果。 
小小的索引+一点点的改动就使mysql 可以支持百万甚至千万级的高效分页！ 
通 过这里的例子，我反思了一点：对于大型系统，PHP千万不能用框架，尤其是那种连sql语句都看不到的框架！因为开始对于我的轻量级框架都差点崩溃！只适 合小型应用的快速开发，对于ERP,OA，大型网站，数据层包括逻辑层的东西都不能用框架。如果程序员失去了对sql语句的把控，那项目的风险将会成几何 级数增加！尤其是用mysql 的时候，mysql 一定需要专业的dba 才可以发挥他的最佳性能。一个索引所造成的性能差别可能是上千倍！ 
PS: 经过实际测试，到了100万的数据，160万数据，15G表，190M索引，就算走索引，limit都得0.49秒。
# 常用调优案例和方法

## 将新对象预留在新生代

由于 Full GC 的成本远远高于 Minor GC，因此某些情况下需要尽可能将对象分配在年轻代。因此，在 JVM 参数调优时可以为应用程序分配一个合理的年轻代空间，以最大限度避免新对象直接进入年老代的情况发生。

合理设置一个年轻代的空间大小。-Xmn 调整这个参数，最好设置成堆内存的3/8,例如最大-Xmx5G，那么 -Xmn应该设置成3/8*2大约在2G左右

设置合理的survivor区并提高survivor区的使用率。 第一种是通过参数-XX:TargetSurvivorRatio提高from区的利用率；第二种方法通过-XX:SurvivorRatio，设置一个更大的from区。

## 大对象进入老年代

因为大对象出现在年轻代很可能扰乱年轻代 GC，并破坏年轻代原有的对象结构。因为尝试在年轻代分配大对象，很可能导致空间不足，为了有足够的空间容纳大对象，JVM 不得不将年轻代中的年轻对象挪到年老代。因为大对象占用空间多，所以可能需要移动大量小的年轻对象进入年老代，这对 GC 相当不利。基于以上原因，可以将大对象直接分配到年老代，保持年轻代对象结构的完整性，这样可以提高 GC 的效率。

可以使用-XX:PetenureSizeThreshold 设置大对象直接进入老年代的阈值。

举例：-XX:PetenureSizeThreshold=1000000 当对象大小超过这个值时，将直接在老年代分配。

## 设置对象进入老年代的年龄

对象在新生代经过一次GC依然存活，则年龄＋1，当年龄达到阀值，就移入老年代。 阀值的最大值通过参数： -XX:MaxTenuringThreshold 来设置，它默认是15。 在实际虚拟机运行过程中，并不是按照这个年龄阀值来判断，而是依据内存使用情况来判断，但这个年龄阀值是最大值，也就说到达这个年龄的对象一定会被移到老年代。

举例：-XX:MaxTenuringThreshold=1 即所有经过一次GC的对象都可以直接进入老年代。

## 稳定与震荡的堆大小

当 -Xms与 -Xmx设置大小一样，是一个稳定的堆，这样做的好处是，减少GC的次数。

当 -Xms与 -Xmx设置大小不一样，是一个不稳定的堆，它会增加GC的次数，但是它在系统不需要使用大内存时，压缩堆空间，使得GC应对一个较小的堆，可以加快单次GC的次数。

可以通过两个参数设置用语压缩和扩展堆空间：

- [ ] -XX:MinHeapFreeRatio: 设置堆的最小空闲比例，默认是40，当堆空间的空闲空间小于这个数值时，jvm会自动扩展空间。
- [ ] -XX：MaxHeapFreeRatio: 设置堆的最大空闲比例，默认是70，当堆空间的空闲空间大于这个数值时，jvm会自动压缩空间。

## 吞吐量优先案例

吞吐量优先的方案将会尽可能减少系统执行垃圾回收的总时间，故可以考虑关注系统吞吐量的并行回收收集器。在拥有4GB内存和32核CPU的计算机上，进行吞吐量的优化，可以使用参数：

```javascript
java –Xmx3800m –Xms3800m –Xmn2G –Xss128k –XX:+UseParallelGC 
   –XX:ParallelGCThreads=20 –XX:+UseParallelOldGC
```

- [ ] –Xmx380m –Xms3800m：设置 Java 堆的最大值和初始值。一般情况下，为了避免堆内存的频繁震荡，导致系统性能下降，我们的做法是设置最大堆等于最小堆。假设这里把最小堆减少为最大堆的一半，即 1900m，那么 JVM 会尽可能在 1900MB 堆空间中运行，如果这样，发生 GC 的可能性就会比较高；
- [ ] -Xss128k：减少线程栈的大小，这样可以使剩余的系统内存支持更多的线程；
- [ ] -Xmn2g：设置年轻代区域大小为 2GB；
- [ ] –XX:+UseParallelGC：年轻代使用并行垃圾回收收集器。这是一个关注吞吐量的收集器，可以尽可能地减少 GC 时间。
- [ ] –XX:ParallelGCThreads：设置用于垃圾回收的线程数，通常情况下，可以设置和 CPU 数量相等。但在 CPU 数量比较多的情况下，设置相对较小的数值也是合理的；
- [ ] –XX:+UseParallelOldGC：设置年老代使用并行回收收集器。

## 使用大页案例

在 Solaris 系统中，JVM 可以支持 Large Page Size 的使用。使用大的内存分页可以增强 CPU 的内存寻址能力，从而提升系统的性能。

```javascript
java –Xmx2506m –Xms2506m –Xmn1536m –Xss128k -XX:++UseParallelGC
 –XX:ParallelGCThreads=20 –XX:+UseParallelOldGC –XX:+LargePageSizeInBytes=256m
```

–XX:+LargePageSizeInBytes：设置大页的大小。

## 降低停顿案例

为降低应用软件的垃圾回收时的停顿，首先考虑的是使用关注系统停顿的 CMS 回收器，其次，为了减少 Full GC 次数，应尽可能将对象预留在年轻代，因为年轻代 Minor GC 的成本远远小于年老代的 Full GC。

```javascript
java –Xmx3550m –Xms3550m –Xmn2g –Xss128k –XX:ParallelGCThreads=20
 –XX:+UseConcMarkSweepGC –XX:+UseParNewGC –XX:+SurvivorRatio=8 –XX:TargetSurvivorRatio=90
 –XX:MaxTenuringThreshold=31
```

- [ ] –XX:ParallelGCThreads=20：设置 20 个线程进行垃圾回收；
- [ ] –XX:+UseParNewGC：新生代使用并行回收器；
- [ ] –XX:+UseConcMarkSweepGC：老年代使用 CMS 收集器降低停顿；
- [ ] –XX:+SurvivorRatio：设置 Eden 区和 Survivor 区的比例为 8:1。稍大的 Survivor 空间可以提高在新生代回收生命周期较短的对象的可能性（如果 Survivor 不够大，一些短命的对象可能直接进入老年代，这对系统来说是不利的）。
- [ ] –XX:TargetSurvivorRatio：设置 Survivor 区的可使用率。这里设置为 90%，则允许 90%的 Survivor 空间被使用。默认值是 50%。故该设置提高了 Survivor 区的使用率。当存放的对象超过这个百分比，则对象会向老年代压缩。因此，这个选项更有助于将对象留在新生代。
- [ ] –XX:MaxTenuringThreshold：设置年轻对象晋升到老年代的年龄。默认值是 15 次，即对象经过 15 次 Minor GC 依然存活，则进入老年代。这里设置为 31，即尽可能地让对象保存在新生代。

----

> <https://cloud.tencent.com/developer/article/1353746>


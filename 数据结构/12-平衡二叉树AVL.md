# 平衡二叉树AVL

[TOC]

## 一：概述

> 平衡二叉树：【详解】又被称为AVL树，它是一 棵空树或对任意一个节点，它的左右两个子树的高度差的绝对值不超过1，并且左右两个子树都是一棵平衡二叉树。这个方案很好的解决了二叉查找树退化成链表的问题，把插入，查找，删除的时间复杂度最好情况和最坏情况都维持在O(logN)。但是频繁旋转会使插入和删除牺牲掉O(logN)左右的时间，不过相对二叉查找树来说，时间上稳定了很多。平衡二叉树的高度和节点数量之间关系：O(log n); 

![image-20190417104421344](https://ws3.sinaimg.cn/large/006tNc79ly1g25fpa6ul8j30ho0figoq.jpg)

判断是否为 平衡二叉树条件：平衡因子[节点的左子树高度 减 右子树高度]的绝对值 < 2 ;下图不是平衡二叉树

![image-20190417104438503](https://ws1.sinaimg.cn/large/006tNc79ly1g25fpkymzbj30ha0hu78f.jpg)

满二叉树：一定是平衡二叉树；除了叶子节点外，所有其他的节点都有左右两个子树；让整棵树的状态达到最低的状态；

![image-20190417104455541](https://ws3.sinaimg.cn/large/006tNc79ly1g25fpvthbnj30gy0d80v8.jpg)

完全二叉树：是平衡二叉树；将所有元素按照二叉树的形状一层一层“铺开”，有可能有非叶子节点其右子树是空的，整体而言，空缺部分一定在右下部分，叶子节点最大深度值与最小深度值相差不会超过 1；

![image-20190417104510150](https://ws2.sinaimg.cn/large/006tNc79ly1g25fq534wpj30hi0a6gnj.jpg)

线段树：也是平衡二叉树；不是完全二叉树，因为空出来的部分不一定在树的右下角的位置，但整体而言，叶子节点要么在最后一层，要不在倒数第二层，整体叶子节点的深度相差不会超过 1 ；

![image-20190417104527347](https://ws1.sinaimg.cn/large/006tNc79ly1g25fqftrddj30n20ck42g.jpg)

-----

计算节点的高度和平衡因子:

```java
import java.util.ArrayList;
 
public class AVLTree<K extends Comparable<K>, V> {
 
    private class Node{
        public K key;
        public V value;
        public Node left, right;
        public int height;		//记录高度值
 
        public Node(K key, V value){
            this.key = key;
            this.value = value;
            left = null;
            right = null;
            height = 1;
        }
    }
 
    private Node root;
    private int size;
 
    public AVLTree(){
        root = null;
        size = 0;
    }
 
    public int getSize(){
        return size;
    }
 
    public boolean isEmpty(){
        return size == 0;
    }
 
    // 获得节点node的高度【新增代码】
    private int getHeight(Node node){
        if(node == null)
            return 0;
        return node.height;
    }
 
    // 获得节点node的平衡因子【新增代码】
    private int getBalanceFactor(Node node){
        if(node == null)
            return 0;
        return getHeight(node.left) - getHeight(node.right);
    }
 
    // 向二分搜索树中添加新的元素(key, value)
    public void add(K key, V value){
        root = add(root, key, value);
    }
 
    // 向以node为根的二分搜索树中插入元素(key, value)，递归算法
    // 返回插入新节点后二分搜索树的根
    private Node add(Node node, K key, V value){
 
        if(node == null){
            size ++;
            return new Node(key, value);
        }
 
        if(key.compareTo(node.key) < 0)
            node.left = add(node.left, key, value);
        else if(key.compareTo(node.key) > 0)
            node.right = add(node.right, key, value);
        else // key.compareTo(node.key) == 0
            node.value = value;
 
        // 更新height
        node.height = 1 + Math.max(getHeight(node.left), getHeight(node.right));
 
        // 计算平衡因子
        int balanceFactor = getBalanceFactor(node);
        if(Math.abs(balanceFactor) > 1)
            System.out.println("unbalanced : " + balanceFactor);
 
        return node;
    }
 
    // 返回以node为根节点的二分搜索树中，key所在的节点
    private Node getNode(Node node, K key){
 
        if(node == null)
            return null;
 
        if(key.equals(node.key))
            return node;
        else if(key.compareTo(node.key) < 0)
            return getNode(node.left, key);
        else // if(key.compareTo(node.key) > 0)
            return getNode(node.right, key);
    }
 
    public boolean contains(K key){
        return getNode(root, key) != null;
    }
 
    public V get(K key){
 
        Node node = getNode(root, key);
        return node == null ? null : node.value;
    }
 
    public void set(K key, V newValue){
        Node node = getNode(root, key);
        if(node == null)
            throw new IllegalArgumentException(key + " doesn't exist!");
 
        node.value = newValue;
    }
 
    // 返回以node为根的二分搜索树的最小值所在的节点
    private Node minimum(Node node){
        if(node.left == null)
            return node;
        return minimum(node.left);
    }
 
    // 删除掉以node为根的二分搜索树中的最小节点
    // 返回删除节点后新的二分搜索树的根
    private Node removeMin(Node node){
 
        if(node.left == null){
            Node rightNode = node.right;
            node.right = null;
            size --;
            return rightNode;
        }
 
        node.left = removeMin(node.left);
        return node;
    }
 
    // 从二分搜索树中删除键为key的节点
    public V remove(K key){
 
        Node node = getNode(root, key);
        if(node != null){
            root = remove(root, key);
            return node.value;
        }
        return null;
    }
 
    private Node remove(Node node, K key){
 
        if( node == null )
            return null;
 
        if( key.compareTo(node.key) < 0 ){
            node.left = remove(node.left , key);
            return node;
        }
        else if(key.compareTo(node.key) > 0 ){
            node.right = remove(node.right, key);
            return node;
        }
        else{   // key.compareTo(node.key) == 0
 
            // 待删除节点左子树为空的情况
            if(node.left == null){
                Node rightNode = node.right;
                node.right = null;
                size --;
                return rightNode;
            }
 
            // 待删除节点右子树为空的情况
            if(node.right == null){
                Node leftNode = node.left;
                node.left = null;
                size --;
                return leftNode;
            }
 
            // 待删除节点左右子树均不为空的情况
 
            // 找到比待删除节点大的最小节点, 即待删除节点右子树的最小节点
            // 用这个节点顶替待删除节点的位置
            Node successor = minimum(node.right);
            successor.right = removeMin(node.right);
            successor.left = node.left;
 
            node.left = node.right = null;
 
            return successor;
        }
    }
 
    public static void main(String[] args){
 
        System.out.println("Pride and Prejudice");
 
        ArrayList<String> words = new ArrayList<>();
        if(FileOperation.readFile("pride-and-prejudice.txt", words)) {
            System.out.println("Total words: " + words.size());
 
            AVLTree<String, Integer> map = new AVLTree<>();
            for (String word : words) {
                if (map.contains(word))
                    map.set(word, map.get(word) + 1);
                else
                    map.add(word, 1);
            }
 
            System.out.println("Total different words: " + map.getSize());
            System.out.println("Frequency of PRIDE: " + map.get("pride"));
            System.out.println("Frequency of PREJUDICE: " + map.get("prejudice"));
        }
 
        System.out.println();
    }
}
```

```java
import java.util.ArrayList;
 
public class BST<K extends Comparable<K>, V> {
 
    private class Node{
        public K key;
        public V value;
        public Node left, right;
 
        public Node(K key, V value){
            this.key = key;
            this.value = value;
            left = null;
            right = null;
        }
    }
 
    private Node root;
    private int size;
 
    public BST(){
        root = null;
        size = 0;
    }
 
    public int getSize(){
        return size;
    }
 
    public boolean isEmpty(){
        return size == 0;
    }
 
    // 向二分搜索树中添加新的元素(key, value)
    public void add(K key, V value){
        root = add(root, key, value);
    }
 
    // 向以node为根的二分搜索树中插入元素(key, value)，递归算法
    // 返回插入新节点后二分搜索树的根
    private Node add(Node node, K key, V value){
 
        if(node == null){
            size ++;
            return new Node(key, value);
        }
 
        if(key.compareTo(node.key) < 0)
            node.left = add(node.left, key, value);
        else if(key.compareTo(node.key) > 0)
            node.right = add(node.right, key, value);
        else // key.compareTo(node.key) == 0
            node.value = value;
 
        return node;
    }
 
    // 返回以node为根节点的二分搜索树中，key所在的节点
    private Node getNode(Node node, K key){
 
        if(node == null)
            return null;
 
        if(key.equals(node.key))
            return node;
        else if(key.compareTo(node.key) < 0)
            return getNode(node.left, key);
        else // if(key.compareTo(node.key) > 0)
            return getNode(node.right, key);
    }
 
    public boolean contains(K key){
        return getNode(root, key) != null;
    }
 
    public V get(K key){
 
        Node node = getNode(root, key);
        return node == null ? null : node.value;
    }
 
    public void set(K key, V newValue){
        Node node = getNode(root, key);
        if(node == null)
            throw new IllegalArgumentException(key + " doesn't exist!");
 
        node.value = newValue;
    }
 
    // 返回以node为根的二分搜索树的最小值所在的节点
    private Node minimum(Node node){
        if(node.left == null)
            return node;
        return minimum(node.left);
    }
 
    // 删除掉以node为根的二分搜索树中的最小节点
    // 返回删除节点后新的二分搜索树的根
    private Node removeMin(Node node){
 
        if(node.left == null){
            Node rightNode = node.right;
            node.right = null;
            size --;
            return rightNode;
        }
 
        node.left = removeMin(node.left);
        return node;
    }
 
    // 从二分搜索树中删除键为key的节点
    public V remove(K key){
 
        Node node = getNode(root, key);
        if(node != null){
            root = remove(root, key);
            return node.value;
        }
        return null;
    }
 
    private Node remove(Node node, K key){
 
        if( node == null )
            return null;
 
        if( key.compareTo(node.key) < 0 ){
            node.left = remove(node.left , key);
            return node;
        }
        else if(key.compareTo(node.key) > 0 ){
            node.right = remove(node.right, key);
            return node;
        }
        else{   // key.compareTo(node.key) == 0
 
            // 待删除节点左子树为空的情况
            if(node.left == null){
                Node rightNode = node.right;
                node.right = null;
                size --;
                return rightNode;
            }
 
            // 待删除节点右子树为空的情况
            if(node.right == null){
                Node leftNode = node.left;
                node.left = null;
                size --;
                return leftNode;
            }
 
            // 待删除节点左右子树均不为空的情况
 
            // 找到比待删除节点大的最小节点, 即待删除节点右子树的最小节点
            // 用这个节点顶替待删除节点的位置
            Node successor = minimum(node.right);
            successor.right = removeMin(node.right);
            successor.left = node.left;
 
            node.left = node.right = null;
 
            return successor;
        }
    }
 
    public static void main(String[] args){
 
        System.out.println("Pride and Prejudice");
 
        ArrayList<String> words = new ArrayList<>();
        if(FileOperation.readFile("pride-and-prejudice.txt", words)) {
            System.out.println("Total words: " + words.size());
 
            BST<String, Integer> map = new BST<>();
            for (String word : words) {
                if (map.contains(word))
                    map.set(word, map.get(word) + 1);
                else
                    map.add(word, 1);
            }
 
            System.out.println("Total different words: " + map.getSize());
            System.out.println("Frequency of PRIDE: " + map.get("pride"));
            System.out.println("Frequency of PREJUDICE: " + map.get("prejudice"));
        }
 
        System.out.println();
    }
}
```

----

判断二叉树是不是二分搜索树和平衡二叉树:

```java
import java.util.ArrayList;
 
public class AVLTree<K extends Comparable<K>, V> {
 
    private class Node{
        public K key;
        public V value;
        public Node left, right;
        public int height;
 
        public Node(K key, V value){
            this.key = key;
            this.value = value;
            left = null;
            right = null;
            height = 1;
        }
    }
 
    private Node root;
    private int size;
 
    public AVLTree(){
        root = null;
        size = 0;
    }
 
    public int getSize(){
        return size;
    }
 
    public boolean isEmpty(){
        return size == 0;
    }
 
    // 判断该二叉树是否是一棵二分搜索树【新增代码】
    public boolean isBST(){
 
        ArrayList<K> keys = new ArrayList<>();
        inOrder(root, keys);
        for(int i = 1 ; i < keys.size() ; i ++)
            if(keys.get(i - 1).compareTo(keys.get(i)) > 0)	//中序遍历不是升序数组
                return false;
        return true;
    }
 
    private void inOrder(Node node, ArrayList<K> keys){
 
        if(node == null)
            return;
 
        inOrder(node.left, keys);
        keys.add(node.key);
        inOrder(node.right, keys);
    }
 
    // 判断该二叉树是否是一棵平衡二叉树【新增代码】
    public boolean isBalanced(){
        return isBalanced(root);
    }
 
    // 判断以Node为根的二叉树是否是一棵平衡二叉树，递归算法【新增代码】
    private boolean isBalanced(Node node){
 
        if(node == null)
            return true;
 
        int balanceFactor = getBalanceFactor(node);
        if(Math.abs(balanceFactor) > 1)		//平衡因子 > 1
            return false;
        return isBalanced(node.left) && isBalanced(node.right);
    }
 
    // 获得节点node的高度
    private int getHeight(Node node){
        if(node == null)
            return 0;
        return node.height;
    }
 
    // 获得节点node的平衡因子
    private int getBalanceFactor(Node node){
        if(node == null)
            return 0;
        return getHeight(node.left) - getHeight(node.right);
    }
 
    // 向二分搜索树中添加新的元素(key, value)
    public void add(K key, V value){
        root = add(root, key, value);
    }
 
    // 向以node为根的二分搜索树中插入元素(key, value)，递归算法
    // 返回插入新节点后二分搜索树的根
    private Node add(Node node, K key, V value){
 
        if(node == null){
            size ++;
            return new Node(key, value);
        }
 
        if(key.compareTo(node.key) < 0)
            node.left = add(node.left, key, value);
        else if(key.compareTo(node.key) > 0)
            node.right = add(node.right, key, value);
        else // key.compareTo(node.key) == 0
            node.value = value;
 
        // 更新height
        node.height = 1 + Math.max(getHeight(node.left), getHeight(node.right));
 
        // 计算平衡因子
        int balanceFactor = getBalanceFactor(node);
        if(Math.abs(balanceFactor) > 1)
            System.out.println("unbalanced : " + balanceFactor);
 
        return node;
    }
 
    // 返回以node为根节点的二分搜索树中，key所在的节点
    private Node getNode(Node node, K key){
 
        if(node == null)
            return null;
 
        if(key.equals(node.key))
            return node;
        else if(key.compareTo(node.key) < 0)
            return getNode(node.left, key);
        else // if(key.compareTo(node.key) > 0)
            return getNode(node.right, key);
    }
 
    public boolean contains(K key){
        return getNode(root, key) != null;
    }
 
    public V get(K key){
 
        Node node = getNode(root, key);
        return node == null ? null : node.value;
    }
 
    public void set(K key, V newValue){
        Node node = getNode(root, key);
        if(node == null)
            throw new IllegalArgumentException(key + " doesn't exist!");
 
        node.value = newValue;
    }
 
    // 返回以node为根的二分搜索树的最小值所在的节点
    private Node minimum(Node node){
        if(node.left == null)
            return node;
        return minimum(node.left);
    }
 
    // 删除掉以node为根的二分搜索树中的最小节点
    // 返回删除节点后新的二分搜索树的根
    private Node removeMin(Node node){
 
        if(node.left == null){
            Node rightNode = node.right;
            node.right = null;
            size --;
            return rightNode;
        }
 
        node.left = removeMin(node.left);
        return node;
    }
 
    // 从二分搜索树中删除键为key的节点
    public V remove(K key){
 
        Node node = getNode(root, key);
        if(node != null){
            root = remove(root, key);
            return node.value;
        }
        return null;
    }
 
    private Node remove(Node node, K key){
 
        if( node == null )
            return null;
 
        if( key.compareTo(node.key) < 0 ){
            node.left = remove(node.left , key);
            return node;
        }
        else if(key.compareTo(node.key) > 0 ){
            node.right = remove(node.right, key);
            return node;
        }
        else{   // key.compareTo(node.key) == 0
 
            // 待删除节点左子树为空的情况
            if(node.left == null){
                Node rightNode = node.right;
                node.right = null;
                size --;
                return rightNode;
            }
 
            // 待删除节点右子树为空的情况
            if(node.right == null){
                Node leftNode = node.left;
                node.left = null;
                size --;
                return leftNode;
            }
 
            // 待删除节点左右子树均不为空的情况
 
            // 找到比待删除节点大的最小节点, 即待删除节点右子树的最小节点
            // 用这个节点顶替待删除节点的位置
            Node successor = minimum(node.right);
            successor.right = removeMin(node.right);
            successor.left = node.left;
 
            node.left = node.right = null;
 
            return successor;
        }
    }
 
    public static void main(String[] args){
 
        System.out.println("Pride and Prejudice");
 
        ArrayList<String> words = new ArrayList<>();
        if(FileOperation.readFile("pride-and-prejudice.txt", words)) {
            System.out.println("Total words: " + words.size());
 
            AVLTree<String, Integer> map = new AVLTree<>();
            for (String word : words) {
                if (map.contains(word))
                    map.set(word, map.get(word) + 1);
                else
                    map.add(word, 1);
            }
 
            System.out.println("Total different words: " + map.getSize());
            System.out.println("Frequency of PRIDE: " + map.get("pride"));
            System.out.println("Frequency of PREJUDICE: " + map.get("prejudice"));
 
            System.out.println("is BST : " + map.isBST());
            System.out.println("is Balanced : " + map.isBalanced());
        }
 
        System.out.println();
    }
}
```

可见，是二分搜索树，但不是平衡二叉树；

---

## 二：AVL 旋转操作的基本原理

AVL 实现自平衡的机制：左旋转和右旋转；

维护平衡的时机：

在二分搜索树中，想要插入一个节点的话；

![image-20190417104709176](https://ws1.sinaimg.cn/large/006tNc79ly1g25fs7lwfij30hg0a4mzt.jpg)

需要从根节点开始，一路下来寻找到该节点正确的插入位置，正确的插入位置都是叶子节点的位置

![image-20190417104724150](https://ws4.sinaimg.cn/large/006tNc79ly1g25fsgazc3j30ia0a6go9.jpg)

由于新添加节点才会导致整棵二分搜索树不满足平衡性；不平衡节点只有可能发生在从插入位置开始，向其父元素中找，会发生在其父节点与祖先节点中，因为插入新节点后，其父节点与祖先节点左右子树需要进行更新；在更新后，平衡因子可能 > 1 ;

![image-20190417104741328](https://ws2.sinaimg.cn/large/006tNc79ly1g25fsr7698j30ig0acacq.jpg)

故维护平衡的时机：加入节点后，沿着节点向上维护平衡性；

图解：

![image-20190417104806318](/Users/codecarver/Library/Application Support/typora-user-images/image-20190417104806318.png)

----

## 三 ：如何维护平衡性

1. 右旋转

   ![image-20190417104856061](https://ws2.sinaimg.cn/large/006tNc79ly1g25fu2ybxvj31b60huaem.jpg)

右旋转 过程：

![image-20190417104920522](https://ws4.sinaimg.cn/large/006tNc79ly1g25fuhhvw7j30qe0r8gsp.jpg)

![image-20190417104942804](https://ws1.sinaimg.cn/large/006tNc79ly1g25fuv7pg1j31hk0tmqen.jpg)

2. 左旋转：右子树高度比左子树高度要高，且高度差 > 1;右子树高度 >= 左子树；如图中，存在关系：T4 < y < T3 < x < T1 < z < T2;插入的元素在不平衡节点的右侧的右侧

   ![image-20190417105012470](/Users/codecarver/Library/Application Support/typora-user-images/image-20190417105012470.png)

![image-20190417105029058](https://ws2.sinaimg.cn/large/006tNc79ly1g25fvo5kz4j30qg0w2ahw.jpg)

![image-20190417105042303](https://ws3.sinaimg.cn/large/006tNc79ly1g25fvw1fzjj31hw0g479w.jpg)

```java
import java.util.ArrayList;
 
public class AVLTree<K extends Comparable<K>, V> {
 
    private class Node{
        public K key;
        public V value;
        public Node left, right;
        public int height;
 
        public Node(K key, V value){
            this.key = key;
            this.value = value;
            left = null;
            right = null;
            height = 1;
        }
    }
 
    private Node root;
    private int size;
 
    public AVLTree(){
        root = null;
        size = 0;
    }
 
    public int getSize(){
        return size;
    }
 
    public boolean isEmpty(){
        return size == 0;
    }
 
    // 判断该二叉树是否是一棵二分搜索树
    public boolean isBST(){
 
        ArrayList<K> keys = new ArrayList<>();
        inOrder(root, keys);
        for(int i = 1 ; i < keys.size() ; i ++)
            if(keys.get(i - 1).compareTo(keys.get(i)) > 0)
                return false;
        return true;
    }
 
    private void inOrder(Node node, ArrayList<K> keys){
 
        if(node == null)
            return;
 
        inOrder(node.left, keys);
        keys.add(node.key);
        inOrder(node.right, keys);
    }
 
    // 判断该二叉树是否是一棵平衡二叉树
    public boolean isBalanced(){
        return isBalanced(root);
    }
 
    // 判断以Node为根的二叉树是否是一棵平衡二叉树，递归算法
    private boolean isBalanced(Node node){
 
        if(node == null)
            return true;
 
        int balanceFactor = getBalanceFactor(node);
        if(Math.abs(balanceFactor) > 1)
            return false;
        return isBalanced(node.left) && isBalanced(node.right);
    }
 
    // 获得节点node的高度
    private int getHeight(Node node){
        if(node == null)
            return 0;
        return node.height;
    }
 
    // 获得节点node的平衡因子
    private int getBalanceFactor(Node node){
        if(node == null)
            return 0;
        return getHeight(node.left) - getHeight(node.right);
    }
 
    // 对节点y进行向右旋转操作，返回旋转后新的根节点x
    //        y                              x
    //       / \                           /   \
    //      x   T4     向右旋转 (y)        z     y
    //     / \       - - - - - - - ->    / \   / \
    //    z   T3                       T1  T2 T3 T4
    //   / \
    // T1   T2
    private Node rightRotate(Node y) {	//【旋转操作，新增代码】
        Node x = y.left;
        Node T3 = x.right;
 
        // 向右旋转过程
        x.right = y;
        y.left = T3;
 
        // 更新height
        y.height = Math.max(getHeight(y.left), getHeight(y.right)) + 1;
        x.height = Math.max(getHeight(x.left), getHeight(x.right)) + 1;
 
        return x;
    }
 
    // 对节点y进行向左旋转操作，返回旋转后新的根节点x
    //    y                             x
    //  /  \                          /   \
    // T1   x      向左旋转 (y)       y     z
    //     / \   - - - - - - - ->   / \   / \
    //   T2  z                     T1 T2 T3 T4
    //      / \
    //     T3 T4
    private Node leftRotate(Node y) {
        Node x = y.right;
        Node T2 = x.left;
 
        // 向左旋转过程
        x.left = y;
        y.right = T2;
 
        // 更新height
        y.height = Math.max(getHeight(y.left), getHeight(y.right)) + 1;
        x.height = Math.max(getHeight(x.left), getHeight(x.right)) + 1;
 
        return x;
    }
 
    // 向二分搜索树中添加新的元素(key, value)
    public void add(K key, V value){
        root = add(root, key, value);
    }
 
    // 向以node为根的二分搜索树中插入元素(key, value)，递归算法
    // 返回插入新节点后二分搜索树的根
    private Node add(Node node, K key, V value){
 
        if(node == null){
            size ++;
            return new Node(key, value);
        }
 
        if(key.compareTo(node.key) < 0)
            node.left = add(node.left, key, value);
        else if(key.compareTo(node.key) > 0)
            node.right = add(node.right, key, value);
        else // key.compareTo(node.key) == 0
            node.value = value;
 
        // 更新height
        node.height = 1 + Math.max(getHeight(node.left), getHeight(node.right));
 
        // 计算平衡因子
        int balanceFactor = getBalanceFactor(node);
//        if(Math.abs(balanceFactor) > 1)
//            System.out.println("unbalanced : " + balanceFactor);
 
        // 平衡维护
        if (balanceFactor > 1 && getBalanceFactor(node.left) >= 0)
            return rightRotate(node);
 
        if (balanceFactor < -1 && getBalanceFactor(node.right) <= 0)
            return leftRotate(node);
 
        return node;
    }
 
    // 返回以node为根节点的二分搜索树中，key所在的节点
    private Node getNode(Node node, K key){
 
        if(node == null)
            return null;
 
        if(key.equals(node.key))
            return node;
        else if(key.compareTo(node.key) < 0)
            return getNode(node.left, key);
        else // if(key.compareTo(node.key) > 0)
            return getNode(node.right, key);
    }
 
    public boolean contains(K key){
        return getNode(root, key) != null;
    }
 
    public V get(K key){
 
        Node node = getNode(root, key);
        return node == null ? null : node.value;
    }
 
    public void set(K key, V newValue){
        Node node = getNode(root, key);
        if(node == null)
            throw new IllegalArgumentException(key + " doesn't exist!");
 
        node.value = newValue;
    }
 
    // 返回以node为根的二分搜索树的最小值所在的节点
    private Node minimum(Node node){
        if(node.left == null)
            return node;
        return minimum(node.left);
    }
 
    // 删除掉以node为根的二分搜索树中的最小节点
    // 返回删除节点后新的二分搜索树的根
    private Node removeMin(Node node){
 
        if(node.left == null){
            Node rightNode = node.right;
            node.right = null;
            size --;
            return rightNode;
        }
 
        node.left = removeMin(node.left);
        return node;
    }
 
    // 从二分搜索树中删除键为key的节点
    public V remove(K key){
 
        Node node = getNode(root, key);
        if(node != null){
            root = remove(root, key);
            return node.value;
        }
        return null;
    }
 
    private Node remove(Node node, K key){
 
        if( node == null )
            return null;
 
        if( key.compareTo(node.key) < 0 ){
            node.left = remove(node.left , key);
            return node;
        }
        else if(key.compareTo(node.key) > 0 ){
            node.right = remove(node.right, key);
            return node;
        }
        else{   // key.compareTo(node.key) == 0
 
            // 待删除节点左子树为空的情况
            if(node.left == null){
                Node rightNode = node.right;
                node.right = null;
                size --;
                return rightNode;
            }
 
            // 待删除节点右子树为空的情况
            if(node.right == null){
                Node leftNode = node.left;
                node.left = null;
                size --;
                return leftNode;
            }
 
            // 待删除节点左右子树均不为空的情况
 
            // 找到比待删除节点大的最小节点, 即待删除节点右子树的最小节点
            // 用这个节点顶替待删除节点的位置
            Node successor = minimum(node.right);
            successor.right = removeMin(node.right);
            successor.left = node.left;
 
            node.left = node.right = null;
 
            return successor;
        }
    }
 
    public static void main(String[] args){
 
        System.out.println("Pride and Prejudice");
 
        ArrayList<String> words = new ArrayList<>();
        if(FileOperation.readFile("pride-and-prejudice.txt", words)) {
            System.out.println("Total words: " + words.size());
 
            AVLTree<String, Integer> map = new AVLTree<>();
            for (String word : words) {
                if (map.contains(word))
                    map.set(word, map.get(word) + 1);
                else
                    map.add(word, 1);
            }
 
            System.out.println("Total different words: " + map.getSize());
            System.out.println("Frequency of PRIDE: " + map.get("pride"));
            System.out.println("Frequency of PREJUDICE: " + map.get("prejudice"));
 
            System.out.println("is BST : " + map.isBST());
            System.out.println("is Balanced : " + map.isBalanced());
        }
 
        System.out.println();
    }
}
```

-----

## 四：LR 和 RL

插入的元素在不平衡的节点的左侧的右侧；不能简单的使用左旋转和右旋转；

![image-20190417105157348](https://ws2.sinaimg.cn/large/006tNc79ly1g25fx7vb1xj30y40j446f.jpg)

LL 和 RR 

LL:新插入的节点在 向上回溯 找到的 第一个不平衡节点 的 左孩子 的 左侧；【右旋转即可】

RR：新插入的节点在 向上回溯 找到的 第一个不平衡节点 的 右孩子 的 右侧；【左旋转即可】

![image-20190417105255420](https://ws4.sinaimg.cn/large/006tNc79ly1g25fy7zug9j30zy0eqn3l.jpg)

LR:新插入的节点在 向上回溯 找到的 第一个不平衡节点 的 左孩子 的 右侧；

![image-20190417105311949](https://ws4.sinaimg.cn/large/006tNc79ly1g25fyi9sn3j30hy0imdka.jpg)

LR 处理过程：

首先对 x 进行左旋转，将其转换为 LL 的情况；再用处理 LL 的方法进行右旋转即可；

![image-20190417105337716](https://ws2.sinaimg.cn/large/006tNc79ly1g25fyxpv0xj30u00xwthl.jpg)

RL处理过程：

首先对 x 进行右旋转，将其转换为 RR 的情况；再用处理 RR 的方法进行左旋转即可；

![image-20190417105354778](https://ws3.sinaimg.cn/large/006tNc79ly1g25fz8u2cyj30ns0bwtbe.jpg)

```java
import java.util.ArrayList;
 
public class AVLTree<K extends Comparable<K>, V> {
 
    private class Node{
        public K key;
        public V value;
        public Node left, right;
        public int height;
 
        public Node(K key, V value){
            this.key = key;
            this.value = value;
            left = null;
            right = null;
            height = 1;
        }
    }
 
    private Node root;
    private int size;
 
    public AVLTree(){
        root = null;
        size = 0;
    }
 
    public int getSize(){
        return size;
    }
 
    public boolean isEmpty(){
        return size == 0;
    }
 
    // 判断该二叉树是否是一棵二分搜索树
    public boolean isBST(){
 
        ArrayList<K> keys = new ArrayList<>();
        inOrder(root, keys);
        for(int i = 1 ; i < keys.size() ; i ++)
            if(keys.get(i - 1).compareTo(keys.get(i)) > 0)
                return false;
        return true;
    }
 
    private void inOrder(Node node, ArrayList<K> keys){
 
        if(node == null)
            return;
 
        inOrder(node.left, keys);
        keys.add(node.key);
        inOrder(node.right, keys);
    }
 
    // 判断该二叉树是否是一棵平衡二叉树
    public boolean isBalanced(){
        return isBalanced(root);
    }
 
    // 判断以Node为根的二叉树是否是一棵平衡二叉树，递归算法
    private boolean isBalanced(Node node){
 
        if(node == null)
            return true;
 
        int balanceFactor = getBalanceFactor(node);
        if(Math.abs(balanceFactor) > 1)
            return false;
        return isBalanced(node.left) && isBalanced(node.right);
    }
 
    // 获得节点node的高度
    private int getHeight(Node node){
        if(node == null)
            return 0;
        return node.height;
    }
 
    // 获得节点node的平衡因子
    private int getBalanceFactor(Node node){
        if(node == null)
            return 0;
        return getHeight(node.left) - getHeight(node.right);
    }
 
    // 对节点y进行向右旋转操作，返回旋转后新的根节点x
    //        y                              x
    //       / \                           /   \
    //      x   T4     向右旋转 (y)        z     y
    //     / \       - - - - - - - ->    / \   / \
    //    z   T3                       T1  T2 T3 T4
    //   / \
    // T1   T2
    private Node rightRotate(Node y) {
        Node x = y.left;
        Node T3 = x.right;
 
        // 向右旋转过程
        x.right = y;
        y.left = T3;
 
        // 更新height
        y.height = Math.max(getHeight(y.left), getHeight(y.right)) + 1;
        x.height = Math.max(getHeight(x.left), getHeight(x.right)) + 1;
 
        return x;
    }
 
    // 对节点y进行向左旋转操作，返回旋转后新的根节点x
    //    y                             x
    //  /  \                          /   \
    // T1   x      向左旋转 (y)       y     z
    //     / \   - - - - - - - ->   / \   / \
    //   T2  z                     T1 T2 T3 T4
    //      / \
    //     T3 T4
    private Node leftRotate(Node y) {
        Node x = y.right;
        Node T2 = x.left;
 
        // 向左旋转过程
        x.left = y;
        y.right = T2;
 
        // 更新height
        y.height = Math.max(getHeight(y.left), getHeight(y.right)) + 1;
        x.height = Math.max(getHeight(x.left), getHeight(x.right)) + 1;
 
        return x;
    }
 
    // 向二分搜索树中添加新的元素(key, value)
    public void add(K key, V value){
        root = add(root, key, value);
    }
 
    // 向以node为根的二分搜索树中插入元素(key, value)，递归算法
    // 返回插入新节点后二分搜索树的根
    private Node add(Node node, K key, V value){
 
        if(node == null){
            size ++;
            return new Node(key, value);
        }
 
        if(key.compareTo(node.key) < 0)
            node.left = add(node.left, key, value);
        else if(key.compareTo(node.key) > 0)
            node.right = add(node.right, key, value);
        else // key.compareTo(node.key) == 0
            node.value = value;
 
        // 更新height
        node.height = 1 + Math.max(getHeight(node.left), getHeight(node.right));
 
        // 计算平衡因子
        int balanceFactor = getBalanceFactor(node);
 
        // 平衡维护
		//LL
        if (balanceFactor > 1 && getBalanceFactor(node.left) >= 0)
            return rightRotate(node);
		//RR
        if (balanceFactor < -1 && getBalanceFactor(node.right) <= 0)
            return leftRotate(node);
		//LR
        if (balanceFactor > 1 && getBalanceFactor(node.left) < 0) {
            node.left = leftRotate(node.left);
            return rightRotate(node);
        }
		//RL
        if (balanceFactor < -1 && getBalanceFactor(node.right) > 0) {
            node.right = rightRotate(node.right);
            return leftRotate(node);
        }
 
        return node;
    }
 
    // 返回以node为根节点的二分搜索树中，key所在的节点
    private Node getNode(Node node, K key){
 
        if(node == null)
            return null;
 
        if(key.equals(node.key))
            return node;
        else if(key.compareTo(node.key) < 0)
            return getNode(node.left, key);
        else // if(key.compareTo(node.key) > 0)
            return getNode(node.right, key);
    }
 
    public boolean contains(K key){
        return getNode(root, key) != null;
    }
 
    public V get(K key){
 
        Node node = getNode(root, key);
        return node == null ? null : node.value;
    }
 
    public void set(K key, V newValue){
        Node node = getNode(root, key);
        if(node == null)
            throw new IllegalArgumentException(key + " doesn't exist!");
 
        node.value = newValue;
    }
 
    // 返回以node为根的二分搜索树的最小值所在的节点
    private Node minimum(Node node){
        if(node.left == null)
            return node;
        return minimum(node.left);
    }
 
    // 删除掉以node为根的二分搜索树中的最小节点
    // 返回删除节点后新的二分搜索树的根
    private Node removeMin(Node node){
 
        if(node.left == null){
            Node rightNode = node.right;
            node.right = null;
            size --;
            return rightNode;
        }
 
        node.left = removeMin(node.left);
        return node;
    }
 
    // 从二分搜索树中删除键为key的节点
    public V remove(K key){
 
        Node node = getNode(root, key);
        if(node != null){
            root = remove(root, key);
            return node.value;
        }
        return null;
    }
 
    private Node remove(Node node, K key){
 
        if( node == null )
            return null;
 
        if( key.compareTo(node.key) < 0 ){
            node.left = remove(node.left , key);
            return node;
        }
        else if(key.compareTo(node.key) > 0 ){
            node.right = remove(node.right, key);
            return node;
        }
        else{   // key.compareTo(node.key) == 0
 
            // 待删除节点左子树为空的情况
            if(node.left == null){
                Node rightNode = node.right;
                node.right = null;
                size --;
                return rightNode;
            }
 
            // 待删除节点右子树为空的情况
            if(node.right == null){
                Node leftNode = node.left;
                node.left = null;
                size --;
                return leftNode;
            }
 
            // 待删除节点左右子树均不为空的情况
 
            // 找到比待删除节点大的最小节点, 即待删除节点右子树的最小节点
            // 用这个节点顶替待删除节点的位置
            Node successor = minimum(node.right);
            successor.right = removeMin(node.right);
            successor.left = node.left;
 
            node.left = node.right = null;
 
            return successor;
        }
    }
 
    public static void main(String[] args){
 
        System.out.println("Pride and Prejudice");
 
        ArrayList<String> words = new ArrayList<>();
        if(FileOperation.readFile("pride-and-prejudice.txt", words)) {
            System.out.println("Total words: " + words.size());
 
            AVLTree<String, Integer> map = new AVLTree<>();
            for (String word : words) {
                if (map.contains(word))
                    map.set(word, map.get(word) + 1);
                else
                    map.add(word, 1);
            }
 
            System.out.println("Total different words: " + map.getSize());
            System.out.println("Frequency of PRIDE: " + map.get("pride"));
            System.out.println("Frequency of PREJUDICE: " + map.get("prejudice"));
 
            System.out.println("is BST : " + map.isBST());
            System.out.println("is Balanced : " + map.isBalanced());
        }
 
        System.out.println();
    }
}
```

```java
import java.util.ArrayList;
import java.util.Collections;
 
public class Main {
 
    public static void main(String[] args) {
 
        System.out.println("Pride and Prejudice");
 
        ArrayList<String> words = new ArrayList<>();
        if(FileOperation.readFile("pride-and-prejudice.txt", words)) {
            System.out.println("Total words: " + words.size());
 
            // Collections.sort(words);
 
            // Test BST
            long startTime = System.nanoTime();
 
            BST<String, Integer> bst = new BST<>();
            for (String word : words) {
                if (bst.contains(word))
                    bst.set(word, bst.get(word) + 1);
                else
                    bst.add(word, 1);
            }
 
            for(String word: words)
                bst.contains(word);
 
            long endTime = System.nanoTime();
 
            double time = (endTime - startTime) / 1000000000.0;
            System.out.println("BST: " + time + " s");
 
 
            // Test AVL Tree
            startTime = System.nanoTime();
 
            AVLTree<String, Integer> avl = new AVLTree<>();
            for (String word : words) {
                if (avl.contains(word))
                    avl.set(word, avl.get(word) + 1);
                else
                    avl.add(word, 1);
            }
 
            for(String word: words)
                avl.contains(word);
 
            endTime = System.nanoTime();
 
            time = (endTime - startTime) / 1000000000.0;
            System.out.println("AVL: " + time + " s");
        }
 
        System.out.println();
    }
}
```

> 有了 自平衡 机制，二叉树不会退化为 链表；

----

## 五：从AVL树中删除元素

使用二分搜索树的思路将某一元素删除掉，删除掉之后，从删除节点的子树的根节点出发，向上回溯搜索，对于其父节点和祖宗节点来说，可能破坏了平衡性，需要维护平衡；具体的维护平衡的方式与添加元素的方式完全一样

```java
import java.util.ArrayList;
 
public class AVLTree<K extends Comparable<K>, V> {
 
    private class Node{
        public K key;
        public V value;
        public Node left, right;
        public int height;
 
        public Node(K key, V value){
            this.key = key;
            this.value = value;
            left = null;
            right = null;
            height = 1;
        }
    }
 
    private Node root;
    private int size;
 
    public AVLTree(){
        root = null;
        size = 0;
    }
 
    public int getSize(){
        return size;
    }
 
    public boolean isEmpty(){
        return size == 0;
    }
 
    // 判断该二叉树是否是一棵二分搜索树
    public boolean isBST(){
 
        ArrayList<K> keys = new ArrayList<>();
        inOrder(root, keys);
        for(int i = 1 ; i < keys.size() ; i ++)
            if(keys.get(i - 1).compareTo(keys.get(i)) > 0)
                return false;
        return true;
    }
 
    private void inOrder(Node node, ArrayList<K> keys){
 
        if(node == null)
            return;
 
        inOrder(node.left, keys);
        keys.add(node.key);
        inOrder(node.right, keys);
    }
 
    // 判断该二叉树是否是一棵平衡二叉树
    public boolean isBalanced(){
        return isBalanced(root);
    }
 
    // 判断以Node为根的二叉树是否是一棵平衡二叉树，递归算法
    private boolean isBalanced(Node node){
 
        if(node == null)
            return true;
 
        int balanceFactor = getBalanceFactor(node);
        if(Math.abs(balanceFactor) > 1)
            return false;
        return isBalanced(node.left) && isBalanced(node.right);
    }
 
    // 获得节点node的高度
    private int getHeight(Node node){
        if(node == null)
            return 0;
        return node.height;
    }
 
    // 获得节点node的平衡因子
    private int getBalanceFactor(Node node){
        if(node == null)
            return 0;
        return getHeight(node.left) - getHeight(node.right);
    }
 
    // 对节点y进行向右旋转操作，返回旋转后新的根节点x
    //        y                              x
    //       / \                           /   \
    //      x   T4     向右旋转 (y)        z     y
    //     / \       - - - - - - - ->    / \   / \
    //    z   T3                       T1  T2 T3 T4
    //   / \
    // T1   T2
    private Node rightRotate(Node y) {
        Node x = y.left;
        Node T3 = x.right;
 
        // 向右旋转过程
        x.right = y;
        y.left = T3;
 
        // 更新height
        y.height = Math.max(getHeight(y.left), getHeight(y.right)) + 1;
        x.height = Math.max(getHeight(x.left), getHeight(x.right)) + 1;
 
        return x;
    }
 
    // 对节点y进行向左旋转操作，返回旋转后新的根节点x
    //    y                             x
    //  /  \                          /   \
    // T1   x      向左旋转 (y)       y     z
    //     / \   - - - - - - - ->   / \   / \
    //   T2  z                     T1 T2 T3 T4
    //      / \
    //     T3 T4
    private Node leftRotate(Node y) {
        Node x = y.right;
        Node T2 = x.left;
 
        // 向左旋转过程
        x.left = y;
        y.right = T2;
 
        // 更新height
        y.height = Math.max(getHeight(y.left), getHeight(y.right)) + 1;
        x.height = Math.max(getHeight(x.left), getHeight(x.right)) + 1;
 
        return x;
    }
 
    // 向二分搜索树中添加新的元素(key, value)
    public void add(K key, V value){
        root = add(root, key, value);
    }
 
    // 向以node为根的二分搜索树中插入元素(key, value)，递归算法
    // 返回插入新节点后二分搜索树的根
    private Node add(Node node, K key, V value){
 
        if(node == null){
            size ++;
            return new Node(key, value);
        }
 
        if(key.compareTo(node.key) < 0)
            node.left = add(node.left, key, value);
        else if(key.compareTo(node.key) > 0)
            node.right = add(node.right, key, value);
        else // key.compareTo(node.key) == 0
            node.value = value;
 
        // 更新height
        node.height = 1 + Math.max(getHeight(node.left), getHeight(node.right));
 
        // 计算平衡因子
        int balanceFactor = getBalanceFactor(node);
 
        // 平衡维护
        // LL
        if (balanceFactor > 1 && getBalanceFactor(node.left) >= 0)
            return rightRotate(node);
 
        // RR
        if (balanceFactor < -1 && getBalanceFactor(node.right) <= 0)
            return leftRotate(node);
 
        // LR
        if (balanceFactor > 1 && getBalanceFactor(node.left) < 0) {
            node.left = leftRotate(node.left);
            return rightRotate(node);
        }
 
        // RL
        if (balanceFactor < -1 && getBalanceFactor(node.right) > 0) {
            node.right = rightRotate(node.right);
            return leftRotate(node);
        }
 
        return node;
    }
 
    // 返回以node为根节点的二分搜索树中，key所在的节点[新增代码]
    private Node getNode(Node node, K key){
 
        if(node == null)
            return null;
 
        if(key.equals(node.key))
            return node;
        else if(key.compareTo(node.key) < 0)
            return getNode(node.left, key);
        else // if(key.compareTo(node.key) > 0)
            return getNode(node.right, key);
    }
 
    public boolean contains(K key){
        return getNode(root, key) != null;
    }
 
    public V get(K key){
 
        Node node = getNode(root, key);
        return node == null ? null : node.value;
    }
 
    public void set(K key, V newValue){
        Node node = getNode(root, key);
        if(node == null)
            throw new IllegalArgumentException(key + " doesn't exist!");
 
        node.value = newValue;
    }
 
    // 返回以node为根的二分搜索树的最小值所在的节点
    private Node minimum(Node node){
        if(node.left == null)
            return node;
        return minimum(node.left);
    }
 
    // 从二分搜索树中删除键为key的节点[新增代码]
    public V remove(K key){
 
        Node node = getNode(root, key);
        if(node != null){
            root = remove(root, key);
            return node.value;
        }
        return null;
    }
 
    private Node remove(Node node, K key){
 
        if( node == null )
            return null;
 
        Node retNode;
        if( key.compareTo(node.key) < 0 ){
            node.left = remove(node.left , key);
            // return node;
            retNode = node;
        }
        else if(key.compareTo(node.key) > 0 ){
            node.right = remove(node.right, key);
            // return node;
            retNode = node;
        }
        else{   // key.compareTo(node.key) == 0
 
            // 待删除节点左子树为空的情况
            if(node.left == null){
                Node rightNode = node.right;
                node.right = null;
                size --;
                // return rightNode;
                retNode = rightNode;
            }
 
            // 待删除节点右子树为空的情况
            else if(node.right == null){
                Node leftNode = node.left;
                node.left = null;
                size --;
                // return leftNode;
                retNode = leftNode;
            }
 
            // 待删除节点左右子树均不为空的情况
            else{
                // 找到比待删除节点大的最小节点, 即待删除节点右子树的最小节点
                // 用这个节点顶替待删除节点的位置
                Node successor = minimum(node.right);
                //successor.right = removeMin(node.right);
                successor.right = remove(node.right, successor.key);
                successor.left = node.left;
 
                node.left = node.right = null;
 
                // return successor;
                retNode = successor;
            }
        }
 
        if(retNode == null)
            return null;
 
        // 更新height
        retNode.height = 1 + Math.max(getHeight(retNode.left), getHeight(retNode.right));
 
        // 计算平衡因子
        int balanceFactor = getBalanceFactor(retNode);
 
        // 平衡维护
        // LL
        if (balanceFactor > 1 && getBalanceFactor(retNode.left) >= 0)
            return rightRotate(retNode);
 
        // RR
        if (balanceFactor < -1 && getBalanceFactor(retNode.right) <= 0)
            return leftRotate(retNode);
 
        // LR
        if (balanceFactor > 1 && getBalanceFactor(retNode.left) < 0) {
            retNode.left = leftRotate(retNode.left);
            return rightRotate(retNode);
        }
 
        // RL
        if (balanceFactor < -1 && getBalanceFactor(retNode.right) > 0) {
            retNode.right = rightRotate(retNode.right);
            return leftRotate(retNode);
        }
 
        return retNode;
    }
 
    public static void main(String[] args){
 
        System.out.println("Pride and Prejudice");
 
        ArrayList<String> words = new ArrayList<>();
        if(FileOperation.readFile("pride-and-prejudice.txt", words)) {
            System.out.println("Total words: " + words.size());
 
            AVLTree<String, Integer> map = new AVLTree<>();
            for (String word : words) {
                if (map.contains(word))
                    map.set(word, map.get(word) + 1);
                else
                    map.add(word, 1);
            }
 
            System.out.println("Total different words: " + map.getSize());
            System.out.println("Frequency of PRIDE: " + map.get("pride"));
            System.out.println("Frequency of PREJUDICE: " + map.get("prejudice"));
 
            System.out.println("is BST : " + map.isBST());
            System.out.println("is Balanced : " + map.isBalanced());
 
            for(String word: words){
                map.remove(word);
                if(!map.isBST() || !map.isBalanced())
                    throw new RuntimeException();
            }
        }
 
        System.out.println();
    }
}
```

---

## 六：基于AVL树的集合(set)和映射(map)

Map:

```java
public class AVLMap<K extends Comparable<K>, V> implements Map<K, V> {
 
    private AVLTree<K, V> avl;
 
    public AVLMap(){
        avl = new AVLTree<>();
    }
 
    @Override
    public int getSize(){
        return avl.getSize();
    }
 
    @Override
    public boolean isEmpty(){
        return avl.isEmpty();
    }
 
    @Override
    public void add(K key, V value){
        avl.add(key, value);
    }
 
    @Override
    public boolean contains(K key){
        return avl.contains(key);
    }
 
    @Override
    public V get(K key){
        return avl.get(key);
    }
 
    @Override
    public void set(K key, V newValue){
        avl.set(key, newValue);
    }
 
    @Override
    public V remove(K key){
        return avl.remove(key);
    }
}
```

```java
import java.util.ArrayList;
 
public class TestMapMain {
 
    private static double testMap(Map<String, Integer> map, String filename){
 
        long startTime = System.nanoTime();
 
        System.out.println(filename);
        ArrayList<String> words = new ArrayList<>();
        if(FileOperation.readFile(filename, words)) {
            System.out.println("Total words: " + words.size());
 
            for (String word : words){
                if(map.contains(word))
                    map.set(word, map.get(word) + 1);
                else
                    map.add(word, 1);
            }
 
            System.out.println("Total different words: " + map.getSize());
            System.out.println("Frequency of PRIDE: " + map.get("pride"));
            System.out.println("Frequency of PREJUDICE: " + map.get("prejudice"));
        }
 
        long endTime = System.nanoTime();
 
        return (endTime - startTime) / 1000000000.0;
    }
 
    public static void main(String[] args) {
 
        String filename = "pride-and-prejudice.txt";
 
        BSTMap<String, Integer> bstMap = new BSTMap<>();
        double time1 = testMap(bstMap, filename);
        System.out.println("BST Map: " + time1 + " s");
 
        System.out.println();
 
        LinkedListMap<String, Integer> linkedListMap = new LinkedListMap<>();
        double time2 = testMap(linkedListMap, filename);
        System.out.println("Linked List Map: " + time2 + " s");
 
        System.out.println();
 
        AVLMap<String, Integer> avlMap = new AVLMap<>();
        double time3 = testMap(avlMap, filename);
        System.out.println("AVL Map: " + time3 + " s");
    }
}
```

---

Set:

```java
public class AVLSet<E extends Comparable<E>> implements Set<E> {
 
    private AVLTree<E, Object> avl;
 
    public AVLSet(){
        avl = new AVLTree<>();
    }
 
    @Override
    public int getSize(){
        return avl.getSize();
    }
 
    @Override
    public boolean isEmpty(){
        return avl.isEmpty();
    }
 
    @Override
    public void add(E e){
        avl.add(e, null);
    }
 
    @Override
    public boolean contains(E e){
        return avl.contains(e);
    }
 
    @Override
    public void remove(E e){
        avl.remove(e);
    }
}
```

```java
public interface Set<E> {
 
    void add(E e);
    boolean contains(E e);
    void remove(E e);
    int getSize();
    boolean isEmpty();
}
```

```java
import java.util.ArrayList;
 
public class TestSetMain {
 
    private static double testSet(Set<String> set, String filename){
 
        long startTime = System.nanoTime();
 
        System.out.println(filename);
        ArrayList<String> words = new ArrayList<>();
        if(FileOperation.readFile(filename, words)) {
            System.out.println("Total words: " + words.size());
 
            for (String word : words)
                set.add(word);
            System.out.println("Total different words: " + set.getSize());
        }
 
        long endTime = System.nanoTime();
 
        return (endTime - startTime) / 1000000000.0;
    }
 
    public static void main(String[] args) {
 
        String filename = "pride-and-prejudice.txt";
 
        BSTSet<String> bstSet = new BSTSet<>();
        double time1 = testSet(bstSet, filename);
        System.out.println("BST Set: " + time1 + " s");
 
        System.out.println();
 
        LinkedListSet<String> linkedListSet = new LinkedListSet<>();
        double time2 = testSet(linkedListSet, filename);
        System.out.println("Linked List Set: " + time2 + " s");
 
        System.out.println();
 
        AVLSet<String> avlSet = new AVLSet<>();
        double time3 = testSet(avlSet, filename);
        System.out.println("AVL Set: " + time3 + " s");
    }
}
```


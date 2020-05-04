package internet_chess;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class ChessPanel extends JPanel implements Runnable{
    private int BLACKCHESS = 1;
    private int WHITECHESS = 0;//黑1，白0
    public Chess chess[] = new Chess[44];//创建了44个棋子对象
    private int width = 40;
    private int height = 40;
    public int Row = 7;
    public int Col = 11;//9行5列
    public int map[][] = new int [Row][Col];
    public int player;//设置当前玩家对象
    private boolean isFirst = false;//判断是否是第一次点击的棋子，以此分开两次点击棋子的碰撞矛盾
    //进行移动时，选择两组坐标
    private int x1, y1;//第一组，记录ToMove的坐标
    private int x2, y2;//第二组，记录MoveTo的坐标
    private boolean flag = true;//用来控制线程的运行
    private boolean isPlay = false;//回合
    private boolean eating = false;//状态
    private boolean judge = false;//判断胜负
    private Chess firstChess = null;//

    private InetAddress myID;//自己id地址
    private InetAddress youID;//目标ID地址
    private int sendport;//发送端口
    private int receiveport = 8888;//接收端口

    public boolean getEating(){
        return eating;
    }
    public ChessPanel()//构造函数
    {
        init_map();//初始化棋盘
        //给这个面板添加鼠标监听机制
        this.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                if(isPlay == true)//判断是否该本方下棋
                {
                    SelectChess(e.getPoint());//选择要移动的棋子
                    repaint();
                }
            }

            public void SelectChess(Point pos)//行走
            {
                int x = pos.x;
                int y = pos.y;//获取此时此刻鼠标点击的位置坐标
                System.out.println("选择要移动的棋子坐标：x->"+x+"  y->"+y);
                if(x > 0 && x < (Col-1)*width && y > 0 && y < (Row-1)*height)//判断鼠标是否在合理的范围，不在就直接退出
                {
                    Point point = ReSetID(x,y);

                    if(isFirst)//判断是否是第一次选中的棋子
                    {
                        x1 = point.x;
                        y1 = point.y;
                        //判断第一次选中的棋子是不是自己的棋子或者是不是无效棋子，不是就失败
                        int id = map[x1][y1];
                        if(id != -1 && chess[id].owner == player)
                        {
                            isFirst = false;
                            firstChess = chess[id];
                            System.out.println("id->"+id);
                        }
                        else//第一次选择的棋子无效
                        {
                            JOptionPane.showConfirmDialog(null, "提示", "第一次选棋无效！请重新选择！", JOptionPane.OK_OPTION);
                            isFirst = true;
                        }
                    }
                    else//第二次选中的棋子
                    {//原版本bug太多，重写
                        //三个boolen:eating,isFirst,isPlay
                        //判断，是否为-1
                        x2 = point.x;
                        y2 = point.y;
                        int id = map[x2][y2];
                        if(!eating)//没有在吃
                        {
                            if(id==-1){
                                if(CanEat()){//能否吃，如果可以
                                    if(IsEatChess(firstChess)&&IsEatAble(firstChess,x2,y2)) {//吃棋，吃完后还可以吃//递归
                                        EatChess(firstChess,x2,y2);//吃棋
                                        if(IsEatChess(firstChess))//如果还可以吃
                                        {
                                            isFirst = false;//已拥有第一个棋子
                                            eating = true;//正在吃棋
                                        }
                                        else {//否则回合结束
                                            Cancle44();
                                            repaint();
                                            isFirst = true;
                                            isPlay = false;
                                            if(!judgeVictory())
                                            send("end|",youID,sendport);//回合结束
                                        }
                                    }
                                }
                                else {//否则进行移动
                                    if(IsMoveChess(firstChess,x2,y2))//如果可移动，则移动
                                    {
                                        int idx1 = map[x1][y1];
                                        map[x1][y1] = 44;
                                        map[x2][y2] = idx1;
                                        firstChess.SetPos(x2, y2);
                                        send("move|"+String.valueOf(idx1)+"|"+String.valueOf(Row-1-x2)+"|"+String.valueOf(Col-1-y2)+"|",youID,sendport);
                                        System.out.println("移动棋子:目标-》"+(Row-1-x2)+"  "+(Col-1-y2));
                                        Cancle44();
                                        repaint();
                                        isFirst = true;
                                        isPlay = false;
                                        if(!judgeVictory())
                                        send("end|",youID,sendport);//回合结束
                                    }
                                }
                            }
                            else {//如果不为空。则必为己方棋子
                                if((id!=44&& chess[id].owner == player))//将第二次选择的棋子当做第一次选择的棋子
                                {
                                    firstChess = chess[id];
                                    x1 = x2;
                                    y1 = y2;
                                    isFirst = false;
                                }
                            }
                        }
                        else{//正在吃
                            if(IsEatAble(firstChess, x2, y2)){
                                EatChess(firstChess,x2,y2);//吃棋
                                if(!IsEatChess(firstChess))//如果不能吃了
                                {
                                    Cancle44();
                                    repaint();
                                    eating = false;
                                    isFirst = true;
                                    isPlay = false;
                                    if(!judgeVictory())
                                    send("end|",youID,sendport);//回合结束
                                }
                            }
                        }
                        //bug 过多，已重写
                        /*if(eating&&!IsEatChess(firstChess)){//判断结束吃子
                            //eating==true必有isFirst==false
                            Cancle44();
                            repaint();
                            isFirst = true;
                            isPlay = false;//装让控制权
                            eating = false;
                            send("end|",youID,sendport);
                        }
                        else {
                            x2 = point.x;
                            y2 = point.y;
                            int id = map[x2][y2];
                            if(id != -1 &&id!=44&& chess[id].owner != player)//第二次选择了敌方棋子，不可选
                            {
                                JOptionPane.showConfirmDialog(null, "提示", "对不起，移动棋子失败，请重新选择目标！", JOptionPane.ERROR_MESSAGE);
                                isFirst = false;
                            }
                            else if((!eating)&&id != -1 &&id!=44&& chess[id].owner == player)//第二次又选择了自己的棋子，那么就将第二次选择的棋子当做第一次选择的棋子
                                //当正在吃棋时，不会执行此项
                            {
                                firstChess = chess[id];
                                x1 = x2;
                                y1 = y2;
                                isFirst = false;
                            }
                            else if(id==44) {
                                isFirst = false;
                                firstChess = chess[id];
                                JOptionPane.showConfirmDialog(null, "提示", "对不起，不能移动到本回合移动过的地方！", JOptionPane.ERROR_MESSAGE);
                            }
                            else//第二次选择的棋子是空，移动棋子//下属函数无必要判断是否为空
                            {
                                secondChess = null;
                                if(CanEat()){
                                    if(IsEatChess(firstChess)&&IsEatAble(firstChess,x2,y2)) {//吃棋，吃完后还可以吃//递归
                                        EatChess(firstChess,x2,y2);
                                        if(IsEatChess(firstChess))//如果还可以吃
                                        {
                                            isFirst = false;//已拥有第一个棋子
                                            eating = true;//正在吃棋
                                        }
                                        else {
                                            Cancle44();
                                            repaint();
                                            isFirst = true;
                                            isPlay = false;//装让控制权
                                            eating = false;
                                            judgeVictory();
                                            send("end|",youID,sendport);
                                        }
                                    }
                                    else if(!IsEatChess(firstChess)&&eating){
                                        Cancle44();
                                        repaint();
                                        isFirst = true;
                                        isPlay = false;//装让控制权
                                        eating = false;
                                        judgeVictory();
                                        send("end|",youID,sendport);
                                    }
                                }
                                else if(!CanEat())
                                    if(IsMoveChess(firstChess,x2,y2))//如果没有棋子可以吃，则移动
                                {
                                    int idx1 = map[x1][y1];
                                    map[x1][y1] = 44;
                                    map[x2][y2] = idx1;
                                    firstChess.SetPos(x2, y2);
                                    send("move|"+String.valueOf(idx1)+"|"+String.valueOf(Row-1-x2)+"|"+String.valueOf(Col-1-y2)+"|",youID,sendport);
                                    System.out.println("移动棋子:目标-》"+(Row-1-x2)+"  "+(Col-1-y2));
                                    Cancle44();
                                    repaint();
                                    isFirst = true;
                                    isPlay = false;
                                    judgeVictory();
                                    send("end|",youID,sendport);
                                }
                            }
                        }*/
                    }
                }
            }
        });
    }
    public boolean judgeVictory(){
        judge = false;
        for(Chess i :chess){
            if(i!=null&&i.owner!=player) judge = true;}
        if(!judge){
            send("success",youID,sendport);
            JOptionPane.showConfirmDialog(null, "游戏结束", "游戏结束！", JOptionPane.OK_OPTION);
        }
        return !judge;//judge==true.没结束
    }
    public boolean CanEat(){
        boolean flag = false;
        for(Chess i:chess)
            if(i!=null&&i.owner==player&&IsEatChess(i)) flag = true;
        //判断是否可以吃棋，只要有一个棋子可以吃棋，就必须吃
        return flag;
    }
    public void Cancle44(){//清除轨道标记
        for(int i=0;i<Row;i++)
            for(int j=0;j<Col;j++)
                if(map[i][j]==44) map[i][j] = -1;
    }
    //这里可以独立出一个class
    public void EatChess(Chess chessA,int x2, int y2){//A移动到x2,y2的位置，并吃子

        if (x2 < 1 && x2 > 5 && y2 < 1 && y2 > 9) {return;}//判断越界
        int x1 = chessA.point.x;
        int y1 = chessA.point.y;//toMove.point
        int id = map[x2][y2];//MoveTo
        //isFirst = true;我是个傻逼
        //开始判断是否可以移动棋子，如果可以就将棋子移动，并发信息给对方我们已经移动的棋子信息
        //判断是否可以移动棋子
        if(IsEatAble(firstChess,x2,y2))//可以吃
        {
            //移动
            int idx1 = map[x1][y1];
            map[x1][y1] = 44;//设为轨迹
            map[x2][y2] = idx1;//将棋子移动到x2,y2
            firstChess.SetPos(x2, y2);
            send("move|"+String.valueOf(idx1)+"|"+String.valueOf(Row-1-x2)+"|"+String.valueOf(Col-1-y2)+"|",youID,sendport);
            System.out.println("移动棋子:目标-》"+(Row-1-x2)+"  "+(Col-1-y2));


            //一条轨迹上所有【相邻】的敌方棋子，都会被吃掉
            //怎样得知这些棋子在这条直线上呢？
            //递归!public void Eat
            //吃子
            Eat(x1,y1,x2,y2);//正向吃
            Eat(x2,y2,x1,y1);//反向吃
        }
    }
    public void Eat(int x0, int y0, int x, int y){
        //x0,y0为第一个坐标，x,y为第二个坐标，被吃的是第三个坐标
        int xx = 2*x-x0;
        int yy = 2*y-y0;
        if(xx<1||xx>5||yy<1||yy>9) return;//越界，则返回
        int id = map[xx][yy];
        //if(id!=-1&&id!=44&&chess[id]==null||chess[id].owner==player) return;//不是棋子，或者是玩家的棋子，则返回
        if(id==-1||id==44||chess[id]==null||chess[id].owner==player) return;//无视上面的傻逼代码
        //我是个智障
        //无比确信
        chess[id] = null;//移除
        map[xx][yy] = -1;//我是傻逼
        send("eat|"+String.valueOf(id)+"|",youID,sendport);
        Eat(x,y,xx,yy);
    }
    public boolean IsEatChess(Chess chessA){//判断棋子是否可以吃棋
        boolean eatAble = false;
        //一共有八种移动可能
        //这里使用穷举
        int x = chessA.point.x;
        int y = chessA.point.y;
        var ids = new int[][] {{x+1,y+1},{x+1,y},{x+1,y-1},{x,y+1},{x,y-1},{x-1,y+1},{x-1,y},{x-1,y-1}};
        for(int[] ints:ids){
            int xx = ints[0];
            int yy = ints[1];
            if( xx<1||yy<1||xx>5||yy>9) continue;//判断越界
            if (!IsMoveChess(chessA, xx, yy)) continue;
            if(0<(2*xx-x)&&(2*xx-x)<6&&0<(2*yy-y)&&(2*yy-y)<10){
                int id = map[2*xx-x][2*yy-y];//
                if(id!=-1&&id!=44&&chess[id]!=null && chess[id].owner != player) {
                    eatAble = true;
                    break;
                }
            }
            if(1<=2*x-xx&&2*x-xx<=5&&2*y-yy>=1&&2*y-yy<=9){//判断越界
                int id = map[2*x-xx][2*y-yy];
                if(id!=-1&&id!=44&&chess[id]!=null && chess[id].owner != player) {
                    eatAble = true;
                    break;
                }
            }

        }
        return eatAble;
    }
    public boolean IsEatAble(Chess chessA, int x,int y){//判断移动后是否可以吃棋||x,y为MoveTo
        int x0 = chessA.point.x;
        int y0 = chessA.point.y;//当前坐标
        if (!IsMoveChess(chessA, x, y)) return false;//不能移动，则不可吃棋
        if(1<=x+x-x0&&x+x-x0<=5&&y+y-y0>=1&&y+y-y0<=9){//撞吃
                int id1 = map[x+x-x0][y+y-y0];
                if(id1!=-1&&id1!=44&&chess[id1]!=null && chess[id1].owner != player) return true;//存在且不属于玩家
            }
        if(1<=x0+x0-x&&x0+x0-x<=5&&y0+y0-y>=1&&y0+y0-y<=9){//拖吃
            int id2 = map[x0+x0-x][y0+y0-y];
            if(id2!=-1&&id2!=44&&chess[id2]!=null && chess[id2].owner != player) return true;
        }
        return false;
    }

    public boolean IsMoveChess(Chess chess,int x,int y)//判断是否可以将棋子移动到x,y||x,y为MoveTo
    {
        int id = map[x][y];
        if(id!=-1) return false;//非空格不可挪动
        if (x >= 1 && x <= 5 && y >= 1 && y <= 9) {
            int x0 = chess.point.x;
            int y0 = chess.point.y;
            int xMove = Math.abs(x-x0);
            int yMove = Math.abs(y-y0);
            if (xMove*yMove == 1&&(x0+y0)%2==0){//在X字格上，允许斜走
                return true;
            }else if(xMove==0&&yMove==1) return true;//允许直走
            else if(xMove==1&&yMove==0) return true;//允许直走
            else return false;
        } else//越界
            return false;
    }
    public Point ReSetID(int x, int y)//重置id,将id转化成可辨识的坐标信息
    {
        int posx = (y+height/2)/height;
        int posy = (x+width/2)/width;
        return new Point(posx,posy);
    }

    public void init_map()//初始化棋盘
    {
        for(int i = 0; i < Row; i++)
        {
            for(int j = 0; j < Col; j++)
            {
                map[i][j] = -1;//将棋盘初始化为-1，表示没有棋子id
            }
        }
    }

    public void paint(Graphics g)//绘制棋盘
    {
        super.paint(g);
        g.clearRect(0,0,this.getWidth(),this.getHeight());
        for(int j = 1; j < Row-1; j++)//横线
        {
            g.drawLine(width, j*height, (Col-2)*width, j*height);
        }
        for(int i = 1; i < Col-1; i++)//竖线
        {
            g.drawLine(i*width, height, i*width, (Row-2)*height);
        }
        for (int i = 2 ;i<Col-2;i++)//斜线
            for(int j = 2; j < Row-2; j++) {
                if((i+j)%2==0){
                    g.drawLine((i-1)*width,(j-1)*height,(i+1)*width,(j+1)*height);
                    g.drawLine((i+1)*width,(j-1)*height,(i-1)*width,(j+1)*height);
                }
            }
        for(int i = 0; i < chess.length; i++)
        {
            if(chess[i] != null)
            {
                chess[i].paint(g, this);
                if((!eating)&&chess[i].owner==player&&IsEatChess(chess[i])){
                    chess[i].paintSeclected(g);
                }
            }
        }
        if(eating&&IsEatChess(firstChess)){
            firstChess.paintSeclected(g);
        }
    }

    public void send(String str,InetAddress ip,int port) //发送数据报
    {
        DatagramSocket s = null;
        try{
            s = new DatagramSocket();//创建一个数据报套接字
            byte data[] = new byte[100];
            data = str.getBytes();
            DatagramPacket pocket = new DatagramPacket(data,data.length,ip,port);//将数据报的信息放入自寻址包中,自寻址信息包括数据，数据长度，目标ip地址，目标端口号
            s.send(pocket);//发送自寻址包
            System.out.println("发送信息："+str);
        }catch(IOException ex)
        {
            ex.printStackTrace();
        }finally
        {
            if(s != null)
                s.close();
        }
    }

    public void startgame(InetAddress ip, int otherport, int myport)//游戏正式开始的起点入口
    {
        youID = ip;
        this.sendport = otherport;
        this.receiveport = myport;
        try{
            myID = InetAddress.getLocalHost();
        }catch(UnknownHostException ex)
        {
            ex.printStackTrace();
        }
        send("play|",youID,sendport);//发送邀请，等待目标ip的回应----开启一个线程，不断监听端口，检查是否有消息，是否建立连接成功
        Thread t = new Thread(this);
        t.start();
    }
//无bug
    public void FirstPaintChess()//初始化棋盘
    {
        init_map();//重新加载
        paintChess();
        if(player == BLACKCHESS)
        {
            ReverseChess();//黑棋转置棋盘
        }
        repaint();
    }
//无bug
    public void ReverseChess()//转置棋盘
    {
        for(int i = 0; i < 44; i++)
        {
            if(chess[i] != null)
            {
                chess[i].ReversePos();
                int xx = chess[i].point.x;
                int yy = chess[i].point.y;
                map[xx][yy] = i;
            }
        }
    }
//无bug
    public void paintChess()//初始化棋子
    {
        for (int i=0;i<9;i++) {
            chess[i] = new Chess(BLACKCHESS, new Point(1,i+1));
            map[1][i+1] =i;
        }
        for (int i=9;i<18;i++) {
            chess[i] = new Chess(BLACKCHESS, new Point(2,i-8));
            map[2][i-8] =i;
        }
        for( int i=18, j = 1; i<20;i++, j+=2){
            chess[i] = new Chess( BLACKCHESS, new Point(3, j));
            map[3][j] = i;
        }
        for( int i=20, j = 6; i<22;i++, j+=2){
            chess[i] = new Chess( BLACKCHESS, new Point(3, j));
            map[3][j] = i;
        }
        for( int i=22;i<31;i++){
            chess[i] = new Chess( WHITECHESS, new Point(4, i-21));
            map[4][i-21] = i;
        }
        for( int i=31;i<40;i++){
            chess[i] = new Chess( WHITECHESS, new Point(5, i-30));
            map[5][i-30] = i;
        }
        for(int i = 40, j = 2; i < 42; i++, j+=2) {
            chess[i] = new Chess( WHITECHESS,new Point(3,j));
            map[3][j] = i;
        }
        for(int i = 42, j = 7; i < 44; i++, j+=2) {
            chess[i] = new Chess( WHITECHESS,new Point(3,j));
            map[3][j] = i;
        }
    }
//
    @Override
    public void run()//数据接收
    {
        DatagramSocket sock = null;
        try {
            sock = new DatagramSocket(receiveport);//打开监听窗口
            byte data[] = new byte[100];
            DatagramPacket pocket = new DatagramPacket(data,data.length);
            while(flag)
            {
                sock.receive(pocket);//接收数据
                //读取接收信息
                String str = new String(data);
                String s[] = new String[6];
                s = str.split("\\|");//将数据信息按照|进行分割
                if(s[0].equals("play"))//表示此时这个对象是一个被邀请的对象，将被邀请的对象设置为黑棋
                {
                    player = BLACKCHESS;//被邀请者设为黑棋
                    send("connect|",youID,sendport);
                    //开始画棋盘
                    FirstPaintChess();
                    isPlay = false;//因为是白棋先走，所以黑棋此时不能下棋
                }
                else if(s[0].equals("connect"))//表示此时的对象是游戏发出者对象，并且已经和被邀请对象建立连接
                {
                    player = WHITECHESS;//游戏发出者设为白棋对象
                    FirstPaintChess();
                    isPlay = true;//因为此时是白棋，而白旗先走，所以白棋此时可以下棋
                }
                else if(s[0].equals("lose"))//认输
                {
                    JOptionPane.showConfirmDialog(null, "对方已认输，游戏结束", "游戏结束！", JOptionPane.OK_OPTION);
                    isPlay = false;
                }
                else if(s[0].equals("success"))//
                {
                    JOptionPane.showConfirmDialog(null, "游戏结束", "游戏结束！", JOptionPane.OK_OPTION);
                    isPlay = false;
                }
                else if(s[0].equals("eat"))//对方吃棋
                {
                    int indx = Integer.parseInt(s[1]);
                    int x = chess[indx].point.x;
                    int y = chess[indx].point.y;
                    chess[indx] = null;
                    map[x][y] = -1;
                    repaint();
                }
                else if(s[0].equals("end"))//回合结束
                {
                    Cancle44();
                    repaint();
                    eating = false;
                    isPlay = true;
                    isFirst = true;
                    JOptionPane.showConfirmDialog(null, "提示", "到你了!", JOptionPane.OK_OPTION);
                }
                else if(s[0].equals("move"))//对方走棋
                {
                    int indx = Integer.parseInt(s[1]);
                    System.out.println("indx->"+indx);
                    int posx = Integer.parseInt(s[2]);
                    System.out.println("posx->"+posx);
                    int posy = Integer.parseInt(s[3]);
                    System.out.println("posy->"+posy);
                    int x = chess[indx].point.x;
                    int y = chess[indx].point.y;
                    map[x][y] = -1;
                    chess[indx].point.x = posx;
                    chess[indx].point.y = posy;
                    if(map[posx][posy] != -1)
                    {
                        chess[map[posx][posy]] = null;
                    }
                    map[posx][posy] = indx;
                }
                else if(s[0].equals("quit"))//对方退出
                {
                    JOptionPane.showConfirmDialog(null, "提示", "对方离开，游戏结束！", JOptionPane.OK_OPTION);
                    isPlay = false;
                    flag = false;//退出线程
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally
        {
            if(sock != null)
            {
                sock.close();
            }
        }

    }
}
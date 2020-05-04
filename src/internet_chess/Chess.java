package internet_chess;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.ImageObserver;

import javax.swing.JPanel;

public class Chess {
    public int owner;//当前棋子对象的所有者--黑方还是红方
    public Point point;//当前棋子对象的位置
    public Image chessImage;//当前棋子对象的图像
    private final static int BLACKCHESS = 1;
    private final static int WHITECHESS = 0;//红方0，黑方1
    private final static int width = 40;
    private final static int height = 40;
    public final static int Row = 7;
    public final static int Col = 11;

    public Chess(int owner,Point point)//获取每一个棋子对象名字，所有者，位置，和图片信息
    {
        this.owner = owner;
        this.point = point;
        if(owner == BLACKCHESS)//如果所有者是黑方
        {
            chessImage = Toolkit.getDefaultToolkit().getImage("Image/chessBlack.png");
        }
        else if(owner == WHITECHESS)
        {
            chessImage = Toolkit.getDefaultToolkit().getImage("Image/chessWhite.png");
        }
    }

    protected void paint(Graphics g,JPanel i)//画棋子
    {
        g.drawImage(chessImage, point.y*width-width/2+10, point.x*height-height/2+10, width/2, height/2, (ImageObserver)i);
    }

    protected void paintSeclected(Graphics g)//画鼠标选择了以后的棋子对象的边框
    {
        g.drawRect(point.y*width-width/2, point.x*height-height/2, width, height);
    }

    public void SetPos(int x, int y)//重新设置移动以后的棋子对象的位置坐标
    {
        point.x = x;
        point.y = y;
    }

    public void ReversePos()//将该对象的位置坐标逆置输出，用于方便显示信息情况
    {
        point.x = Row-1 - point.x;
        point.y = Col-1 - point.y;
    }
}
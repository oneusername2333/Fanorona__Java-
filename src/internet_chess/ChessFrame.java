package internet_chess;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.swing.*;

public class ChessFrame extends JFrame{
    private JButton start = new JButton("开始");
    private JButton end = new JButton("结束");
    private JButton lose = new JButton("认输");
    private JButton jump = new JButton("跳过");
    private JPanel paneUp = new JPanel();
    private ChessPanel chesspanel = new ChessPanel();
    private JPanel paneDown = new JPanel();
    private JLabel IPlabel = new JLabel("IP：");
    private JLabel otherPortlabel = new JLabel("目标端口");
    private JLabel imageicon = new JLabel();
    private JTextField ip_address = new JTextField("127.0.0.1");
    private JTextField otherPort = new JTextField("8888");

    private InetAddress myID;//自己id地址
    private InetAddress youID;//目标ID地址
    private int sendport;//发送端口
    private int receiveport = 8888;//接收端口


    public ChessFrame()//构造函数
    {
        paneDown.setLayout(new FlowLayout());
        IPlabel.setBounds(10, 10, 40, 20);
        ip_address.setBounds(new Rectangle(60,10,50,20));
        paneDown.add(IPlabel);
        paneDown.add(ip_address);
        paneDown.add(otherPortlabel);
        paneDown.add(otherPort);
        paneDown.add(start);
        paneDown.add(lose);
        paneDown.add(end);
        paneDown.add(jump);
        lose.setEnabled(false);

        imageicon.setBounds(new Rectangle(300,0,100,100));

        Container con = this.getContentPane();
        con.add(paneUp,BorderLayout.NORTH);
        con.add(chesspanel,BorderLayout.CENTER);
        con.add(paneDown,BorderLayout.SOUTH);

        this.setTitle("马达加斯加迂棋");
        this.setSize(new Dimension(600,400));
        this.setVisible(true);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        start.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                try {
                    String ip = ip_address.getText();//获取当前目标ip地址
                    sendport = Integer.parseInt(otherPort.getText());//获取目标连接端口
                    myID = InetAddress.getLocalHost();//获取本地ip地址
                    youID = InetAddress.getByName(ip);//获取目标ip地址
                } catch (UnknownHostException e1) {
                    e1.printStackTrace();
                }
                chesspanel.startgame(youID,sendport,receiveport);
                lose.setEnabled(true);
            }
        });

        end.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                try{
                    chesspanel.send("quit|",youID,sendport);//向对方发送离开信息，同时断开连接
                    System.exit(0);
                }catch(Exception ex)
                {
                    ex.printStackTrace();
                }
            }
        });

        lose.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                try{
                    chesspanel.send("lose|",youID,sendport);//向对方发送认输信息
                }catch(Exception ex)
                {
                    ex.printStackTrace();
                }
            }
        });
        jump.addMouseListener(new MouseAdapter()
    {
        public void mouseClicked(MouseEvent e)
        {
            if(chesspanel.getEating()){
                try{
                    chesspanel.send("end|",youID,sendport);//向对方发送认输信息
                }catch(Exception ex)
                {
                    ex.printStackTrace();
                }}
            else {
                JOptionPane.showConfirmDialog(null, "要想跳过本回合，你需要先走至少一步！", "提示", JOptionPane.OK_OPTION);
            }
        }
    });
    }

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        new ChessFrame();
    }

}
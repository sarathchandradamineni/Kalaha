 package ai;

import ai.Global;
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import kalaha.*;

/**
 * This is the main class for your Kalaha AI bot. Currently
 * it only makes a random, valid move each turn.
 * 
 * @author Johan Hagelbäck
 */
public class AIClient implements Runnable
{
    int rootnodeplayer;
    class Node
    {
        Node n1;
        Node n2;
        Node n3;
        Node n4;
        Node n5;
        Node n6;
        int ambo[];
        int eval_of_each_child_ambo[];
        int ambo_used;
        int player;
        int e_val;
        int e_val_obtained_from_child;
        int e_val_from_ambo = 0;
        int level;
        int freechance = 0;//setting the root node doesnot have a free chance to play again
        int alpha_beta_eval;
        int alpha;
        int beta;
        public Node()
        {
            ambo = new int[14];
            eval_of_each_child_ambo = new int[7];
            n1 = null;
            n2 = null;
            n3 = null;
            n4 = null;
            n5 = null;
            n6 = null;
        }  
    }
    private int player;
    private JTextArea text;
    
    private PrintWriter out;
    private BufferedReader in;
    private Thread thr;
    private Socket socket;
    private boolean running;
    private boolean connected;
    	
    /**
     * Creates a new client.
     */
    public AIClient()
    {
	player = -1;
        connected = false;
        
        //This is some necessary client stuff. You don't need
        //to change anything here.
        initGUI();
	
        try
        {
            addText("Connecting to localhost:" + KalahaMain.port);
            socket = new Socket("localhost", KalahaMain.port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            addText("Done");
            connected = true;
        }
        catch (Exception ex)
        {
            addText("Unable to connect to server");
            return;
        }
    }
    
    /**
     * Starts the client thread.
     */
    public void start()
    {
        //Don't change this
        if (connected)
        {
            thr = new Thread(this);
            thr.start();
        }
    }
    
    /**
     * Creates the GUI.
     */
    private void initGUI()
    {
        //Client GUI stuff. You don't need to change this.
        JFrame frame = new JFrame("My AI Client");
        frame.setLocation(Global.getClientXpos(), 445);
        frame.setSize(new Dimension(420,250));
        frame.getContentPane().setLayout(new FlowLayout());
        
        text = new JTextArea();
        JScrollPane pane = new JScrollPane(text);
        pane.setPreferredSize(new Dimension(400, 210));
        
        frame.getContentPane().add(pane);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        frame.setVisible(true);
    }
    
    /**
     * Adds a text string to the GUI textarea.
     * 
     * @param txt The text to add
     */
    public void addText(String txt)
    {
        //Don't change this
        text.append(txt + "\n");
        text.setCaretPosition(text.getDocument().getLength());
    }
    
    /**
     * Thread for server communication. Checks when it is this
     * client's turn to make a move.
     */
    public void run()
    {
        String reply;
        running = true;
        
        try
        {
            while (running)
            {
                //Checks which player you are. No need to change this.
                if (player == -1)
                {
                    out.println(Commands.HELLO);
                    reply = in.readLine();

                    String tokens[] = reply.split(" ");
                    player = Integer.parseInt(tokens[1]);
                        
                    addText("I am player " + player);
                }
                
                //Check if game has ended. No need to change this.
                out.println(Commands.WINNER);
                reply = in.readLine();
                if(reply.equals("1") || reply.equals("2") )
                {
                    int w = Integer.parseInt(reply);
                    if (w == player)
                    {
                        addText("I won!");
                    }
                    else
                    {
                        addText("I lost...");
                    }
                    running = false;
                }
                if(reply.equals("0"))
                {
                    addText("Even game!");
                    running = false;
                }

                //Check if it is my turn. If so, do a move
                out.println(Commands.NEXT_PLAYER);
                reply = in.readLine();
                if (!reply.equals(Errors.GAME_NOT_FULL) && running)
                {
                    int nextPlayer = Integer.parseInt(reply);

                    if(nextPlayer == player)
                    {
                        out.println(Commands.BOARD);
                        String currentBoardStr = in.readLine();
                        boolean validMove = false;
                        while (!validMove)
                        {
                            long startT = System.currentTimeMillis();
                            //This is the call to the function for making a move.
                            //You only need to change the contents in the getMove()
                            //function.
                            GameState currentBoard = new GameState(currentBoardStr);
                            int cMove = getMove(currentBoard);
                            
                            //Timer stuff
                            long tot = System.currentTimeMillis() - startT;
                            double e = (double)tot / (double)1000;
                            
                            out.println(Commands.MOVE + " " + cMove + " " + player);
                            reply = in.readLine();
                            if (!reply.startsWith("ERROR"))
                            {
                                validMove = true;
                                addText("Made move " + cMove + " in " + e + " secs");
                            }
                        }
                    }
                }
                
                //Wait
                Thread.sleep(100);
            }
	}
        catch (Exception ex)
        {
            running = false;
        }
        
        try
        {
            socket.close();
            addText("Disconnected from server");
        }
        catch (Exception ex)
        {
            addText("Error closing connection: " + ex.getMessage());
        }
    }
    
    /**
     * This is the method that makes a move each time it is your turn.
     * Here you need to change the call to the random method to your
     * Minimax search.
     * 
     * @param currentBoard The current board state
     * @return Move to make (1-6)
     */
    public int getMove(GameState currentBoard)
    {
        Node node = new Node();
        int cur_player = currentBoard.getNextPlayer();
        
        //iterative deepening
        // Depending on the number of non empty nodes we will set the level for the tree
        Node root_node = new Node(); // Declaration of the root node to get number of non empty ambos
        root_node = boardState(root_node,currentBoard); //Initialization of this node from the board
        
        int number_of_non_empty_ambos = 0;
        
        if(cur_player == 1)
        {
            for(int i = 1; i <= 6;i++)
            {
                root_node.ambo[i] = currentBoard.getSeeds(i,1);
                
                if(root_node.ambo[i] != 0)
                    number_of_non_empty_ambos++;
            }
        }
        else
        {
            for(int i = 1 ; i <= 6;i++)
            {
                root_node.ambo[i] = currentBoard.getSeeds(i,2);
                
                if(root_node.ambo[i] != 0)
                    number_of_non_empty_ambos++;
            }
        }

        System.out.println("number of non empty ambos "+number_of_non_empty_ambos);
           
        if(number_of_non_empty_ambos == 6)
        {
            //if the number of non empty ambos are 6 then tree with 7 levels will be generated
            node = treeCreation(null,cur_player,0,7,currentBoard);
        }
        else if(number_of_non_empty_ambos == 5)
        {
            //if the number of non empty ambos are 5 then tree with 8 levels will be generated
            node = treeCreation(null,cur_player,0,8,currentBoard);
        }
        else if(number_of_non_empty_ambos == 4)
        {
            //if the number of non empty ambos are 4 then tree with 9 levels will be generated
            node = treeCreation(null,cur_player,0,9,currentBoard);
        }
        else if(number_of_non_empty_ambos == 3)
        {
            //if the number of non empty ambos are 3 then tree with 10 levels will be generated
            node = treeCreation(null,cur_player,0,10,currentBoard);
        }
        else if(number_of_non_empty_ambos == 2)
        {
            //if the number of non empty ambos are 2 then tree with 10 levels will be generated
            node = treeCreation(null,cur_player,0,10,currentBoard);
        }
        else if(number_of_non_empty_ambos == 1)
        {
            //if the number of non empty ambos are 1 then tree with 10 levels will be generated
            node = treeCreation(null,cur_player,0,10,currentBoard);
        }
 
        return node.e_val_from_ambo;       
    }
    
    public Node treeCreation(Node parent_node,int cur_player,int ambo_selected,int level,GameState currentBoard)
    {
        
        //System.out.println("new iteration");
        
        Node node = new Node();
        
        if(parent_node == null && ambo_selected == 0) //root node creation
        {
            node = boardState(node,currentBoard);
            node.level = level;
            node.player = cur_player;
            //System.out.println("root node");
            rootnodeplayer = cur_player;
            node.alpha_beta_eval = -200;
            node.alpha = -200;
            node.beta = 200;
        }
        else
        { 
            node = nodeForm(parent_node,cur_player,ambo_selected,currentBoard);
            
            if(node == null)
            {
                //System.out.println("null node");
                return null;
            }
            else
            {
                if(node.freechance == 1)
                {
                    node.level = level;
                    //System.out.println("free chance is existed for this node");
                    level = level+1;
                    node.player = cur_player;
                }
                else
                {
                    cur_player = playerInterchange(cur_player);
                    node.player = cur_player;
                    node.level = level;
                }
                
                node.alpha = parent_node.alpha;
                node.beta = parent_node.beta;
                
                if(node.player == rootnodeplayer)
                {
                    node.alpha_beta_eval = -200;
                }
                else
                {
                    node.alpha_beta_eval = 200;
                } 
            }
        }
        
        //nodeDisplay(node);
        
        level = level - 1;
        //cur_player = playerInterchange(cur_player);
        
        if(level >= 0)
        {
            
            if(node.player == rootnodeplayer)  //maximizing player this is for alpha beta pruning
            {
                //System.out.println("player 1 code");
                
                if(node.alpha_beta_eval <= node.beta)  
                {
                    node.n1 = treeCreation(node,cur_player,1,level,currentBoard);
                    
                    if(node.n1 == null)
                    {
                        node.eval_of_each_child_ambo[1] = -100;
                    }
                    else   
                    {
                        node.eval_of_each_child_ambo[1] = node.n1.e_val;
                    
                        if(node.n1.level == 0) //if we dealing with leaf node  
                        {
                            node.n1.alpha_beta_eval = node.n1.e_val;
                        }
                

                        if(node.alpha_beta_eval <= node.n1.alpha_beta_eval)
                        {
                            node.alpha_beta_eval = node.n1.alpha_beta_eval;
                        } 
                    
                        node.alpha = node.n1.alpha_beta_eval;         
                    }
                }
                else
                {
                    node.eval_of_each_child_ambo[1] = -100;
                    //System.out.println("pruning implemented");
                }
                
                if(node.alpha_beta_eval <= node.beta)
                {
                    node.n2 = treeCreation(node,cur_player,2,level,currentBoard);
                                       
                    if(node.n2 == null)
                    {
                        node.eval_of_each_child_ambo[2] = -100;
                    }
                    else
                    {
                        node.eval_of_each_child_ambo[2] = node.n2.e_val;
                            
                        if(node.n2.level == 0)
                        {
                                node.n2.alpha_beta_eval = node.n2.e_val;
                        }
                            
                        if(node.alpha_beta_eval <= node.n2.alpha_beta_eval)
                        {
                            node.alpha_beta_eval = node.n2.alpha_beta_eval;
                        }
                    
                            node.alpha = node.n2.alpha_beta_eval;
                        }
                }
                else
                {
                    node.eval_of_each_child_ambo[2] = -100;
                   // System.out.println("pruning implemented");
                }
                
                
                if(node.alpha_beta_eval <= node.beta)
                {
                    node.n3 = treeCreation(node,cur_player,3,level,currentBoard);
                    if(node.n3 == null)
                    {
                        node.eval_of_each_child_ambo[3] = -100;
                    }
                    else
                    {
                        node.eval_of_each_child_ambo[3] = node.n3.e_val;
                        
                        if(node.n3.level == 0)
                        {
                            //System.out.println("level 0");
                            node.n3.alpha_beta_eval = node.n3.e_val;
                        } 
                        
                        if(node.alpha_beta_eval <= node.n3.alpha_beta_eval)
                        {
                            node.alpha_beta_eval = node.n3.alpha_beta_eval;
                        }
                        
                        node.alpha = node.n3.alpha_beta_eval;
                    }
                }
                else
                {
                    node.eval_of_each_child_ambo[3] = -100;
                   // System.out.println("pruning implemented");
                }
                
                if(node.alpha_beta_eval <= node.beta)
                {
                    node.n4 = treeCreation(node,cur_player,4,level,currentBoard);
                    
                    if(node.n4 == null)
                    {
                        node.eval_of_each_child_ambo[4] = -100;
                    }
                    else
                    {
                        node.eval_of_each_child_ambo[4] = node.n4.e_val;
                        
                        if(node.n4.level == 0)
                        {
                            //System.out.println("level 0");
                            node.n4.alpha_beta_eval = node.n4.e_val;
                        }
                        
                        if(node.alpha_beta_eval <= node.n4.alpha_beta_eval)
                        {
                            node.alpha_beta_eval = node.n4.alpha_beta_eval;
                        }
                        
                        node.alpha = node.n4.alpha_beta_eval;
                    }
                }
                else
                {
                    node.eval_of_each_child_ambo[4] = -100;
                   // System.out.println("pruning implemented");
                }
                 
                if(node.alpha_beta_eval <= node.beta)
                {
                    node.n5 = treeCreation(node,cur_player,5,level,currentBoard);
                    
                    if(node.n5 == null)
                    {
                        node.eval_of_each_child_ambo[5] = -100;
                    }
                    else
                    {
                        node.eval_of_each_child_ambo[5] = node.n5.e_val;
                        if(node.n5.level == 0)
                        {
                            
                            node.n5.alpha_beta_eval = node.n5.e_val;
                        }
                        
                        if(node.alpha_beta_eval <= node.n5.alpha_beta_eval)
                        {
                            node.alpha_beta_eval = node.n5.alpha_beta_eval;
                        }
                        
                        node.alpha = node.n5.alpha_beta_eval;
                    }
                }
                else
                {
                    node.eval_of_each_child_ambo[5] = -100;
                   // System.out.println("pruning implemented");
                }

                if(node.alpha_beta_eval <= node.beta)
                {
                    node.n6 = treeCreation(node,cur_player,6,level,currentBoard);
                    
                    if(node.n6 == null)
                    {
                        node.eval_of_each_child_ambo[6] = -100;
                    }
                    else
                    {
                        node.eval_of_each_child_ambo[6] = node.n6.e_val;
                        
                        if(node.n6.level == 0)
                        {
                           
                            node.n6.alpha_beta_eval = node.n6.e_val;
                        }
                        
                        if(node.alpha_beta_eval <= node.n6.alpha_beta_eval)
                        {
                            node.alpha_beta_eval = node.n6.alpha_beta_eval;
                        }
                        
                        node.alpha = node.n6.alpha_beta_eval;
                    }
                }
                else
                {
                    node.eval_of_each_child_ambo[6] = -100;
                  //  System.out.println("pruning implemented");
                }

            }
            else //if the player is minimizing player (this is for aplha beta pruning)
            {
                if(node.alpha_beta_eval >= node.alpha)
                {
                    node.n1 = treeCreation(node,cur_player,1,level,currentBoard);
                    
                    if(node.n1 == null)
                    {
                            node.eval_of_each_child_ambo[1] = 100;
                             
                    }
                    else   
                    {
                        node.eval_of_each_child_ambo[1] = node.n1.e_val;
                        
                        if(node.n1.level == 0) //if we dealing with leaf node  
                        {
                            
                            node.n1.alpha_beta_eval = node.n1.e_val;
                        }
                
                        if(node.alpha_beta_eval >= node.n1.alpha_beta_eval)
                        {
                            node.alpha_beta_eval = node.n1.alpha_beta_eval;
                        }
                        node.beta = node.n1.alpha_beta_eval;
                    }
                }
                else
                {
                    node.eval_of_each_child_ambo[1] = 100;
                   // System.out.println("pruning implemented");
                }
                
                if(node.alpha_beta_eval >= node.alpha)
                {
                    node.n2 = treeCreation(node,cur_player,2,level,currentBoard);
                    
                    
                    //System.out.println("one one one");
                    
                    if(node.n2 == null)
                    {
                        node.eval_of_each_child_ambo[2] = 100;
                        //System.out.println("two two two");
                        
                    }
                    else
                    {
                        node.eval_of_each_child_ambo[2] = node.n2.e_val;
                        
                        if(node.n2.level == 0)
                        {         
                           node.n2.alpha_beta_eval = node.n2.e_val;
                        }
                        
    
                        
                        if(node.alpha_beta_eval >= node.n2.alpha_beta_eval)
                        {
                            node.alpha_beta_eval = node.n2.alpha_beta_eval;
                        }

                        node.beta = node.n2.alpha_beta_eval;

                    }
                }
                else
                {
                    node.eval_of_each_child_ambo[2] = 100;
                   // System.out.println("pruning implemented");
                }



               if(node.alpha_beta_eval >= node.alpha)
               {
                    node.n3 = treeCreation(node,cur_player,3,level,currentBoard);
                    
                    if(node.n3 == null)
                    {
                        node.eval_of_each_child_ambo[3] = 100;
                       
                    }
                    else
                    {
                        node.eval_of_each_child_ambo[3] = node.n3.e_val;
                        
                        if(node.n3.level == 0)
                        {   
                            node.n3.alpha_beta_eval = node.n3.e_val;
                        }
                        
                        if(node.alpha_beta_eval >= node.n3.alpha_beta_eval)
                        {
                            node.alpha_beta_eval = node.n3.alpha_beta_eval;
                        }
                        
                        node.beta = node.n3.alpha_beta_eval;
                    }
               }
               else
               {
                   node.eval_of_each_child_ambo[3] = 100;
                   // System.out.println("pruning implemented");
               }
            
               if(node.alpha_beta_eval >= node.alpha)
               {
                    node.n4 = treeCreation(node,cur_player,4,level,currentBoard);
                    
                    if(node.n4 == null)
                    {
                        node.eval_of_each_child_ambo[4] = 100;
                        
                    }
                    else
                    {
                        node.eval_of_each_child_ambo[4] = node.n4.e_val;
                    
                        if(node.n4.level == 0)
                        {
                            
                            node.n4.alpha_beta_eval = node.n4.e_val;
                        }
                        
                        if(node.alpha_beta_eval >= node.n4.alpha_beta_eval)
                        {
                            node.alpha_beta_eval = node.n4.alpha_beta_eval;
                        }
                        
                        node.beta = node.n4.alpha_beta_eval;
                    }
               }
               else
               {
                   node.eval_of_each_child_ambo[4] = 100;
                   // System.out.println("pruning implemented");
               }

               if(node.alpha_beta_eval >= node.alpha)
               {
                    node.n5 = treeCreation(node,cur_player,5,level,currentBoard);
                    if(node.n5 == null)
                    {
                        node.eval_of_each_child_ambo[5] = 100;
                        
                    }
                    else
                    {
                        node.eval_of_each_child_ambo[5] = node.n5.e_val;
                        
                        if(node.n5.level == 0)
                        {
                           
                        node.n5.alpha_beta_eval = node.n5.e_val;
                        }
                        
                        if(node.alpha_beta_eval >= node.n5.alpha_beta_eval)
                        {
                            node.alpha_beta_eval = node.n5.alpha_beta_eval;
                        }
                        
                        node.beta = node.n5.alpha_beta_eval;
                    }
               }
               else
               {
                   node.eval_of_each_child_ambo[5] = 100;
                   // System.out.println("pruning implemented");
               }

               if(node.alpha_beta_eval >= node.alpha)
               {
                    node.n6 = treeCreation(node,cur_player,6,level,currentBoard);
                    
                    if(node.n6 == null)
                    {
                        node.eval_of_each_child_ambo[6] = 100;
                     
                    }
                    else
                    {
                        node.eval_of_each_child_ambo[6] = node.n6.e_val;
                        
                        if(node.n6.level == 0)
                        {
                           
                            node.n6.alpha_beta_eval = node.n6.e_val;
                        }
                        
                        if(node.alpha_beta_eval >= node.n6.alpha_beta_eval)
                        {
                            node.alpha_beta_eval = node.n6.alpha_beta_eval;
                        }
                        
                        node.beta = node.n6.alpha_beta_eval;
                    }
               }
               else
               {
                   node.eval_of_each_child_ambo[6] = 100;
                  //  System.out.println("pruning implemented");
               }

            }
            
            
            if(node.player == rootnodeplayer)  //node is a maximizing player
            {
                //System.out.println("maximizing node");
                int max_val = node.eval_of_each_child_ambo[1];
                node.e_val_from_ambo = 1;
                for(int i = 1; i < 7;i++)
                {
                    if(max_val < node.eval_of_each_child_ambo[i])
                    {
                        max_val = node.eval_of_each_child_ambo[i];
                        node.e_val_from_ambo = i;
                    }
                }
                
                if(max_val == -100)
                {
                    if(rootnodeplayer == 1)
                    {
                        node.e_val = node.ambo[7] - node.ambo[0];
                    }
                    else
                    {
                        node.e_val = node.ambo[0] - node.ambo[7];
                    }
                    
                    node.alpha_beta_eval = node.e_val;
                }
                else
                {
                    node.e_val = max_val;
                }
            }
            else    //node is a minimizing player
            {
                //System.out.println("minimizing node");
                int min_val = node.eval_of_each_child_ambo[1];
                node.e_val_from_ambo = 1;
                for(int i = 1; i < 7;i++)
                {
                    if(min_val > node.eval_of_each_child_ambo[i])
                    {
                        min_val = node.eval_of_each_child_ambo[i];
                        node.e_val_from_ambo = i;
                    }
                }
                if(min_val == 100)
                {
                    if(rootnodeplayer == 1)
                    {
                        node.e_val = node.ambo[7] - node.ambo[0];
                    }
                    else
                    {
                        node.e_val = node.ambo[0] - node.ambo[7];
                    }
                    
                    node.alpha_beta_eval = node.e_val;
                }
                else
                {
                    node.e_val = min_val;
                }
                              
            }
            //System.out.println("returning node is");
            //nodeDisplay(node);
        }
        else
        {
            return null;
        }
        
        
        //System.out.println("returning node is");
        //nodeDisplay(node);
        
        return node;
    }
    

    
    public int playerInterchange(int cur_player)
    {
        
        if(cur_player == 1)
            return 2;
        else
            return 1;
    }
    


public Node nodeForm(Node node,int cur_player,int ambo_selected,GameState currentBoard)
    {
        
        Node dup_node = new Node(); // Decclaration of  duplicate node
        dup_node = duplicationofNode(node);    //initialzation of duplicate node i.e copying the original node to make operations on that 
        
        if(cur_player == 2)
            ambo_selected = ambo_selected + 7;
        
        //taking number of seeds in the ambo
        int num_seeds_in_ambo = dup_node.ambo[ambo_selected];
        
        //if there are no seeds in the ambo then return null
        if(num_seeds_in_ambo == 0)
            return null;
        else
        {
            dup_node.ambo[ambo_selected] = 0;   //making number of seeds in the ambo selected zero
            
            if(cur_player == 2)//if the player is 2 then seed distribution should skip for the player 1 home which is index 7 in ambo array
            {
                int seed_distribution_end_ambo = ambo_selected;           //integer variable which denotes where the distribution ends
                
                int dup_num_of_seeds = num_seeds_in_ambo;
                
                for(int i = 1; i <= dup_num_of_seeds; i++)                 //distributing seeds to furthur ambos
                {
                    if( (ambo_selected + i) % 14 ==  7)                      // skipping the distribution at opponent home
                    {
                        dup_num_of_seeds++;
                    }
                    else  
                    {
                        dup_node.ambo[(ambo_selected + i) % 14] = dup_node.ambo[(ambo_selected + i) % 14] + 1;
                        seed_distribution_end_ambo++;
                    }
                }
                
                if(seed_distribution_end_ambo >=8 && seed_distribution_end_ambo <= 13) //if the seed distribution ends in players own ambos
                {
                    if(dup_node.ambo[seed_distribution_end_ambo] == 1)     //if number of seeds in that particular ambo is 1 which is added now so it is previously empty
                    {
                        if(seed_distribution_end_ambo == 8)            //if the distribution ends at the ambo 8 then all the seeds at the ambo 8 and ambo 6(opposite) are transferred to players home
                        {
                            dup_node.ambo[0] = dup_node.ambo[0] + dup_node.ambo[8] + dup_node.ambo[6]; //adding all the seeds to the players home
                            dup_node.ambo[8] = 0;                          //making 0 seeds in the 8 ambo
                            dup_node.ambo[6] = 0;                          //making 0 seeds in ambo 6 oppositre ambo
                        }
                        else if(seed_distribution_end_ambo == 9)
                        {
                            dup_node.ambo[0] = dup_node.ambo[0] + dup_node.ambo[9] + dup_node.ambo[5]; 
                            dup_node.ambo[9] = 0; 
                            dup_node.ambo[5] = 0;
                        }
                        else if(seed_distribution_end_ambo == 10)
                        {
                            dup_node.ambo[0] = dup_node.ambo[0] + dup_node.ambo[10] + dup_node.ambo[4]; 
                            dup_node.ambo[10] = 0; 
                            dup_node.ambo[4] = 0;
                        }
                        else if(seed_distribution_end_ambo == 11)
                        {
                            dup_node.ambo[0] = dup_node.ambo[0] + dup_node.ambo[11] + dup_node.ambo[3]; 
                            dup_node.ambo[11] = 0; 
                            dup_node.ambo[3] = 0; 
                        }
                        else if(seed_distribution_end_ambo == 12)
                        {
                            dup_node.ambo[0] = dup_node.ambo[0] + dup_node.ambo[12] + dup_node.ambo[2]; 
                            dup_node.ambo[12] = 0; 
                            dup_node.ambo[2] = 0;
                        }
                        else if(seed_distribution_end_ambo == 13)
                        {
                            dup_node.ambo[0] = dup_node.ambo[0] + dup_node.ambo[13] + dup_node.ambo[1]; 
                            dup_node.ambo[13] = 0; 
                            dup_node.ambo[1] = 0;
                        }
                    }
                }
                if(seed_distribution_end_ambo == 0)
                    dup_node.freechance = 1;
                else
                    dup_node.freechance = 0;
                
                if(rootnodeplayer == 1)
                {
                    dup_node.e_val = dup_node.ambo[7] - dup_node.ambo[0];
                    
                }
                else
                {
                    dup_node.e_val = dup_node.ambo[0] - dup_node.ambo[7];
                             
                }
                
            }
            else               //if the player is 1 then seed distribution must skip player 2 home which is index 0 in ambo array
            {
                int dup_num_of_seeds = num_seeds_in_ambo;
                int seed_distribution_end_ambo_p1 = ambo_selected;
                
                for(int i = 1; i <= dup_num_of_seeds; i++) //distributing seeds to furthur ambos
                {
                    if((ambo_selected + i )%14 == 0) //if the index exceeds 13 then we again start from 0
                    {
                        dup_num_of_seeds++;
                    }
                    else  
                    {
                        dup_node.ambo[ (ambo_selected+i) % 14 ] = dup_node.ambo[ (ambo_selected+i) % 14 ] + 1;
                        seed_distribution_end_ambo_p1++;
                    }
                }
                if(seed_distribution_end_ambo_p1 >=1 && seed_distribution_end_ambo_p1 <= 6) //if the seed distribution ends in players own ambos-player 1
                {
                    if(dup_node.ambo[seed_distribution_end_ambo_p1] == 1)     //if number of seeds in that particular ambo is 1 which is added now so it is previously empty
                    {
                        if(seed_distribution_end_ambo_p1 == 1)            //if the distribution ends at the ambo 1 then all the seeds at the ambo 1 and ambo 13(opposite) are transferred to players home
                        {
                            dup_node.ambo[7] = dup_node.ambo[7] + dup_node.ambo[1] + dup_node.ambo[13]; //adding all the seeds to the players home
                            dup_node.ambo[1] = 0;                          //making 0 seeds in the 1 ambo
                            dup_node.ambo[13] = 0;                         //making 0 seeds in ambo 13 oppositre ambo
                        }
                        else if(seed_distribution_end_ambo_p1 == 2)
                        {
                            dup_node.ambo[7] = dup_node.ambo[7] + dup_node.ambo[2] + dup_node.ambo[12]; 
                            dup_node.ambo[2] = 0; 
                            dup_node.ambo[12] = 0;
                        }
                        else if(seed_distribution_end_ambo_p1 == 3)
                        {
                            dup_node.ambo[7] = dup_node.ambo[7] + dup_node.ambo[3] + dup_node.ambo[11]; 
                            dup_node.ambo[3] = 0; 
                            dup_node.ambo[11] = 0;
                        }
                        else if(seed_distribution_end_ambo_p1 == 4)
                        {
                            dup_node.ambo[0] = dup_node.ambo[0] + dup_node.ambo[10] + dup_node.ambo[4]; 
                            dup_node.ambo[10] = 0; 
                            dup_node.ambo[4] = 0; 
                        }
                        else if(seed_distribution_end_ambo_p1 == 5)
                        {
                            dup_node.ambo[0] = dup_node.ambo[0] + dup_node.ambo[9] + dup_node.ambo[5]; 
                            dup_node.ambo[9] = 0; 
                            dup_node.ambo[5] = 0;
                        }
                        else if(seed_distribution_end_ambo_p1 == 6)
                        {
                            dup_node.ambo[0] = dup_node.ambo[0] + dup_node.ambo[6] + dup_node.ambo[8]; 
                            dup_node.ambo[6] = 0; 
                            dup_node.ambo[8] = 0;
                        }
                    }
                }
                
                
                if(seed_distribution_end_ambo_p1 == 7)
                    dup_node.freechance = 1;
                else
                    dup_node.freechance = 0;
               
                if(rootnodeplayer == 1)
                {
                    dup_node.e_val = dup_node.ambo[7] - dup_node.ambo[0];
                    
                }
                else
                {
                    dup_node.e_val = dup_node.ambo[0] - dup_node.ambo[7];
                    
                }

            }
            
        }
        
        //if we want to write eval write here
        return dup_node;
    }
    
    
    /* 
    Return the current board with number of pebbele in each ambo
    */
    public Node boardState(Node node,GameState currentBoard)
    {
        
      for(int i = 0;i < 14;i++)
      {
          if(i == 0)            //for house of the north player
              node.ambo[0] = currentBoard.getScore(2);
          else if(i >=1 && i<=6)//for ambos of the south player
              node.ambo[i] = currentBoard.getSeeds(i,1);
          else if(i == 7)       //for house of the south player
              node.ambo[7] = currentBoard.getScore(1);
          else if(i >= 8)       //for ambos of the north player 
              node.ambo[i] = currentBoard.getSeeds((i-7),2);
      }
      return node;  
    }
    /*
    *Display the node structure for checking in the log
    *input:node which we want to display
    *output:all the contents of the node
    */
    public void nodeDisplay(Node node)
    {
        for(int i =0;i<14;i++)
        {
            System.out.println(node.ambo[i]);
        }
        
        if(node.freechance == 0)
            System.out.println("there is no free chancce for this node");
        else
            System.out.println("There is a free chance for this node");
        
        System.out.println("current player is "+node.player);
        System.out.println("eval for this node is "+(node.e_val));
        
        System.out.println("seeds at ambo 7 are "+node.ambo[7]);
        
        System.out.println("seeds at ambo 0 are "+node.ambo[0]);
        
        System.out.println("node is at level "+node.level);
        
        System.out.println("eval from children "+node.e_val_obtained_from_child);
        
        System.out.println("eval from child "+node.e_val_from_ambo);
        
        System.out.println("alpha beta evaluation value is "+node.alpha_beta_eval);
        
        System.out.println("alpha value is "+node.alpha);
        
        System.out.println("beta value is "+node.beta);

    }
    
    /*
    * creates a duplicate node every time required
    */
    
    public Node duplicationofNode(Node node)
    {
        Node duplicateNode = new Node();
        
        for(int i =0 ;i < 14; i++)
        {
            duplicateNode.ambo[i] = node.ambo[i];
        }
        
        duplicateNode.n1 = node.n1;
        duplicateNode.n2 = node.n2;
        duplicateNode.n3 = node.n3;
        duplicateNode.n4 = node.n4;
        duplicateNode.n5 = node.n5;
        duplicateNode.n6 = node.n6;
        
        duplicateNode.e_val =  node.e_val;
        //duplicateNode.level = node.level;
        duplicateNode.ambo_used =  node.ambo_used;
        duplicateNode.player = node.player;
        duplicateNode.freechance = node.freechance;
        
        return duplicateNode;
    }
    
    /**
     * Returns a random ambo number (1-6) used when making
     * a random move.
     * 
     * @return Random ambo number
     */
    public int getRandom()
    {       
        return 1+(int)(Math.random()*6);
    }
}


import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class 오목 extends JFrame {
    private static final int offset = 20; // how much space between end of board and boundary
    private static final int square = 40; // size of square
    private static final int pieceSize = 15; // radius of pieces
    private static final int fontSize = 20;
    private static final String filePath = "images/background.jpg";
	private static final boolean TEST = true;
    private Point click3, created;
    private List<Point> pieces;
    private List<Set<Point>> set34;
    private int mouseX, mouseY, show;
    private int bUndo = 0, wUndo = 0, startState = 1;
    private String font = "Lucina Grande";
    private boolean ifWon = false, showNum = false, calculating = false, AIMode = false;
    private BufferedImage image;
	private Jack AI;
    // TODO: timer dropdown, specify file format, autosave when game is done
    // TODO: multiplayer like PS6: first try to connect, then load offline/online mode
	// TODO: update to Javadoc style, experiment with loading partially completed games' interaction with Jack
	// TODO: make it so that if the board is empty and someone chooses a new mode, gamemode is automatically refreshed

    // constructor
    public 오목() {
        super("오목");
        // load in background here and not at paintComponent to greatly boost FPS
		if (!TEST) {
			try {
				image = ImageIO.read(getClass().getClassLoader().getResourceAsStream(filePath));
			} catch (IOException e) {
				System.err.println("이미지가 "+filePath+"' 에 존재하지 않습니다");
				System.exit(-1);
			}
		}
        // Helpers to create the canvas and GUI (buttons, etc.)
        JComponent canvas = setupCanvas();
        JComponent gui = setupGUI();
		JMenuBar menu = setUpMenu();
        // Put the buttons and canvas together into the window
        Container cp = getContentPane();
        cp.setLayout(new BorderLayout());
        cp.add(canvas, BorderLayout.CENTER);
        cp.add(gui, BorderLayout.NORTH);
		this.setJMenuBar(menu);
        // Usual initialization
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
		this.setVisible(true);
		// initialize game
        pieces = new ArrayList<>();
		AI = new Jack();
    }

    private JComponent setupCanvas() {
        JComponent canvas = new JComponent() {
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
				// set background image to scale with window size
				if (!TEST) {
					g.drawImage(image,0,0,offset*2+square*18,offset*2+square*18,null);
					for (int i=0; i<19; i++) { // draw base grid - horizontal, then vertical lines
						g.setColor(Color.black);
						g.drawLine(offset, offset+i*square, offset+18*square, offset+i*square);
						g.drawLine(offset+i*square, offset, offset+i*square, offset+18*square);
					}
					for (int x=0; x<3; x++) { // draw guiding dots
						for (int y=0; y<3; y++) {
							// dot size is fixed at 3
							g.fillOval(offset+square*(6*x+3)-3, offset+square*(6*y+3)-3, 6, 6);
						}
					}
				}
                drawPieces(g);
                drawOverlay(g);
            }
        };
        canvas.setPreferredSize(new Dimension(offset*2+square*18, offset*2+square*18));
        canvas.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                play(e.getPoint());
            }
        });
        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getPoint().x;
                mouseY = e.getPoint().y;
                repaint();
            }
        });
        return canvas;
    }

    private JComponent setupGUI() {
		JButton undo = new JButton("한수 무르기");
        undo.addActionListener(e -> undo());
        JButton clear;
        if (!TEST) {
        	clear = new JButton("재시작");
		} else {
        	clear = new JButton("restart");
		}
        clear.addActionListener(e -> clear());
		String[] states = {"2인용", "컴퓨터 - 백", "컴퓨터 - 흑"};
		JComboBox<String> stateB = new JComboBox<>(states);
		stateB.addActionListener(e -> {
			if (((JComboBox<String>)e.getSource()).getSelectedItem() == "2인용") {
				startState = 1;
			} else if (((JComboBox<String>)e.getSource()).getSelectedItem() == "컴퓨터 - 백") {
				startState = 2;
			} else {
				startState = 3;
			}
			System.out.println("Start state changed to: "+startState);
		});
		JButton first = new JButton("<<");
		//first.setPreferredSize(new Dimension(30, 20)); // TODO: how to set button size without changing style?
		first.addActionListener(e -> {
			if (pieces.size() > 0) {
				show = 1;
				repaint();
			}
		});
		JButton prev = new JButton("<");
		//prev.setPreferredSize(new Dimension(20, 20));
		prev.addActionListener(e -> {
			if (show > 1) {
				show--;
				repaint();
			}
		});
		JButton next = new JButton(">");
		//next.setPreferredSize(new Dimension(20, 20));
		next.addActionListener(e -> {
			if (show < pieces.size()) {
				show++;
				repaint();
			}
		});
		JButton last = new JButton(">>");
		//last.setPreferredSize(new Dimension(30, 20));
		last.addActionListener(e -> {
			show = pieces.size();
			repaint();
		});
        JComponent gui = new JPanel();
		gui.add(stateB);
        gui.add(undo);
        gui.add(clear);
        gui.add(first);
        gui.add(prev);
        gui.add(next);
        gui.add(last);
		if (TEST) {
			JButton test = new JButton("test");
			test.addActionListener(e -> {System.out.println("Show = "+show); AI.test();});
			gui.add(test);
		}
        return gui;
    }

    private JMenuBar setUpMenu() {
		JMenuBar menubar = new JMenuBar();
		JMenu fileMenu = new JMenu("파일");
		JMenuItem openMi = new JMenuItem("열기");
		openMi.addActionListener((ActionEvent e) -> load());
		JMenuItem saveMi = new JMenuItem("저장");
		saveMi.addActionListener((ActionEvent e) -> save());
		fileMenu.add(openMi);
		fileMenu.add(saveMi);
		JMenu numMenu = new JMenu("수");
		JCheckBoxMenuItem showMi = new JCheckBoxMenuItem("보이기");
		showMi.setMnemonic(KeyEvent.VK_S);
		showMi.addItemListener((ItemEvent e) -> {showNum = !showNum; repaint();});
		JMenu fontMenu = new JMenu("글꼴");
		fontMenu.setMnemonic(KeyEvent.VK_F);
		ButtonGroup fontGroup = new ButtonGroup();
		JRadioButtonMenuItem font1RMi = new JRadioButtonMenuItem("Arial");
		fontMenu.add(font1RMi);
		font1RMi.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				font = "Arial";
				repaint();
			}
		});
		JRadioButtonMenuItem font2RMi = new JRadioButtonMenuItem("Courier");
		fontMenu.add(font2RMi);
		font2RMi.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				font = "Courier";
				repaint();
			}
		});
		JRadioButtonMenuItem font3RMi = new JRadioButtonMenuItem("Helvetica Neue");
		fontMenu.add(font3RMi);
		font3RMi.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				font = "Helvetica Neue";
				repaint();
			}
		});
		JRadioButtonMenuItem font4RMi = new JRadioButtonMenuItem("Lucina Grande");
		font4RMi.setSelected(true);
		fontMenu.add(font4RMi);
		font4RMi.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				font = "Lucina Grande";
				repaint();
			}
		});
		fontGroup.add(font1RMi);
		fontGroup.add(font2RMi);
		fontGroup.add(font3RMi);
		fontGroup.add(font4RMi);
		numMenu.add(showMi);
		numMenu.add(fontMenu);
		menubar.add(fileMenu);
		menubar.add(numMenu);
		return menubar;
	}

    private void drawPieces(Graphics g) {
        FontMetrics metrics = g.getFontMetrics(new Font(font, Font.PLAIN, fontSize));
        FontMetrics metrics2 = g.getFontMetrics(new Font(font, Font.PLAIN, fontSize-4));
        for (int i=0; i<show; i++) {
            if (i%2 == 0) { // black's pieces
                g.setColor(Color.black);
                g.fillOval(offset+square*pieces.get(i).x-pieceSize, offset+square*pieces.get(i).y-pieceSize,
						pieceSize*2, pieceSize*2);
                if (showNum) {
                    g.setColor(Color.white);
                }
            } else {
                g.setColor(Color.white);
                g.fillOval(offset+square*pieces.get(i).x-pieceSize, offset+square*pieces.get(i).y-pieceSize,
						pieceSize*2, pieceSize*2);
                if (showNum) {
                    g.setColor(Color.black);
                }
            }
			if (showNum) { // drawing numbers
				if (i<99) {
					g.setFont(new Font(font, Font.PLAIN, fontSize));
					g.drawString(Integer.toString(i + 1), offset + square * pieces.get(i).x
							- (metrics.stringWidth(Integer.toString(i + 1))) / 2,offset+square*pieces.get(i).y
							- (metrics.getHeight()) / 2 + metrics.getAscent());
				} else {
					g.setFont(new Font(font, Font.PLAIN,fontSize - 4)); // 3-digits get decreased font size
					g.drawString(Integer.toString(i + 1), offset + square * pieces.get(i).x
							- (metrics2.stringWidth(Integer.toString(i + 1))) / 2,offset+square*pieces.get(i).y
							- (metrics2.getHeight()) / 2 + metrics2.getAscent());
				}
			}
        }
        if (TEST) {
			g.setFont(new Font(font, Font.PLAIN, fontSize - 4));
			int[][] scores = AI.getScores();
        	for (int i=0; i<19; i++) {
        		for (int j=0; j<19; j++) {
        			if (scores[i][j] > 0) {
						g.setColor(Color.blue);
					} else if (scores[i][j] < 0) {
						g.setColor(Color.red);
					} else {
        				g.setColor(Color.gray);
					}
        			g.drawString(Integer.toString(scores[i][j]), offset + square * i
						- (metrics2.stringWidth(Integer.toString(scores[i][j]))) / 2, offset + square * j
						- (metrics2.getHeight()) / 2 + metrics2.getAscent());
				}
			}
		}
    }

    private void drawOverlay(Graphics g) {
		if (!calculating) {
			if (!ifWon) {
				int px = Math.round((mouseX-offset+square/2)/square);
				int py = Math.round((mouseY-offset+square/2)/square);
				if (created == null) {
					if (click3 != null) {
						if ((click3.x-px)*(click3.x-px)+(click3.y-py)*(click3.y-py) >= 1) {
							click3 = null;
							return;
						}
						g.setColor(new Color(220,83,74));
						g.fillOval(offset+square*px-pieceSize,offset+square*py-pieceSize,
								pieceSize*2,pieceSize*2);
						return;
					}
					for (Point p : pieces) {
						if ((p.x-px)*(p.x-px)+(p.y-py)*(p.y-py) < 1) {
							g.setColor(new Color(220,83,74));
							g.fillOval(offset+square*px-pieceSize,offset+square*py-pieceSize,
									pieceSize*2,pieceSize*2);
							return;
						}
					}
					if (pieces.size()%2 == 0) {
						g.setColor(new Color(0,0,0,127));
						g.fillOval(offset+square*px-pieceSize, offset+square*py-pieceSize,
								pieceSize*2, pieceSize*2);
					} else {
						g.setColor(new Color(255,255,255,127));
						g.fillOval(offset+square*px-pieceSize, offset+square*py-pieceSize,
								pieceSize*2, pieceSize*2);
					}
					return;
				}
				if ((created.x-px)*(created.x-px)+(created.y-py)*(created.y-py) >= 1) {
					created = null;
				}
			}
		}
    }

    private void play(Point p) {
        if (!ifWon) {
            int px = Math.round((p.x-offset+square/2)/square);
            int py = Math.round((p.y-offset+square/2)/square);
            Point pt = new Point(px, py);
            if (!pieces.contains(pt)) {
                pieces.add(pt);
                set34 = open3(pieces);
                if (legalMove(pt)) {
                	show = pieces.size();
                    created = pt;
                    if (TEST || AIMode) AI.addPoint(px, py);
                    if (won()) {
						ifWon = true;
                        if (pieces.size()%2 == 0) {
							JOptionPane.showMessageDialog(오목.this, "백 승리!");
                        } else {
							JOptionPane.showMessageDialog(오목.this, "흑 승리!");
                        }
						repaint();
                    } else {
						repaint();
						if (AIMode) {
							calculating = true;
							double startTime = System.nanoTime();
							Point tmp = AI.winningMove();
							pieces.add(tmp);
							AI.addPoint(tmp.x, tmp.y);
							double endTime = System.nanoTime();
							double duration = (endTime - startTime)/1000000;
							System.out.println("It took "+duration+" ms to calculate the best move");
							calculating = false;
							show = pieces.size();
						}
						repaint();
					}
                } else {
                    click3 = pt;
                    pieces.remove(click3);
                    repaint();
					JOptionPane.showMessageDialog(오목.this, "삼삼!");
                }
            }
        }
    }

    private void undo() {
    	// TODO: fix Jack's interaction with undo, then restore the undo by removing if statement with AIMode
        if (!TEST && !AIMode && !ifWon) {
            if (pieces.size()%2 == 1 && bUndo<3) {
                pieces.remove(pieces.size()-1);
                bUndo++;
                set34 = open3(pieces);
                show = pieces.size();
            } else if (pieces.size()>0 && wUndo<3) {
                pieces.remove(pieces.size()-1);
                wUndo++;
                set34 = open3(pieces);
                show = pieces.size();
            } else {
				JOptionPane.showMessageDialog(오목.this, "수 되돌리기 불가능!");
            }
            repaint();
        }
    }

    private void clear() {
        pieces = new ArrayList<>();
		set34 = new ArrayList<>();
        bUndo = wUndo = 0;
        show = 0;
        ifWon = calculating = false;
        created = null;
        click3 = null;
        AI = new Jack();
		if (startState == 1) { // 2P
			AIMode = false;
		} else if (startState == 2) { // COM WHITE
			AIMode = true;
		} else {
			AIMode = true;
			pieces.add(new Point(9,9));
			show++;
			AI.addPoint(9,9);
		}
        repaint();
    }

    private void save() {
        BufferedWriter output = null;
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showSaveDialog(오목.this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                output = new BufferedWriter(new FileWriter(file));
                String s = "";
				for (Point piece : pieces) {
					s += "(" + piece.x + "," + piece.y + ")";
				}
                output.write(s+"|"+bUndo+"|"+wUndo);
                System.out.println("저장 성공!");
            } catch (IOException e) {
                System.out.println("저장 불가능\n" + e.getMessage());
            } finally {
                if (output != null) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        System.out.println("저장 파일 체크!\n" + e.getMessage());
                    }
                }
            }
        }
    }

    private void load() {
        String line;
        String[] part, frags;
        BufferedReader input = null;
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(오목.this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                input = new BufferedReader(new FileReader(file));
                clear();
                line = input.readLine();
                part = line.split("\\|");
                bUndo = Integer.parseInt(part[1]);
                wUndo = Integer.parseInt(part[2]);
                frags = part[0].split("[\\W]"); // splits on any non-alphabetical character
                for (int i=1; i<frags.length-1; i=i+3) {
                    pieces.add(new Point(Integer.parseInt(frags[i]), Integer.parseInt(frags[i+1])));
                    // save some computational resources by NOT calculating threat spaces and shit if we don't have to
                    if (AIMode) AI.addPoint(Integer.parseInt(frags[i]), Integer.parseInt(frags[i+1]));
                }
                show = pieces.size();
                set34 = open3(pieces); // for winning check
				System.out.println("set34: "+set34.toString());
				ifWon = won();
                repaint();
            } catch (IOException e) {
                System.out.println("파일을 제대로 선택했는지 점검\n" + e.getMessage());
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        System.out.println("저장 파일 체크\n" + e.getMessage());
                    }
                }
            }
        }
    }

    // house rule: bans a move that simultaneously forms two open rows of three stones
    private boolean legalMove(Point p) {
		// the rules go out the window when fighting AI.
		// TODO: Turn it back on when I figure out how to make the AI check if it is making legal moves
		if (pieces.size() < 9 || AIMode) {
			return true;
		}
		for (Set<Point> set : set34) {
			if (set.contains(p) && set.size() == 3) {
				for (Point neighbor : set) {
					for (Set<Point> set2 : set34) {
						if (!set.equals(set2) && set2.contains(neighbor) && set2.size() == 3) {
							return false;
						}
					}
				}
			}
		}
        return true;
    }

    private boolean won() {
        if (pieces.size() < 9) {
            return false;
        }
        for (Set<Point> set : set34) {
            if (set.size() == 4) {
                List<Point> points = new ArrayList<>();
                for (Point p : set) {
                    points.add(p);
                }
                if (points.get(0).x == points.get(1).x) { // they are on vertical line
                    points.sort((Point o1, Point o2) -> o1.y - o2.y);
                } else { // either horizontal or diagonal line
                    points.sort((Point o1, Point o2) -> o1.x - o2.x);
                }
                for (int i=(pieces.size()%2+1)%2; i<pieces.size(); i=i+2) {
                    if (pieces.get(i).equals(new Point(2*points.get(0).x-points.get(1).x,2*points.get(0).y
							-points.get(1).y)) || pieces.get(i).equals(new Point(2*points.get(3).x-points.get(2).x,
							2*points.get(3).y-points.get(2).y))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // finds list of all sets of 3 adjacent points without any blockages and 4 that have at least one opening
	// quick and dirty way of finding open sets of 3 and 4, for checking for users' legal move and win conditions
    private List<Set<Point>> open3(List<Point> points) {
        List<Set<Point>> result = new ArrayList<>();
        for (int i=(points.size()%2+1)%2; i<points.size(); i=i+2) {
            Point p1 = points.get(i);
            for (int j=i+2; j<points.size(); j=j+2) {
                Point p2 = points.get(j);
                if ((p1.x-p2.x)*(p1.x-p2.x)+(p1.y-p2.y)*(p1.y-p2.y) <= 2) { // if p1 and p2 are adjacent
                    for (int k=(points.size()%2+1)%2; k<points.size(); k=k+2) {
                        if (k != i && k != j) {
							Point p3 = points.get(k);
							boolean passed = false;
							boolean blocked = false;
							Point p41, p42;
							p41 = p42 = new Point();
							if (p3.x!=0 && p3.x!=18 && p3.y!=0 && p3.y!=18) {
								if (p3.equals(new Point(2*p1.x-p2.x, 2*p1.y-p2.y))) { // p3 p1 p2
									if (p2.x!=0 && p2.x !=18 && p2.y!=0 && p2.y!=18) { // boundary check
										p41 = new Point(2*p2.x-p1.x, 2*p2.y-p1.y); // checking both ends
										p42 = new Point(2*p3.x-p1.x, 2*p3.y-p1.y);
										passed = true;
									}
								} else if (p3.equals(new Point(2*p2.x-p1.x,2*p2.y-p1.y))) { // p3 p2 p1
									if (p1.x!=0 && p1.x!=18 && p1.y!=0 && p1.y!=18) {
										p41 = new Point(2*p1.x-p2.x, 2*p1.y-p2.y);
										p42 = new Point(2*p3.x-p2.x, 2*p3.y-p2.y);
										passed = true;
									}
								}
								if (passed) {
									// if either is of other color, throw it out
									for (int n=points.size()%2; n<points.size(); n=n+2) {
										if (points.get(n).equals(p41) || points.get(n).equals(p42)) {
											passed = false;
											blocked = true;
										}
									}
									if (!blocked) {
										for (int n=(points.size()%2+1)%2; n<points.size(); n=n+2) {
											if (points.get(n).equals(p41) || points.get(n).equals(p42)) {
												Set<Point> halfOpenSet4 = new HashSet<>();
												halfOpenSet4.add(p1);
												halfOpenSet4.add(p2);
												halfOpenSet4.add(p3);
												halfOpenSet4.add(points.get(n));
												if (!result.contains(halfOpenSet4)) {
													result.add(halfOpenSet4);
												}
												passed = false;
											}
										}
									}
								}
							}
							if (passed) {
								Set<Point> openSet3 = new HashSet<>();
								openSet3.add(p1);
								openSet3.add(p2);
								openSet3.add(p3);
								if (!result.contains(openSet3)) {
									result.add(openSet3);
								}
							}
						}
                    }
                }
            }
        }
        return result;
    }

    public static void main(String[] cheese) {
        new 오목();
    }
}
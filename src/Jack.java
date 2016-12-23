import java.awt.*;
import java.util.*;
import java.util.List;

public class Jack {
	private int depth = 2*(2)+1, turn = 1; // -1 for white, 1 for black. Depth should be odd
	private int[][] board, scores; // actual board for storing pieces. Separate from storing board space scores
	private Map<Point, List<PI>> lookup; // threat space (incl. 0) -> threats -> score
	private Map<Point, List<List<PI>>> threatSpaces; // threat -> threat space lines -> space & score
	// TODO: threat depth search using threat scores, detecting any dangerous patterns
	// TODO: add alpha-beta pruning in minimax tree for winningmove
	// NOTE: Jack maximizes points for whoever has the current turn

	// custom data type
	public class PI {
		private Point p;
		private int i;

		public PI (Point p, int i) {
			this.p = p; this.i =i;
		}

		public Point getP() {
			return p;
		}

		public void setP(Point p) {
			this.p = p;
		}

		public int getI() {
			return i;
		}

		public void setI(int i) {
			this.i = i;
		}

		public String toString() {
			return "<("+p.x+", "+p.y+"), "+i+">";
		}
	}

	// constructor
	public Jack() {
		board = new int[19][19];
		lookup = new HashMap<>();
		threatSpaces = new HashMap<>();
		scores = new int[19][19];
	}

	// officially adds point, modifying the actual threatSpaces and lookup
	public void addPoint(int x, int y) {
		board[x][y] = turn;
		turn = 0 - turn;
		// add point to lookup and threatSpaces
		threatSpaces = step(x,y, threatSpaces,lookup,turn);
		lookup = hash(threatSpaces);
		scores = calculateScores(lookup);
	}

	// modifies sequences, threat spaces, and scores given a new point
	private Map<Point, List<List<PI>>> step(int x, int y, Map<Point, List<List<PI>>> threatSpaces,
												 Map<Point, List<PI>> lookup, int turn) {
		Map<Point, List<List<PI>>> result = new HashMap<>();
		// first, alternate scores as ones that are affected and not affected both need to alternate scores
		for (Point threat : threatSpaces.keySet()) {
			List<List<PI>> updatedList = new ArrayList<>();
			for (List<PI> threatLine : threatSpaces.get(threat)) {
				List<PI> newList = new ArrayList<>(threatLine);
				// if color of sequence is the same as the color of whoever just put down their stone
				if (board[threat.x][threat.y] == turn) {
					for (PI spaceScore : newList) {
						// multiply all scores by 2
						spaceScore.setI(spaceScore.getI() * 2);
					}
				} else {
					for (PI spaceScore : newList) {
						spaceScore.setI(spaceScore.getI() / 2);
					}
				}
				updatedList.add(newList);
			}
			result.put(threat, updatedList);
		}
		// lookup the point and see which ones it affect
		Point latestPoint = new Point(x, y); // the point that is affecting
		if (lookup.containsKey(latestPoint)) {
			for (PI threats : lookup.get(latestPoint)) {
				Point threat = threats.getP();
				if (board[threat.x][threat.y] != turn) { // same color
					// first, update the score of affected threat space
					// TODO: optimize search by limiting search based on relative location of the new stone
					for (int i=0; i<8; i++) {
						for (int j=0; j<threatSpaces.get(threat).get(i).size(); j++) {
							if (threatSpaces.get(threat).get(i).get(j).getP().equals(latestPoint)) {
								result.get(threat).get(i).get(j).setI(0);
							}
						}
					}
				} else {
					// TODO: opposing color - check other side for <5 then remove threat spaces accordingly
					boolean blocked = false;
					for (int i=0; i<8; i++) {
						for (int j=0; j<threatSpaces.get(threat).get(i).size(); j++) {
							if (!blocked) {
								if (threatSpaces.get(threat).get(i).get(j).getP().equals(latestPoint)) {
									List<PI> newLine = new ArrayList<>();
									for (int k=0; k<j; k++) {
										newLine.add(threatSpaces.get(threat).get(i).get(k));
									}
									if (j > 0) { // reduce score of the stone adjacent to the opposing piece by 1/4th
										newLine.set(j-1, new PI(newLine.get(j-1).getP(), newLine.get(j-1).getI()/4));
									} else if (j == 0) { // reduce score of other side by 1/4
										int opposite = (i + 4) % 8;
										for (int k=0; k<threatSpaces.get(threat).get(opposite).size(); k++) {
											threatSpaces.get(threat).get(opposite).get(k).setI(threatSpaces.get(threat)
													.get(opposite).get(k).getI() / 4);
										}
									}
									result.get(threat).set(i, newLine);
									blocked = true;
								}
							}
						}
					}
				}
			}
		}
		// this is a new threat 'sequence' containing only one point (goes 8-way)
		// insert the threat point - expand until board boundary or opposite color is reached. when same color, score 0
		int[] xFactor = {1, 1}, yFactor = {0, 1};
		List<List<PI>> threatLines = new ArrayList<>();
		int blocked = -1;
		for (int i=0; i<4; i++) {
			for (int j=0; j<=1; j++) {
				List<PI> threatLine = new ArrayList<>();
				boolean clash = false;
				for (int k=1; k<=5; k++) {
					if (!clash) {
						int xt = x + k * xFactor[j];
						int yt = y + k * yFactor[j];
						if (0<=xt && xt<19 && 0<=yt && yt<19) {
							if (board[xt][yt] == 0) {
								if (k != 5) { // actual threat space
									threatLine.add(new PI(new Point(xt, yt), -4 * turn)); // starting value
								} else { // 0-space
									threatLine.add(new PI(new Point(xt, yt), 0));
								}
							} else if (board[xt][yt] == -1*turn) {
								threatLine.add(new PI(new Point(xt, yt), 0));
							} else {
								if (!threatLine.isEmpty()) {
									threatLine.get(k - 2).setI(threatLine.get(k - 2).getI() / 4);
								}
								if (k == 1) {
									blocked = 2 * i + j;
								}
								clash = true;
							}
						} else {
							// either a differing color or out of bounds
							if (!threatLine.isEmpty()) {
								threatLine.get(k - 2).setI(threatLine.get(k - 2).getI() / 4);
							}
							if (k == 1) {
								blocked = 2 * i + j;
							}
							clash = true;
						}
					}
				}
					/*if (!threatLine.isEmpty())*/ threatLines.add(threatLine);
			}
			// rotate 90° left
			int[] temp = Arrays.copyOf(xFactor,2);
			for (int j=0; j<=1; j++) {
				xFactor[j] = 0 - yFactor[j];
				yFactor[j] = temp[j];
			}
		}
		result.put(latestPoint, threatLines);
		// adjusting score of threat spaces of opposite side of wherever an opposing piece is directly touching
		if (blocked >= 0) {
			for (int i=0; i<result.get(latestPoint).get((blocked + 4) % 8).size(); i++) {
				result.get(latestPoint).get((blocked + 4) % 8).get(i).setI(result.get(latestPoint)
						.get((blocked + 4) % 8).get(i).getI() / 4);
			}
		}
		return result;
	}

	// calculates lookup given threat spaces
	private Map<Point, List<PI>> hash(Map<Point, List<List<PI>>> threatSpaces) {
		Map<Point, List<PI>> result = new HashMap<>();
		for (Point threat : threatSpaces.keySet()) {
			for (List<PI> threatLine : threatSpaces.get(threat)) {
				for (PI threatSpace : threatLine) {
					if (!result.containsKey(threatSpace.getP())) {
						List<PI> temp = new ArrayList<>();
						temp.add(new PI(threat, threatSpace.getI()));
						result.put(threatSpace.getP(), temp);
					} else {
						result.get(threatSpace.getP()).add(new PI(threat, threatSpace.getI()));
					}
				}
			}
		}
		return result;
	}

	private int[][] calculateScores(Map<Point, List<PI>> lookup) {
		int[][] result = new int[19][19];
		for (Point threatSpace : lookup.keySet()) {
			if (lookup.get(threatSpace).size() == 1) {
				result[threatSpace.x][threatSpace.y] = lookup.get(threatSpace).get(0).getI();
			} else {
				int black = 0, white = 0;
				for (PI threatPI : lookup.get(threatSpace)) {
					if (threatPI.getI() != 0) {
						if (board[threatPI.getP().x][threatPI.getP().y] == 1) {
							if (black == 0) {
								black = threatPI.getI();
							} else {
								if (turn == -1) {
									black *= threatPI.getI();
								} else {
									black *= (threatPI.getI() / 2);
								}
							}
						} else {
							if (white == 0) {
								white = threatPI.getI();
							} else {
								if (turn == 1) {
									white *= (-1 * threatPI.getI());
								} else {
									white *= (threatPI.getI() / -2);
								}
							}
						}
					}
				}
				if (black == 0) {
					result[threatSpace.x][threatSpace.y] = white;
				} else if (white == 0) {
					result[threatSpace.x][threatSpace.y] = black;
				} else {
					result[threatSpace.x][threatSpace.y] = turn * (black - white);
				}
			}
		}
		return result;
	}

	public void test() {
		System.out.println("<--------test-------->");
		System.out.println("threatSpaces: "+ threatSpaces.toString());
		System.out.println("lookup: "+lookup.toString());
		System.out.println("number of threat spaces: "+lookup.keySet().size());
	}

	public int[][] getScores() {
		return scores;
	}

	// return the best move
	public Point winningMove() {
		Point result = new Point(10,10);
		return result;
	}
}
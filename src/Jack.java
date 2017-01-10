import java.awt.*;
import java.util.*;
import java.util.List;

public class Jack {
	private static final int SUFFICIENTLY_LARGE_NUMBER = 100_000_000;
	private static final double DEFENSE_WEIGHT = 0.9;
	private static final double THRESHOLD = 2/3;
	private int depth = 5, turn = 1; // -1 for white, 1 for black. Depth should be odd
	private int[][] board, scores; // actual board for storing pieces. Separate from storing board space scores
	private Map<Point, List<List<PI>>> threatSpaces; // threat -> threat space lines -> space & score
	private Map<Point, List<List<Point>>> lookup; // threat space (incl. 0) -> list of threat sequences
	private double[] time;
	// TODO: if I can figure out how to deep copy objects, then implement undo
	// link: http://stackoverflow.com/questions/2156120/java-recommended-solution-for-deep-cloning-copying-an-instance
	// TODO: add alpha-beta pruning in minimax tree for winningMove

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
		threatSpaces = new HashMap<>();
		lookup = new HashMap<>();
		scores = new int[19][19];
		time = new double[8];
	}

	// officially adds point, modifying the actual threatSpaces and lookup
	public void addPoint(int x, int y) {
		board[x][y] = turn;
		turn = 0 - turn;
		// add point to lookup and threatSpaces
		threatSpaces = step(x, y, threatSpaces, lookup, turn);
		time[5] = System.nanoTime();
		lookup = hash(lookup, x, y);
		time[6] = System.nanoTime();
		scores = calculateScores(lookup, threatSpaces);
		time[7] = System.nanoTime();
	}

	// modifies sequences, threat spaces, and scores given a new point
	private Map<Point, List<List<PI>>> step(int x, int y, Map<Point, List<List<PI>>> threatSpaces,
												 Map<Point, List<List<Point>>> lookup, int turn) {
		time[0] = System.nanoTime();
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
		time[1] = System.nanoTime();
		// lookup the point and see which ones it affect
		Point latestPoint = new Point(x, y); // the point that is affecting
		if (lookup.containsKey(latestPoint)) {
			for (List<Point> threatSequence : lookup.get(latestPoint)) {
				for (Point threat : threatSequence) {
					if (board[threat.x][threat.y] != turn) { // same color
						// first, update the score of affected threat space
						int[] temp = threatSpaceFinder(threat, latestPoint);
						result.get(threat).get(temp[0]).get(temp[1]).setI(0);
					} else {
						// TODO: opposing color - check other side for <5 then remove threat spaces accordingly
						boolean blocked = false;
						for (int i=0; i<8; i++) {
							for (int j=0; j<threatSpaces.get(threat).get(i).size(); j++) {
								if (!blocked && threatSpaces.get(threat).get(i).get(j).getP().equals(latestPoint)) {
									List<PI> newLine = new ArrayList<>();
									for (int k=0; k<j; k++) {
										newLine.add(threatSpaces.get(threat).get(i).get(k));
									}
									if (j > 0) { // reduce score of the stone adjacent to the opposing piece by 1/4th
										newLine.set(j-1, new PI(newLine.get(j-1).getP(), newLine.get(j-1).getI() / 4));
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
		time[2] = System.nanoTime();
		// this is a new threat 'sequence' containing only one point (goes 8-way)
		// insert the threat point - expand until board boundary or opposite color is reached. when same color, score 0
		int[] xFactor = {1, 1}, yFactor = {0, 1};
		List<List<PI>> threatLines = new ArrayList<>();
		List<Integer> blocked = new ArrayList<>();
		for (int i=0; i<4; i++) {
			for (int j=0; j<=1; j++) {
				List<PI> threatLine = new ArrayList<>();
				boolean clash = false;
				for (int k=1; k<=5; k++) {
					if (!clash) {
						int xt = x + k * xFactor[j];
						int yt = y + k * yFactor[j];
						if (0<=xt && xt<19 && 0<=yt && yt<19 && board[xt][yt] != turn) {
							if (board[xt][yt] == 0) {
								if (k != 5) { // actual threat space
									threatLine.add(new PI(new Point(xt, yt), -4 * turn)); // starting value
								} else { // 0-space
									threatLine.add(new PI(new Point(xt, yt), 0));
								}
							} else { // same color
								threatLine.add(new PI(new Point(xt, yt), 0));
							}
						} else {
							if (!threatLine.isEmpty()) { // out of bounds or differing color
								threatLine.get(k - 2).setI(threatLine.get(k - 2).getI() / 4);
							}
							if (k == 1) {
								blocked.add(2 * i + j);
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
		time[3] = System.nanoTime();
		// adjusting score of threat spaces of opposite side of wherever an opposing piece is directly touching
		if (!blocked.isEmpty()) {
			for (int side : blocked) {
				int oppositeSide = (side + 4) % 8;
				for (int i=0; i<result.get(latestPoint).get(oppositeSide).size(); i++) {
					result.get(latestPoint).get(oppositeSide).get(i)
							.setI(result.get(latestPoint).get(oppositeSide).get(i).getI() / 4);
				}
			}
		}
		time[4] = System.nanoTime();
		return result;
	}

	private int[] threatSpaceFinder(Point threat, Point threatSpace) {
		int[] result = new int[2];
		if (threat.x > threatSpace.x) {
			// to the left
			if (threat.y > threatSpace.y) {
				// NW
				result[0] = 5;
			} else if (threat.y < threatSpace.y) {
				// SW
				result[0] = 3;
			} else {
				// left
				result[0] = 4;
			}
			result[1] = threat.x - threatSpace.x - 1;
		} else if (threat.x < threatSpace.x) {
			// to the right
			if (threat.y > threatSpace.y) {
				// NE
				result[0] = 7;
			} else if (threat.y < threatSpace.y) {
				// SE
				result[0] = 1;
			} else {
				// right
				result[0] = 0;
			}
			result[1] = threatSpace.x - threat.x - 1;
		} else {
			if (threat.y > threatSpace.y) {
				// up top
				result[0] = 6;
			} else {
				// down low
				result[0] = 2;
			}
			result[1] = Math.abs(threatSpace.y - threat.y) - 1;
		}
		return result;
	}

	private Map<Point, List<List<Point>>> hash(Map<Point, List<List<Point>>> lookup, int x, int y) {
		Map<Point, List<List<Point>>> result = new HashMap<>(lookup);
		Point latestPoint = new Point(x, y);
		List<List<Point>> visited = new ArrayList<>();
		if (lookup.containsKey(latestPoint)) result.remove(latestPoint);
		int[] xFactor = {1, 1}, yFactor = {0, 1};
		for (int i=0; i<4; i++) {
			for (int j=0; j<2; j++) {
				boolean blocking = false;
				for (int k = 1; k<5; k++) {
					// check all threat spaces that could be affected by the latest point
					int xt = x + k * xFactor[j];
					int yt = y + k * yFactor[j];
					if (0<=xt && xt<19 && 0<=yt && yt<19) {
						if (board[xt][yt] == 0) {
							// a valid threat space
							Point threatSpace = new Point(xt, yt);
							boolean exists = false, found = false, toAdd = false;
							if (lookup.containsKey(threatSpace)) {
								// modify existing sequence that has this threat space
								// TODO: fix control flow
								for (int m = 0; m < lookup.get(threatSpace).size(); m++) {
									List<Point> sequence = result.get(threatSpace).get(m);
									// check if the sequence is valid
									if (!visited.contains(sequence) && (inRange(latestPoint, sequence.get(0)) ||
											inRange(latestPoint, sequence.get(sequence.size() - 1)))) {
										exists = true;
										// and check if the sequence, threat space, and the new threat all line up
										if (inLine(sequence.get(0), threatSpace, latestPoint)) {
											found = true;
											if (board[sequence.get(0).x][sequence.get(0).y] != turn) {
												// the sequence has same color as the latest point
												if (isClear(sequence.get(sequence.size() - 1), latestPoint, turn)) {
													if (sequence.size() == 1) {
														if (closer(threatSpace, sequence.get(0), x, y)) {
															sequence.add(0, latestPoint);
														} else {
															sequence.add(latestPoint);
														}
													} else {
														boolean out = false;
														for (int n = 1; n < sequence.size(); n++) {
															if (!inRange(latestPoint, sequence.get(n))) {
																out = true;
															}
														}
														if (!out) {
															int position = position(sequence, latestPoint);
															if (position == 1) {
																// latestPoint --- start --- end
																sequence.add(0, latestPoint);
															} else if (position == 2) {
																// start --- latestPoint --- end
																int n = 1;
																// putting it in order so that I don't have to sort
																while (!closer(threatSpace, sequence.get(n), x, y)) {
																	n++;
																}
																sequence.add(n, latestPoint);
															} else {
																// start --- end --- latestPoint
																sequence.add(latestPoint);
															}
														} else {
															List<Point> temp = splitOff(latestPoint, sequence);
															result.get(threatSpace).add(temp);
															// need to do this - otherwise this sequence is checked
															// again for reasons unknown - do not touch!!
															visited.add(temp);
														}
													}
												}
											} else {
												// the sequence has different color from the new threat point, so
												// trim the existing sequence as necessary
												List<Point> opposite = oppositeSide(latestPoint, threatSpace, sequence);
												if (!opposite.isEmpty()) {
													for (Point p : opposite) {
														sequence.remove(p);
													}
												} else if (!blocking) {
													toAdd = true;
												}
												if (sequence.size() == 0) sequence.add(latestPoint);
											}
										}
									}
								}
							}
							// threat space doesn't exist or none of the sequences affect latest point
							if (!exists || !found || toAdd) {
								// add in new sequence
								List<Point> sequence = new ArrayList<>();
								sequence.add(latestPoint);
								if (!exists) {
									List<List<Point>> sequenceList = new ArrayList<>();
									sequenceList.add(sequence);
									result.put(threatSpace, sequenceList);
								} else {
									result.get(threatSpace).add(sequence);
								}
							}
						} else if (board[xt][yt] == turn) {
							blocking = true;
						}
					}
				}
			}
			// rotate 90° left
			int[] temp = Arrays.copyOf(xFactor,2);
			for (int j=0; j<=1; j++) {
				xFactor[j] = 0 - yFactor[j];
				yFactor[j] = temp[j];
			}
		}
		return result;
	}

	// returns true if new point is closer than existing point to threat space
	private boolean closer(Point origin, Point existing, int x, int y) {
		return (origin.x - x) * (origin.x - x) + (origin.y - y) * (origin.y - y) <
				(origin.x - existing.x) * (origin.x - existing.x) + (origin.y - existing.y) * (origin.y - existing.y);
	}

	// if three points are in line, returns true
	private boolean inLine(Point A, Point B, Point C) {
		return A.x * (B.y - C.y) + B.x * (C.y - A.y) + C.x * (A.y - B.y) == 0;
	}

	// returns true if a point is within range of 4
	private boolean inRange(Point o, Point toCheck) {
		return toCheck.x >= o.x - 4 && toCheck.x <= o.x + 4 && toCheck.y >= o.y - 4 && toCheck.y <= o.y + 4;
	}

	// returns relative position of latest point in relation to existing sequence
	private int position(List<Point> sequence, Point latestPoint) {
		Point start = sequence.get(0), end = sequence.get(sequence.size() - 1);
		if (latestPoint.x > Math.min(start.x, end.x) && latestPoint.x < Math.max(start.x, end.x) &&
				latestPoint.y > Math.min(start.y, end.y) && latestPoint.y < Math.max(start.y, end. y)) {
			return 2;
		} else if (closer(latestPoint, end, start.x, start.y)) {
			return 1;
		} else {
			return 3;
		}
	}

	// checks if there are no opposing colors between the end point and the new point
	private boolean isClear(Point end, Point latestPoint, int turn) {
		if (end.x == latestPoint.x) {
			// vertical
			for (int i = 1; i < Math.abs(end.y - latestPoint.y); i++) {
				if (board[end.x][Math.min(end.y, latestPoint.y) + i] == turn) return false;
			}
		} else if (end.y == latestPoint.y) {
			// horizontal
			for (int i = 1; i < Math.abs(end.x - latestPoint.x); i++) {
				if (board[Math.min(end.x, latestPoint.x) + i][end.y] == turn) return false;
			}
		} else if ((end.y - latestPoint.y)/(end.x - latestPoint.x) == 1){
			// right up diagonal (slope = 1)
			for (int i = 1; i < Math.abs(end.x - latestPoint.x); i++) {
				if (board[Math.min(end.x, latestPoint.x) + i][Math.min(end.y, latestPoint.y) + i] == turn) return false;
			}
		} else {
			// right down diagonal (slope = -1)
			for (int i = 1; i < Math.abs(end.x - latestPoint.x); i++) {
				if (board[Math.min(end.x, latestPoint.x) + i][Math.max(end.y, latestPoint.y) - i] == turn) return false;
			}
		}
		return true;
	}

	// given a sequence in which at least one element is out of range and a point, splits off a new sequence
	private List<Point> splitOff(Point latestPoint, List<Point> sequence) {
		List<Point> result = new ArrayList<>();
		result.add(latestPoint);
		Map<Integer, Point> distance = new HashMap<>();
		for (Point p : sequence) {
			distance.put((p.x - latestPoint.x) * (p.x - latestPoint.x) +
					(p.y - latestPoint.y) * (p.y - latestPoint.y), p);
		}
		for (int i=1; i<5; i++) {
			for (Integer d : distance.keySet()) {
				if (d == i * i || d == i * i * 2) {
					result.add(distance.get(d));
				}
			}
		}
		return result;
	}

	// lists all points in the sequence that are on the opposite side of threat space, with latest point as axis
	// also, can assume that the sequence and point are in line
	private List<Point> oppositeSide(Point latestPoint, Point threatSpace, List<Point> sequence) {
		List<Point> result = new ArrayList<>();
		if (latestPoint.x == threatSpace.x) {
			// vertical
			if (latestPoint.y > threatSpace.y) {
				for (Point p : sequence) {
					if (p.y > latestPoint.y) result.add(p);
				}
			} else {
				for (Point p : sequence) {
					if (p.y < latestPoint.y) result.add(p);
				}
			}
		} else if (latestPoint.x > threatSpace.x) {
			for (Point p : sequence) {
				if (p.x > latestPoint.x) result.add(p);
			}
		} else {
			for (Point p : sequence) {
				if (p.x < latestPoint.x) result.add(p);
			}
		}
		return result;
	}

	private int[][] calculateScores(Map<Point, List<List<Point>>> lookup, Map<Point, List<List<PI>>> threatSpaces) {
		int[][] result = new int[19][19];
		for (Point threatSpace : lookup.keySet()) {
			int black = 0, white = 0;
			// sequence-based scoring
			for (List<Point> threatSequence : lookup.get(threatSpace)) {
				int seqScore = 0, blackCount = 0, whiteCount = 0;
				// for points within a sequence, multiply them
				for (Point threat : threatSequence) {
					int[] temp = threatSpaceFinder(threat, threatSpace);
					if (board[threatSequence.get(0).x][threatSequence.get(0).y] == 1) {
						if (blackCount == 0) {
							seqScore = threatSpaces.get(threat).get(temp[0]).get(temp[1]).getI();
						} else {
							if (turn == 1) {
								seqScore *= (threatSpaces.get(threat).get(temp[0]).get(temp[1]).getI() / 2);
							} else {
								seqScore *= threatSpaces.get(threat).get(temp[0]).get(temp[1]).getI();
							}
						}
						blackCount++;
					} else {
						if (whiteCount == 0) {
							try {
								seqScore = threatSpaces.get(threat).get(temp[0]).get(temp[1]).getI();
							} catch (IndexOutOfBoundsException e) {
								System.out.println("Error: "+threat.toString()+", temp: "+temp[0]+" "+temp[1]);
								System.out.println("ThreatSequence: "+threatSequence.toString());
								System.out.println("ThreatSpace: "+threatSpace.toString());
							}
						} else {
							if (turn == 1) {
								seqScore *= (threatSpaces.get(threat).get(temp[0]).get(temp[1]).getI() * -1);
							} else {
								seqScore *= (threatSpaces.get(threat).get(temp[0]).get(temp[1]).getI() / -2);
							}
						}
						whiteCount++;
					}
				}
				// then add up the scores of the sequences
				if (board[threatSequence.get(0).x][threatSequence.get(0).y] == 1) {
					if (blackCount == 4) { // winning condition
						black = SUFFICIENTLY_LARGE_NUMBER;
					} else {
						black += seqScore;
					}
				} else {
					if (whiteCount == 4) {
						white = (-1 * SUFFICIENTLY_LARGE_NUMBER);
					} else {
						white += seqScore;
					}
				}
			}
			if (black == 0) {
				result[threatSpace.x][threatSpace.y] = white;
			} else if (white == 0) {
				result[threatSpace.x][threatSpace.y] = black;
			} else {
				if (turn == -1) {
					// white's turn
					result[threatSpace.x][threatSpace.y] = white - (int)(DEFENSE_WEIGHT * black);
				} else {
					result[threatSpace.x][threatSpace.y] = black - (int)(DEFENSE_WEIGHT * white);
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
		System.out.println("(Step) time to alternate scores: "+(time[1]-time[0])/1000000+" ms");
		System.out.println("(Step) time to modify existing threat space: "+(time[2]-time[1])/1000000+" ms");
		System.out.println("(Step) time to put in new threat: "+(time[3]-time[2])/1000000+" ms");
		System.out.println("(Step) time to correct scores that are directly touched: "+(time[4]-time[3])/1000000+" ms");
		System.out.println("(Hash) time to generate lookup: "+(time[6]-time[5])/1000000+" ms");
		System.out.println("(Calc) time to calculate scores: "+(time[7]-time[6])/1000000+" ms");
		System.out.println("Total time to add a point: "+(time[7]-time[0])/1000000+" ms");
	}

	public int[][] getScores() {
		return scores;
	}

	// return the best move
	public Point winningMove() {
		Point result = new Point(10,10);
		return result;
	}

	// the minimax depth-first search with alphabeta pruning
	private int alphaBeta(int[][] board, Map<Point, List<List<PI>>> threatSpaces,
						  Map<Point, List<List<Point>>> lookup, int[][] scores, int depth) {
		return 0;
	}
}